package jpstrack.android;

import java.io.File;
import java.util.List;

import jpstrack.fileio.GPSFileSaver;

import android.app.Activity;
import android.content.Intent;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class Main extends Activity implements LocationListener, OnClickListener { 

	private static final int MIN_METRES = 1;
	private static final int MIN_SECONDS = 5;
	private static final String[] PROVIDER_STATUS_VALUES = { 
		"out of service",
		"temporarily unavailable", 
		"available"
	};

	private LocationManager mgr;
	private TextView output;
	private String preferred;
	private EditText latOutput, longOutput;
	private GPSFileSaver trackerIO;
	private View startButton, pauseButton, stopButton;
	
	public static final String TEMP_HARDCODED_DIR = "/sdcard/jpstrack";	// xxx
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		output = (TextView) findViewById(R.id.output);
		
		mgr = (LocationManager) getSystemService(LOCATION_SERVICE); 
		for (String prov : mgr.getAllProviders()) {
			log(getString(R.string.provider_found) + prov);
		}

		new File(TEMP_HARDCODED_DIR).mkdirs();
		
		Criteria criteria = new Criteria();
		criteria.setAccuracy(Criteria.ACCURACY_FINE);
		List<String> providers = mgr.getProviders(criteria, true);
		if (providers == null || providers.size() == 0) {
			log(getString(R.string.cannot_get_gps_service));
			return;
		}
 		preferred = providers.get(0);			// first == preferred
		log(getString(R.string.preferred_provider_is) + preferred);

		final Location lastKnownLocation = mgr.getLastKnownLocation(preferred);
		if (lastKnownLocation != null) {
			printLocation(getString(R.string.last_known_location_), lastKnownLocation); 
		}
		
		// I/O Helper
		trackerIO = new GPSFileSaver(TEMP_HARDCODED_DIR, "201005271313.gpx");
		
		// THE GUI
		latOutput = (EditText) findViewById(R.id.lat_output);
		latOutput.setEnabled(false);
		longOutput = (EditText) findViewById(R.id.lon_output);
		longOutput.setEnabled(false);
		startButton = findViewById(R.id.start_button);
		startButton.setOnClickListener(this);
		pauseButton = findViewById(R.id.pause_button);
		pauseButton.setOnClickListener(this);
		pauseButton.setEnabled(false);
		stopButton = findViewById(R.id.stop_button);
		stopButton.setOnClickListener(this);
		stopButton.setEnabled(false);
		TextView fileNameLabel = (TextView) findViewById(R.id.filename_label);
		fileNameLabel.setText("YYYYMMDDHHMM.gpx");	// xxx from Prefs or Model??
		
		// third row - note Buttons
		View textNoteButton = findViewById(R.id.textnote_button);
		textNoteButton.setEnabled(false);
		View voiceNoteButton = findViewById(R.id.voicenote_button);
		voiceNoteButton.setOnClickListener(this);
		View takePictureButton = findViewById(R.id.takepicture_button);
		takePictureButton.setEnabled(false);
	}
	
    @Override
    protected void onResume() {
            super.onResume();
            mgr.requestLocationUpdates(preferred, MIN_SECONDS * 1000, MIN_METRES, this);
    }

    /** Called by Android when we get saving; turn off
     * getting GPS updates in hopes this will save battery
     * life. Better probably to power off the GPS, but then,
     * what if some other App is using it...?
     */
    @Override
    protected void onPause() {
            super.onPause();
            mgr.removeUpdates(this);
    }
    
    boolean saving = false, paused = false;
    
	/** From LocationListener, called when the location changes, obviously */
	@Override
	public void onLocationChanged(Location location) {
		if (location == null) {
			log("Got NULL Location from provider!");
		}
		printLocation("Current Location", location);
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
			startButton.setEnabled(false);
			try {
				trackerIO.startFile();
				log("Starting File Updates");
			} catch (RuntimeException e) {
				Toast.makeText(this, "Could not save: " + e, Toast.LENGTH_LONG).show();
				startButton.setEnabled(true);
				return;
			}
			saving = true; paused = false;
			pauseButton.setEnabled(true);
			stopButton.setEnabled(true);
			break;
		case R.id.pause_button:
			paused = !paused;
			break;
		case R.id.stop_button:
			log("Stopping File Updates");
			pauseButton.setEnabled(false);
			stopButton.setEnabled(false);
			saving = false; paused = false;
			trackerIO.endFile();
			startButton.setEnabled(true);
			break;
		case R.id.voicenote_button:
			log("Starting Voice Recording");
			try {
				startActivity(new Intent(this, VoiceNoteActivity.class));
			} catch (Exception e) {
				Toast.makeText(this, "Could not create note: " + e, Toast.LENGTH_LONG).show();
			}
			break;
		default:
			log("Unexpected Click from " + v.getId());
			break;
		}
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
		log("Provider disabled: " + provider);
	}

	/** From LocationListener, things are looking up! */
	@Override
	public void onProviderEnabled(String provider) {
		log("Provider enabled: " + provider);
	}

	/** From LocationListener, something changed. */
	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
		String mesg = String.format("Provider %s status %s", provider, PROVIDER_STATUS_VALUES[status]);
		log(mesg);
	}

	private void log(String string) {
		output.append(string + "\n");
	}

	private void printLocation(String type, Location location) {
		if (location == null)
			log("Location[unknown]");
		else
			log(type + " " + "[" + location.getLatitude() + "," + location.getLongitude() + "]");
	}

}
