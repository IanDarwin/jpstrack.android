package jpstrack.android;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.Properties;

import jpstrack.fileio.FileNameUtils;
import jpstrack.fileio.GPSFileSaver;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.bugsense.trace.BugSenseHandler;

public class Main extends Activity implements GpsStatus.Listener, LocationListener, OnClickListener {

	static final String TAG = "jpstrack";
	
	private static final int ACTION_TAKE_PICTURE = 1;
	private static final int ACTION_TAKE_SOUNDBITE = 2;
	
	private static final int DIALOG_EULA = 0;
	private static final int DIALOG_ABOUT = 1;
	
	private static final int MIN_METRES = 1;
	private static final int MIN_SECONDS = 5;
	private final String PROVIDER = LocationManager.GPS_PROVIDER;
	private static final String[] PROVIDER_STATUS_VALUES = { 
		"out of service",
		"down temporarily", 
		"available" };
	
	public static final String SKIP_SKIP = "DONT_SHOW_SKIP";


	private LocationManager mgr;
	private static File dataDir;
	private TextView output;
	private TextView latOutput, longOutput;
	private TextView fileNameLabel;
	private GPSFileSaver trackerIO;
	private View startButton, pauseButton, stopButton;
	private boolean saving, paused;
	private File savingFile;
	private static boolean sdWritable;

	private File imageFile, soundFile;
	private BroadcastReceiver extStorageRcvr;

	private String OUR_BUGSENSE_API_KEY;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		Log.d(TAG, "onCreate()");
		super.onCreate(savedInstanceState);
		if (isDebug()) {
			setStrictMode();
		}
		
		// Show the EULA if they've not yet agreed to it it.
		if (!SettingsActivity.hasSeenEula(this)) {
			showDialog(DIALOG_EULA);
		}
				
		// Start the welcome page or video if they haven't seen it yet.
		if (!SettingsActivity.hasSeenWelcome(this)) {
			startActivity(new Intent(this, OnboardingActivity.class));
		}

		setContentView(R.layout.main);
		
		View main = findViewById(R.id.mainView);
		main.getBackground().setAlpha(70);
		
		// set up BugSense bug tracking
		loadKeys();
		if (OUR_BUGSENSE_API_KEY != null) {
			BugSenseHandler.setup(this, OUR_BUGSENSE_API_KEY);
		}

		saving = false;
		paused = false;
		
		// Filesystem setup
		checkSdPresent(); // run it manually first, then on change.
		if (!sdWritable)  {
			Toast.makeText(this, "Warning, external storage not available", Toast.LENGTH_LONG).show();
		}
		extStorageRcvr = new BroadcastReceiver() {			
			@Override
			public void onReceive(Context context, Intent intent) {
				Log.d(TAG, "BroadcastReceiver got: " + intent);
				checkSdPresent();
			}
		};
		
		IntentFilter iFilter = new IntentFilter();
		iFilter.addAction(Intent.ACTION_MEDIA_MOUNTED);
		iFilter.addAction(Intent.ACTION_MEDIA_REMOVED);
		registerReceiver(extStorageRcvr, iFilter);

		// Set up the save location (gpx files, text notes, etc.)
		ThreadUtils.executeAndWait(new Runnable() {
			public void run() {				
				String preferredSaveLocation =
						PreferenceManager.getDefaultSharedPreferences(Main.this).getString(SettingsActivity.OPTION_DIR, null);
				if (preferredSaveLocation != null && !"".equals(preferredSaveLocation)) {
					// We've been run before
					dataDir = new File(preferredSaveLocation);
				} else {
					// First run on this device, probably.
					final File externalStorageDirectory = Environment.getExternalStorageDirectory();
					Log.d(TAG, "ExternalStorageDirectory = " + externalStorageDirectory);
					dataDir = new File(externalStorageDirectory, SettingsActivity.DIRECTORY_NAME);
				}
				Log.d(TAG, "Using Data Directory " + dataDir);
				dataDir.mkdirs();	// just in case
				if (!dataDir.exists()) {
					final String message = "Warning: Directory " + dataDir + " not created";
					Log.d(TAG, message);
					Looper.prepare();
					Toast.makeText(Main.this, message, Toast.LENGTH_LONG).show();
				}
			}
		});
		
		output = (TextView) findViewById(R.id.output);
		
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
		
		// GPS setup - do after GUI, of course...
		mgr = (LocationManager) getSystemService(LOCATION_SERVICE);
		mgr.addGpsStatusListener(this);
		Location last = mgr.getLastKnownLocation(PROVIDER);
		onLocationChanged(last);
		startReceiving();
		
		// Now see if we just got interrupted by e.g., rotation
		Main old = (Main) getLastNonConfigurationInstance();
		if (old != null) {
			// Do NOT refer to any GUI components in the old object
			mgr.removeGpsStatusListener(old); // prevent accidents
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
		} else {		
			// I/O Helper
			trackerIO = new GPSFileSaver(dataDir, FileNameUtils.getNextFilename());
		}
	}
	
	/**
	 *  Load a Props file from the APK zipped filesystem, extract our app key from that.
	 */
	public void loadKeys() {
		InputStream is = null;
		try {
			Resources resources = getResources();
			if (resources == null) {
				throw new ExceptionInInitializerError("getResources() returned null");
			}
			
			// If this line won't compile, create an empty file
			// with the exact (but stupid) name res/raw/keys_props.properties 
			// And do Project->Clean, all the usual stuff...
			is = resources.openRawResource(R.raw.keys_props);
			if (is == null) {
				Log.w(TAG, "loadKeys: getResources().openRawResource() returned null");
				return;
			}
			Properties p = new Properties();
			p.load(is);
			OUR_BUGSENSE_API_KEY = p.getProperty("BUGSENSE_API_KEY");
			if (OUR_BUGSENSE_API_KEY == null) {
				String message = "Could not find BUGSENSE_API_KEY in props";
				Log.w(TAG, message);
				return;
			}
			Log.d(TAG, "key = " + OUR_BUGSENSE_API_KEY);
		} catch (Exception e) {
			String message = "Error loading properties: " + e;
			Log.d(TAG, message);
			throw new ExceptionInInitializerError(message);
		} finally {
			if (is != null) {
				try {
					is.close();
				} catch (IOException e) {
					// What a useless exception
				}
			}
		}
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case DIALOG_EULA:
			final AlertDialog alertDialog = new AlertDialog.Builder(this)
			.setCancelable(false)
			.setTitle(R.string.terms)
			.setMessage(R.string.eula)
			.setPositiveButton(R.string.accept_eula,
					new AlertDialog.OnClickListener() {
						public void onClick(DialogInterface dialog,
								int which) {
							Log.d(Main.TAG, "User accepted EULA!");
							SettingsActivity.setSeenEula(Main.this, true);
						}
					})
			.setNegativeButton(R.string.reject_eula,
					new AlertDialog.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							Log.d(Main.TAG, "User REJECTED EULA!");	
							System.exit(-1);
						}
					})
			.create();
			return alertDialog;
		case DIALOG_ABOUT:
			final AlertDialog aboutDialog = new AlertDialog.Builder(this)
			.setCancelable(true)
			.setTitle(R.string.about_name)
			.setMessage(R.string.about_text)
			.setPositiveButton(R.string.about_done_button_label,
					new AlertDialog.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							Log.d(Main.TAG, "User accepted EULA!");
							SettingsActivity.setSeenEula(Main.this, true);
							finish();
						}
			}).create();
			return aboutDialog;
		default:
				return null;
		}
	}

	private boolean isDebug() {
		return true;
	}
	
	public void setStrictMode() {
		// StrictMode.enableDefaults();
		try {
			Class<?> c = Class.forName("android.os.StrictMode");
			Method m = c.getMethod("enableDefaults", (Class<?>[])null);
			m.invoke(null, (Object[])null);
		} catch (Exception e) {
			Log.d(TAG, "Unable to set StrictMode: " + e);
			return;
		}
	}

	@Override
	protected void onDestroy() {
		Log.d(TAG, "I'm being destroyed!");
		super.onDestroy();
		unregisterReceiver(extStorageRcvr);
	}
	
	private void checkSdPresent() {
		String sdState = Environment.getExternalStorageState();
		sdWritable = false;
		if (Environment.MEDIA_MOUNTED.equals(sdState)) {
			sdWritable = true;
		}
	}
	
	/** Returns arbitrary single token object to keep alive across
	 * the destruction and re-creation of the entire Enterprise.
	 */
	@Override
	public Object onRetainNonConfigurationInstance() {
		Log.i(TAG, "Remember: 3");
		return this;
	}

	/** start receiving GPS data... */
	private void startReceiving() {
		Log.d(TAG, "startReceiving()");
		mgr.requestLocationUpdates(PROVIDER, 
			MIN_SECONDS * 1000,
			MIN_METRES,
			this);
	}
	
	private void stopReceiving() {
		mgr.removeUpdates(this);
	}

	/**
	 * Called by Android when we get paused; if not saving,
	 * turn off getting GPS updates, in the (documented)
	 * hopes this will save battery life.
	 */
	@Override
	protected void onPause() {
		Log.d(TAG, "onPause()");
		super.onPause();
		if (!saving || paused) {
			stopReceiving();
		}
	}

	@Override
	protected void onResume() {
		Log.d(TAG, "onResume()");
		super.onResume();
		if (saving && !paused) {
			startReceiving();
		}
	}

	/** From LocationListener, called when the location changes, obviously */
	@Override
	public void onLocationChanged(final Location location) {
		Log.d(TAG, "Got location " + location);
		if (location == null) {
			return;
		}
		
		logToScreen("Location: " + location.getLatitude() + "," + location.getLongitude());
		final double latitude = location.getLatitude();
		final double longitude = location.getLongitude();
		latOutput.setText(Double.toString(latitude));
		longOutput.setText(Double.toString(longitude));
		if (saving && !paused) {
			ThreadUtils.executeAndWait(new Runnable() {
				public void run() {	
					trackerIO.write(location.getTime(), latitude, longitude);
				}
			});
		}
	}

	/** Remember - do not block the GUI thread, kiddies! */
	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.start_button:
			startButton.setEnabled(false);
			try {
				// New filename each time we start recording.
				trackerIO.setFileName(FileNameUtils.getNextFilename());
				savingFile = trackerIO.startFile();
				fileNameLabel.setText(savingFile.getName());
				startReceiving();		// Disk IO is done on the service's thread.
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
			// Don't call stopReceiving() here, so the display
			// will still update. Maybe make this a preference?
			paused = !paused;
			syncPauseButtonToState();
			break;
		case R.id.stop_button:
			saving = false;
			paused = false;
			syncPauseButtonToState();
			stopButton.setEnabled(false);
			ThreadUtils.executeAndWait(new Runnable() {
				public void run() {	
					trackerIO.endFile();
				}
			});
			String mesg = "Saved as " + savingFile.getAbsolutePath();
			logToScreen("Stopping file updates; " + mesg);
			Toast.makeText(this, mesg, Toast.LENGTH_LONG).show();
			fileNameLabel.setText(FileNameUtils.getDefaultFilenameFormatWithExt());
			startButton.setEnabled(true);
			break;
		case R.id.voicenote_button:
			logToScreen("Starting Voice Recording");
			// Use an Intent to get the Voice Record app going.
			// Intent soundIntent = new Intent(MediaStore.Audio.Media.RECORD_SOUND_ACTION);
			// GRRR, standard Sound Recorder doesn't accept MediaStore.EXTRA_OUTPUT)
			Intent soundIntent = new Intent(this, VoiceNoteActivity.class);
			// Set up file to save image into.
			soundFile = new File(Main.getDataDir(), FileNameUtils
					.getNextFilename("mp3"));
			soundIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(soundFile));
			// And away we go!
			startActivityForResult(soundIntent, ACTION_TAKE_SOUNDBITE);
			break;
		case R.id.textnote_button:
			logToScreen("Starting Text Entry");
			try {
				startActivity(new Intent(this, TextNoteActivity.class));
			} catch (Exception e) {
				Toast.makeText(this, getString(R.string.cant_start_activity) + ": " + e, Toast.LENGTH_LONG).show();
			}
			break;
		case R.id.takepicture_button:
			logToScreen("Starting Camera Activity");
			try {
				// Use an Intent to get the Camera app going.
				Intent imageIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
				// Set up file to save image into.
				imageFile = new File(Main.getDataDir(), 
					FileNameUtils.getNextFilename("jpg"));
				imageIntent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 1);
				imageIntent.putExtra(MediaStore.EXTRA_OUTPUT, 
					Uri.fromFile(imageFile));
				// And away we go!
				startActivityForResult(imageIntent, ACTION_TAKE_PICTURE);
			} catch (Exception e) {
				Toast.makeText(this,
					getString(R.string.cant_start_activity) + ": " + e,
					Toast.LENGTH_LONG).show();
			}
			break;
		default:
			logToScreen("Unexpected Click from " + v.getId());
			break;
		}
	}

	/** Called when an Activity we started for Result is complete */
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
		case ACTION_TAKE_PICTURE:
			switch (resultCode) {
			case Activity.RESULT_OK:
				if (imageFile.exists()) {
					final String message = getString(R.string.picture_saved) + " " + imageFile.getAbsoluteFile();
					logToScreen(message);
					Toast.makeText(this, message, Toast.LENGTH_LONG).show();
				} else {
					AlertDialog.Builder alert = new AlertDialog.Builder(this);
					final String message = getString(R.string.picture_created_but_missing);
					logToScreen(message);
					alert.setTitle(getString(R.string.error))
						.setMessage(message)
						.show();
				}
				break;
			case Activity.RESULT_CANCELED:
				logToScreen("Done");
				break;
			default:
				Toast.makeText(this, "Unexpected resultCode: " + resultCode, Toast.LENGTH_LONG).show();
				break;
			}
			break;
		case ACTION_TAKE_SOUNDBITE:
			switch (resultCode) {
			case Activity.RESULT_OK:
				if (soundFile.exists()) {
					final String message = getString(R.string.picture_saved) + " " + soundFile.getAbsoluteFile();
					logToScreen(message);
					Toast.makeText(this, message, Toast.LENGTH_LONG).show();
				} else {
					AlertDialog.Builder alert = new AlertDialog.Builder(this);
					final String message = getString(R.string.picture_created_but_missing);
					logToScreen(message);
					alert.setTitle(getString(R.string.error))
						.setMessage(message)
						.show();
				}
				break;
			case Activity.RESULT_CANCELED:
				logToScreen("Done");
				break;
			default:
				Toast.makeText(this, "Unexpected resultCode: " + resultCode, Toast.LENGTH_LONG).show();
				break;
			}
			break;
		default:
			Toast.makeText(this, "Completion of unknown activity request " + requestCode + "!", Toast.LENGTH_LONG).show();
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
		case R.id.rerun_intro:
			Intent intent = new Intent(this, OnboardingActivity.class);
			intent.putExtra(SKIP_SKIP, true);
			startActivity(intent);
			return true;
		case R.id.about:
			showDialog(DIALOG_ABOUT);
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
		Log.d(TAG, mesg);
		logToScreen(mesg);
	}
	
	/** From GpsStatus.Listener */
	@Override
	public void onGpsStatusChanged(int event) {
		switch (event) {
		case GpsStatus.GPS_EVENT_FIRST_FIX:
			Log.d(TAG, "GPS Status: GotaFix");	
			break;
		case GpsStatus.GPS_EVENT_STARTED:
			Log.d(TAG, "GPS Status: Started!");	
			break;
		case GpsStatus.GPS_EVENT_STOPPED:
			Log.d(TAG, "GPS Status: Stopped");	
			break;
		}
	}

	public static File getDataDir() {
		return dataDir;
	}

	private void logToScreen(String string) {
		if (output == null) {
			Log.e(TAG, "output is NULL, unable to display: " + string);
			return;
		}
		output.append(string + "\n");
	}
	
	// Plain accessors
	public static boolean isSdWritable() {
		return sdWritable;
	}
}
