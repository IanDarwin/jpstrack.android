package jpstrack.android;

import java.io.File;
import java.util.List;

import jpstrack.fileio.FileNameUtils;
import jpstrack.fileio.GPSFileSaver;
import android.app.Activity;
import android.content.Intent;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class Main extends Activity implements LocationListener, OnClickListener {

	private static final String LOG_TAG = "jpstrack";
	private static final int MIN_METRES = 1;
	private static final int MIN_SECONDS = 5;
	private static final String[] PROVIDER_STATUS_VALUES = { 
		"out of service",
		"down temporarily", 
		"available" };

	private LocationManager mgr;
	private static File dataDir;
	private TextView output;
	private String preferred;
	private TextView latOutput, longOutput;
	private TextView fileNameLabel;
	private GPSFileSaver trackerIO;
	private View startButton, pauseButton, stopButton;
	private boolean saving, paused;

	public static final String TEMP_HARDCODED_DIR = "/sdcard/jpstrack"; // xxx

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		saving = false;
		paused = false;

		output = (TextView) findViewById(R.id.output);

		dataDir = new File(TEMP_HARDCODED_DIR);
		dataDir.mkdirs();

		// THE GUI
		latOutput = (TextView) findViewById(R.id.lat_output);
		longOutput = (TextView) findViewById(R.id.lon_output);
		startButton = findViewById(R.id.start_button);
		startButton.setOnClickListener(this);
		pauseButton = findViewById(R.id.pause_button);
		pauseButton.setOnClickListener(this);
		pauseButton.setEnabled(false);
		stopButton = findViewById(R.id.stop_button);
		stopButton.setOnClickListener(this);
		stopButton.setEnabled(false);
		fileNameLabel = (TextView) findViewById(R.id.filename_label);
		fileNameLabel.setText(FileNameUtils.getDefaultFilenameFormatWithExt());

		// third row - note Buttons
		View textNoteButton = findViewById(R.id.textnote_button);
		textNoteButton.setOnClickListener(this);
		View voiceNoteButton = findViewById(R.id.voicenote_button);
		voiceNoteButton.setOnClickListener(this);
		View takePictureButton = findViewById(R.id.takepicture_button);
		takePictureButton.setOnClickListener(this);
		
		// Now see if we just got interrupted by e.g., rotation
		Main old = (Main) getLastNonConfigurationInstance();
		if (old != null) {
			mgr = old.mgr;
			saving = old.saving;
			paused = old.paused;
			startButton.setEnabled(!saving);
			syncPauseButtonToState();
			stopButton.setEnabled(saving);
			// this is the most important line: keep saving to same file!
			trackerIO = old.trackerIO;			
			if (saving) {
				fileNameLabel.setText(trackerIO.getFileName());
			}
			return;
		}
		
		// I/O Helper
		trackerIO = new GPSFileSaver(TEMP_HARDCODED_DIR, FileNameUtils.getNextFilename());
	}
	
	/** Returns arbitrary single token object to keep alive across
	 * the destruction and re-creation of the Enterprise.
	 */
	@Override
	public Object onRetainNonConfigurationInstance() {
		Log.i(LOG_TAG, "Remember: 3");
		return this;
	}
	
	
	void initGPS() {
		// GPS setup
		mgr = (LocationManager) getSystemService(LOCATION_SERVICE);
		for (String prov : mgr.getAllProviders()) {
			Log.i(LOG_TAG, getString(R.string.provider_found) + prov);
		}
		
		Criteria criteria = new Criteria();
		criteria.setAccuracy(Criteria.ACCURACY_FINE);
		List<String> providers = mgr.getProviders(criteria, true);
		if (providers == null || providers.size() == 0) {
			Log.e(LOG_TAG, getString(R.string.cannot_get_gps_service));
			Toast.makeText(this, "Could not open GPS service",
					Toast.LENGTH_LONG).show();
			return;
		}
		preferred = providers.get(0); // first == preferred
		Log.i(LOG_TAG, getString(R.string.preferred_provider_is) + preferred);
		
		final Location lastKnownLocation = mgr.getLastKnownLocation(preferred);
		onLocationChanged(lastKnownLocation);
	}
	
	/**
	 * Called by Android when we get paused; turn off getting GPS updates in
	 * hopes this will save battery life.
	 */
	@Override
	protected void onPause() {
		super.onPause();
		if (!saving || paused) {
			mgr.removeUpdates(this);
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		initGPS();
		if (saving && !paused) {
			mgr.requestLocationUpdates(preferred, MIN_SECONDS * 1000,
					MIN_METRES, this);
		}
	}


	/** From LocationListener, called when the location changes, obviously */
	@Override
	public void onLocationChanged(Location location) {
		if (location == null) {
			Log.e(LOG_TAG, "Got NULL Location from provider!");
			return;
		}
		logToScreen("Location: " + location.getLatitude() + "," + location.getLongitude());
		double latitude = location.getLatitude();
		double longitude = location.getLongitude();
		latOutput.setText(Double.toString(latitude));
		longOutput.setText(Double.toString(longitude));
		if (saving && !paused) {
			trackerIO.write(location.getTime(), latitude, longitude);
		}
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.start_button:
			if (preferred == null) {
				Toast.makeText(this, "GPS not started", Toast.LENGTH_LONG).show(); // XXX LAME LAME LAME - move startup here.
				return;
			}
			startButton.setEnabled(false);
			try {
				// New filename each time we start recording.
				trackerIO.setFileName(FileNameUtils.getNextFilename());
				File f = trackerIO.startFile();
				fileNameLabel.setText(f.getName());
				logToScreen("Starting File Updates");
			} catch (RuntimeException e) {
				Toast.makeText(this, "Could not save: " + e, Toast.LENGTH_LONG).show();
				startButton.setEnabled(true);
				return;
			}
			saving = true;
			paused = false;
			syncPauseButtonToState();
			stopButton.setEnabled(true);
			break;
		case R.id.pause_button:
			paused = !paused;
			syncPauseButtonToState();
			break;
		case R.id.stop_button:
			logToScreen("Stopping File Updates");
			saving = false;
			paused = false;
			syncPauseButtonToState();
			stopButton.setEnabled(false);
			trackerIO.endFile();
			fileNameLabel.setText(FileNameUtils.getDefaultFilenameFormatWithExt());
			startButton.setEnabled(true);
			break;
		case R.id.voicenote_button:
			logToScreen("Starting Voice Recording");
			try {
				startActivity(new Intent(this, VoiceNoteActivity.class));
			} catch (Exception e) {
				Toast.makeText(this, "Could not create note: " + e, Toast.LENGTH_LONG).show();
			}
			break;
		case R.id.textnote_button:
			logToScreen("Starting Text Entry");
			try {
				startActivity(new Intent(this, TextNoteActivity.class));
			} catch (Exception e) {
				Toast.makeText(this, "Could not create text note: " + e, Toast.LENGTH_LONG).show();
			}
			break;
		case R.id.takepicture_button:
			logToScreen("Starting Camera Activity");
			try {
				startActivity(new Intent(this, CameraNoteActivity.class));
			} catch (Exception e) {
				Toast.makeText(this, "Could not start picture acitivity: " + e, Toast.LENGTH_LONG).show();
			}
			break;
		default:
			logToScreen("Unexpected Click from " + v.getId());
			break;
		}
	}

	private void syncPauseButtonToState() {
		pauseButton.setEnabled(saving);
		((Button) pauseButton).setText(paused ? 
				R.string.pause_button_resume_label :
				R.string.pause_button_label);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		MenuInflater inf = getMenuInflater();
		inf.inflate(R.menu.menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.settings:
			startActivity(new Intent(this, SettingsActivity.class));
			return true;
		case R.id.about:
			startActivity(new Intent(this, AboutActivity.class));
			return true;
		}
		return false;
	}

	/** From LocationListener, providing very bad news... */
	@Override
	public void onProviderDisabled(String provider) {
		logToScreen("Provider disabled: " + provider);
	}

	/** From LocationListener, things are looking up! */
	@Override
	public void onProviderEnabled(String provider) {
		logToScreen("Provider enabled: " + provider);
	}

	/** From LocationListener, something changed. */
	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
		String mesg = String.format("Provider %s status %s", provider, PROVIDER_STATUS_VALUES[status]);
		logToScreen(mesg);
	}
	
	public static File getDataDir() {
		return dataDir;
	}

	private void logToScreen(String string) {
		output.append(string + "\n");
	}

}
