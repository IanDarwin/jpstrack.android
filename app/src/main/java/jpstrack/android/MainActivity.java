package jpstrack.android;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Looper;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import jpstrack.fileio.FileNameUtils;
import jpstrack.fileio.GPSFileSaver;
import jpstrack.model.Reading;
import jpstrack.net.NetResult;
import jpstrack.upload.TraceVisibility;
import jpstrack.upload.Upload;

/** 
 * The main Activity class for the Android version of JPSTrack
 * Probably does too much and is too big.
 */
@RequiresApi(api = Build.VERSION_CODES.M)
public class MainActivity extends AppCompatActivity implements LocationListener {

	static final String TAG = "jpstrack";

	private static final int ACTION_TAKE_PICTURE = 1;
	private static final int ACTION_TAKE_SOUNDBITE = 2;

	private static final int DIALOG_EULA = 0;
	private static final int DIALOG_ABOUT = 1;
	private static final int DIALOG_TURN_ON_GPS = 2;
	private static final int DIALOG_OSM_CONFIRM_UPLOAD = 3;

	private static final int MIN_METRES = 1;
	private static final int MIN_SECONDS = 5;
	private final String PROVIDER = LocationManager.GPS_PROVIDER;
	private static final String[] PROVIDER_STATUS_VALUES = {
			"out of service",
			"down temporarily",
			"available"};

	public static final String SKIP_SKIP = "DONT_SHOW_SKIP";

	private LocationManager mgr;
	static File dataDir = new File("/sdcard/Download/jpstrack");
	private TextView output;
	private TextView latOutput, longOutput, altOutput;
	private TextView fileNameLabel;
	private GPSFileSaver trackerIO;
	private View startButton, pauseButton, stopButton;
	private boolean saving, paused;
	protected static boolean sdWritable;

	private File imageFile, soundFile;
	private BroadcastReceiver extStorageRcvr;

	private String osmPassword;
	private final String osmHostProd = "api.openstreetmap.org";
	private final String osmHostTest = "master.apis.dev.openstreetmap.org";

	static ExecutorService threadPool = Executors.newFixedThreadPool(5);

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
			// We don't build the rest of the UI until the EULA is accepted.
			return;
		}

		// Start the welcome page or video if they haven't seen it yet.
		if (!SettingsActivity.hasSeenWelcome(this)) {
			startActivity(new Intent(this, OnboardingActivity.class));
		}

		setContentView(R.layout.main);

		View main = findViewById(R.id.mainView);
		main.getBackground().setAlpha(70);

		saving = false;
		paused = false;

		// Filesystem setup; Broadcast Receiver to watch for changes
		checkSdPresent(); // run it manually first, then on change as per BroadcastReceiver:
		if (!sdWritable) {
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

		ThreadUtils.executeAndWait(setupSaveDirLocation);

		output = findViewById(R.id.output);

		// THE GUI
		latOutput = findViewById(R.id.lat_output);
		longOutput = findViewById(R.id.lon_output);
		altOutput = findViewById(R.id.alt_output);
		startButton = findViewById(R.id.start_button);
		startButton.setOnClickListener(startButtonAction);
		pauseButton = findViewById(R.id.pause_button);
		pauseButton.setOnClickListener(v -> {
			// Don't call stopReceiving() here, so the display
			// will still update. Maybe make this a preference?
			paused = !paused;
			syncPauseButtonToState();
		});
		pauseButton.setEnabled(false);
		stopButton = findViewById(R.id.stop_button);
		stopButton.setOnClickListener(stopButtonAction);
		stopButton.setEnabled(false);
		fileNameLabel = (TextView) findViewById(R.id.filename_label);
		fileNameLabel.setText(FileNameUtils.getDefaultFilenameFormatWithExt());

		// third row - note Buttons
		View textNoteButton = findViewById(R.id.textnote_button);
		textNoteButton.setOnClickListener(textNoteButtonAction);
		View voiceNoteButton = findViewById(R.id.voicenote_button);
		voiceNoteButton.setOnClickListener(voiceNoteButtonAction);
		View takePictureButton = findViewById(R.id.takepicture_button);
		takePictureButton.setOnClickListener(takePictureButtonAction);

		// GPS setup - do after GUI, of course...
		// XXX Permissions goo here.
		mgr = (LocationManager) getSystemService(LOCATION_SERVICE);
		if (!mgr.isProviderEnabled(PROVIDER)) {
			showDialog(DIALOG_TURN_ON_GPS);
		}

		// Set up I/O Helper
		threadPool.submit(() -> dataDir.mkdir());
		trackerIO = new GPSFileSaver(dataDir, FileNameUtils.getNextFilename());

		if (ActivityCompat.checkSelfPermission(this,
				Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
			// We can use the API that requires the permission.
			Location last = mgr.getLastKnownLocation(PROVIDER);
			onLocationChanged(last);
			startReceiving();
		} else if (shouldShowRequestPermissionRationale(
				Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
			// Say why we need it!
			Toast.makeText(this, "Need permission to use the GPS for this program to work!", Toast.LENGTH_LONG).show();
		} else {
			// Ask for the permission.
			// The registered ActivityResultCallback gets the result of this request.
			storagePermissionLauncher.launch(
					Manifest.permission.ACCESS_FINE_LOCATION);
		}

		// Now see if we just got interrupted by e.g., rotation
		MainActivity old = (MainActivity) getLastCustomNonConfigurationInstance();
		Log.d(TAG, "getLastNonConfigurationInstance() = " + old);
		if (old != null) {
			// Do NOT refer to any GUI components from the old object
			saving = old.saving;
			paused = old.paused;
			startButton.setEnabled(!saving);
			syncPauseButtonToState();
			stopButton.setEnabled(saving);
			// this is the most important line: keep saving to same file!
			trackerIO = old.trackerIO;
		}

		if (((ApplicationSingleton)getApplication()).fileInProgress != null) {
			saving = true;
			startButton.setEnabled(false);
			fileNameLabel.setText(((ApplicationSingleton)getApplication()).fileInProgress.getName());
			stopButton.setEnabled(true);
		}
		if (saving) {
			fileNameLabel.setText(trackerIO.getFileName());
		}
	}

	// Apps can no longer implement onConfigurationInstance as it's now final in superclass.
	public Object onRetainCustomNonConfigurationInstance() {
		return this;
	}

	// Register the GPS permissions callback, which handles the user's response to the
	// system permissions dialog. Save the return value, an instance of
	// ActivityResultLauncher, as an instance variable.
	private final ActivityResultLauncher<String> gpsPermissionLauncher =
			registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
				if (isGranted) {
					setupSaveDirectoryInternal();
				} else {
					Toast.makeText(this, "You've disabled permissions so this action will be unusable", Toast.LENGTH_LONG).show();
				}
			});

	// Register the permissions callback, which handles the user's response to the
	// system permissions dialog. Save the return value, an instance of
	// ActivityResultLauncher, as an instance variable.
	private ActivityResultLauncher<String> storagePermissionLauncher =
			registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
				if (isGranted) {
					setupSaveDirectoryInternal();
				} else {
					Toast.makeText(this, "You've disabled permissions so this action will be unusable", Toast.LENGTH_LONG).show();
				}
			});


	/** Set up the save location (gpx files, text notes, etc.) */
	Runnable setupSaveDirLocation = () -> {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
		String preferredSaveLocation =
				prefs.getString(SettingsActivity.OPTION_DIR, dataDir.getAbsolutePath());
		if (preferredSaveLocation != null && !"".equals(preferredSaveLocation)) {
			// We've been run before
			dataDir = new File(preferredSaveLocation);
			prefs.edit().putString(SettingsActivity.OPTION_DIR, dataDir.getAbsolutePath()).commit();
		} else {
			// First run on this device, probably. Use "external storage" so user can
			// access own files without rooting device.
			if (this.checkSelfPermission(
					Manifest.permission.WRITE_EXTERNAL_STORAGE) ==
					PackageManager.PERMISSION_GRANTED) {
				// We can use the API that requires the permission.
				setupSaveDirectoryInternal();
				prefs.edit().putString(SettingsActivity.OPTION_DIR, dataDir.getAbsolutePath()).commit();
			} else if (shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
				// Tell user why we need this!
				Toast.makeText(this,
					"Need permission to write to /sdcard to you can access your files easily",
					Toast.LENGTH_LONG).show();
			} else {
				// Ask user for the permission.
				// The registered ActivityResultCallback gets the result of this request.
				storagePermissionLauncher.launch(
						Manifest.permission.WRITE_EXTERNAL_STORAGE);
			}
		}
	};

	void setupSaveDirectoryInternal() {
		threadPool.submit( () -> {
			final File externalStorageDirectory =
					Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
			dataDir = new File(externalStorageDirectory, SettingsActivity.DIRECTORY_NAME);

			Log.d(TAG, "Trying to use Data Directory " + dataDir);
			dataDir.mkdirs();    // Be sure dir exists. Doc says OK to ignore return & test with isDirectory()
			if (!dataDir.isDirectory()) {
				final String message = "Warning: Directory " + dataDir + " not created";
				Log.d(TAG, message);
				if (Looper.myLooper() == null) {
					Looper.prepare();
				}
				Toast.makeText(MainActivity.this, message, Toast.LENGTH_LONG).show();
			}
		});
	}

	NetResult<String> response;

	/**
	 * UPLOAD A FILE TO OSM
	 */
	private void doUpload() {
		final String description = "Map Track created by JPSTrack";
		final TraceVisibility visibility = TraceVisibility.IDENTIFIABLE;
		final File gpxFile = trackerIO.getFile();

		threadPool.submit(() -> {
				try {
					final String encodedPostBody =
							Upload.encodePostBody(description, visibility, gpxFile);
					// Log.d(TAG, "Post Body " + encodedPostBody);
					String host = SettingsActivity.useSandbox(this) ? osmHostTest : osmHostProd;
					final String userName = SettingsActivity.getOSMUserName(this);
					if (TextUtils.isEmpty(userName)) {
						startActivity(new Intent(this, SettingsActivity.class));
					}
					response = Upload.converse(host, 443,
							userName, osmPassword,
							encodedPostBody);

					// Doing Toast from a bg thread needs loopiness
					if (Looper.myLooper() == null) {
						Looper.prepare();
					}
					int ret = response.getStatus();
					switch (ret) {
						case 200:
							final long gpxId = Long.parseLong(response.getPayload());
							Toast.makeText(this, "Created GPX " + gpxId, Toast.LENGTH_LONG).show();
							break;
						case 401:
						case 403: // auth probs
							Toast.makeText(this, "Login failed", Toast.LENGTH_LONG).show();
							break;
						case 404:
							Toast.makeText(this, "Internal error: Bad URL", Toast.LENGTH_LONG).show();
							break;
						default:
							Toast.makeText(this, "Unexpected upload status " + ret, Toast.LENGTH_LONG).show();
							break;
					}
					MainActivity.this.runOnUiThread(() ->
							logToScreen("Upload completed with status " + ret));
				} catch (IOException e) {
					Log.e(TAG, "Upload caught " + e, e);
					response = new NetResult<>();
					response.setStatus(599);
				}
		});
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
			case DIALOG_EULA:
				return new AlertDialog.Builder(this)
						.setCancelable(false)
						.setTitle(R.string.terms)
						.setMessage(R.string.eula)
						.setPositiveButton(R.string.accept_eula,
								(dialog, which) -> {
									Log.d(TAG, "User accepted EULA!");
									SettingsActivity.setSeenEula(MainActivity.this, true);
									// Trigger a restart!
									startActivity(new Intent(MainActivity.this, MainActivity.class));
								})
						.setNegativeButton(R.string.reject_eula,
								(dialog, which) -> {
									Log.d(TAG, "User REJECTED EULA!");
									System.exit(-1);
								})
						.create();
			case DIALOG_ABOUT:
				return new AlertDialog.Builder(this)
						.setCancelable(true)
						.setTitle(R.string.about_name)
						.setMessage(R.string.about_text)
						.setPositiveButton(R.string.about_done_button_label,
								(dialog, which) -> {
									// Nothing to do?
								}).create();
			case DIALOG_TURN_ON_GPS:
				return new AlertDialog.Builder(this)
						.setCancelable(true)
						.setTitle(R.string.gps_dialog_name)
						.setMessage(R.string.gps_dialog_text)
						.setPositiveButton(R.string.gps_dialog_dismiss_label,
								(dialog1, which1) -> {
									// nothing to do?
								})
						.setNeutralButton(R.string.gps_dialog_settings_label,
								(dialog1, which1) -> {
									Intent settings = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
									startActivity(settings);
								}).create();
			case DIALOG_OSM_CONFIRM_UPLOAD:
				final EditText passwordText = new EditText(this);
				final AlertDialog osmPasswordDialog = new AlertDialog.Builder(this)
						.setCancelable(true)
						.setMessage("OSM Password")
						.setPositiveButton("Upload",
								(dialog, which) -> {
									MainActivity.this.osmPassword = passwordText.getText().toString();
									doUpload();
									dialog.cancel();
								})
						.setNegativeButton("Cancel", (dialog, which) -> {
							// nothing to do?
						}).create();
				osmPasswordDialog.setView(passwordText);
				return osmPasswordDialog;
			default:
				return null;
		}
	}

	private boolean isDebug() {
		return true;
	}

	public void setStrictMode() {
		StrictMode.enableDefaults();
	}

	@Override
	protected void onDestroy() {
		Log.d(TAG, "I'm being destroyed!");
		super.onDestroy();
		try {
			unregisterReceiver(extStorageRcvr);
		} catch (RuntimeException e) {
			// Don't care; interrupted onCreate()
		}
	}

	private void checkSdPresent() {
		String sdState = Environment.getExternalStorageState();
		sdWritable = Environment.MEDIA_MOUNTED.equals(sdState);
	}

	/** start receiving GPS data... */
	private void startReceiving() {
		Log.d(TAG, "startReceiving()");
		if (mgr == null) {
			throw new NullPointerException("mgr == null in startReceiving()");
		}
		if (ActivityCompat.checkSelfPermission(this,
				Manifest.permission.ACCESS_FINE_LOCATION) !=
				PackageManager.PERMISSION_GRANTED) {
			Toast.makeText(this, "Location Permissions denied, not obtaining", Toast.LENGTH_LONG).show();
			return;
		}
		mgr.requestLocationUpdates(PROVIDER,
				MIN_SECONDS * 1000,
				MIN_METRES,
				this);
	}
	
	private void stopReceiving() {
		Log.d(TAG, "stopReceiving()");
		if (mgr != null)
			mgr.removeUpdates(this);
	}

	/**
	 * Called by Android when we get paused; if not saving,
	 * turn off getting GPS updates, to save battery life.
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

		if (saving && !paused) {
			// Convert from Android location object to app-specific one.
			final double latitude = location.getLatitude();
			final double longitude = location.getLongitude();
			final double altitude = location.getAltitude();
			logToScreen("Location: " + latitude + "," + longitude);
			latOutput.setText(Double.toString(latitude));
			longOutput.setText(Double.toString(longitude));
			altOutput.setText(altitude > 0 ? Double.toString(altitude) : "N/A");
			Reading reading = new Reading(latitude, longitude, altitude, location.getTime());
			threadPool.submit(() -> trackerIO.write(reading));
		}
	}

	OnClickListener startButtonAction = v -> {
		startButton.setEnabled(false);
		try {
			// New filename each time we start recording.
			// Re-do setupSaveDirLocations in case user changed it in prefs
			ThreadUtils.executeAndWait(setupSaveDirLocation);

			if (sdWritable) {
				trackerIO.setFileName(FileNameUtils.getNextFilename());
				ThreadUtils.executeAndWait(new Runnable() {
					public void run() {
						((ApplicationSingleton)getApplication()).fileInProgress = trackerIO.startFile();
					}
				});
			} else {
				final String message = "External storage not available; can't record";
				Toast.makeText(this, message, Toast.LENGTH_LONG).show();
				logToScreen(message);
				Log.w(TAG, message);
				return;
			}
			fileNameLabel.setText(((ApplicationSingleton)getApplication()).fileInProgress.getName());
			startReceiving();        // Disk IO is done on the service's thread.
			logToScreen("Starting File Updates");
		} catch (RuntimeException e) {
			final String message = "Could not save: " + e;
			Toast.makeText(this, message, Toast.LENGTH_LONG).show();
			Log.w(TAG, message);
			startButton.setEnabled(true);
			return;
		}
		saving = true;
		paused = false;
		syncPauseButtonToState();
		stopButton.setEnabled(true);
	};

	OnClickListener stopButtonAction = v -> {
		stopReceiving();
		saving = false;
		paused = false;
		syncPauseButtonToState();
		stopButton.setEnabled(false);
		ThreadUtils.executeAndWait(new Runnable() {
			public void run() {
				trackerIO.endFile();
			}
		});
		String mesg = "Saved as " + trackerIO.getFile().getAbsolutePath();
		logToScreen("Stopping file updates; " + mesg);
		Toast.makeText(this, mesg, Toast.LENGTH_LONG).show();
		((ApplicationSingleton)getApplication()).fileInProgress = null;
		if (SettingsActivity.isAlwaysUpload(this)) {
			// Show even if we have a password, it's a confirmation to upload
			showDialog(DIALOG_OSM_CONFIRM_UPLOAD);
		}
		fileNameLabel.setText(FileNameUtils.getDefaultFilenameFormatWithExt());
		startButton.setEnabled(true);
	};

	OnClickListener voiceNoteButtonAction = v -> {
		logToScreen("Starting Voice Recording");
		// Use an Intent to get the Voice Record app going.
		// Intent soundIntent = new Intent(MediaStore.Audio.Media.RECORD_SOUND_ACTION);
		// GRRR, standard Sound Recorder doesn't accept MediaStore.EXTRA_OUTPUT)
		Intent soundIntent = new Intent(this, VoiceNoteActivity.class);
		// Set up file to save image into.
		soundFile = new File(MainActivity.getDataDirectory(), FileNameUtils
				.getNextFilename("mp3"));
		soundIntent.putExtra(MediaStore.EXTRA_OUTPUT,
				FileProvider.getUriForFile(MainActivity.this,
						getApplicationContext().getPackageName() + ".provider",
						soundFile));
		// And away we go!
		startActivityForResult(soundIntent, ACTION_TAKE_SOUNDBITE);
	};

	OnClickListener textNoteButtonAction = v -> {
		logToScreen("Starting Text Entry");
		try {
			startActivity(new Intent(this, TextNoteActivity.class));
		} catch (Exception e) {
			Toast.makeText(this, getString(R.string.cant_start_activity) + ": " + e, Toast.LENGTH_LONG).show();
		}
	};

	OnClickListener takePictureButtonAction = v -> {
		logToScreen("Starting Camera Activity");
		try {
			// Use an Intent to get the Camera app going.
			Intent imageIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
			// Set up file to save image into.
			imageFile = new File(MainActivity.getDataDirectory(),
					FileNameUtils.getNextFilename("jpg"));
			imageIntent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 1);

			imageIntent.putExtra(MediaStore.EXTRA_OUTPUT,
					FileProvider.getUriForFile(this,
							this.getApplicationContext().getPackageName() + ".provider",
							imageFile));
			imageIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
			// And away we go!
			startActivityForResult(imageIntent, ACTION_TAKE_PICTURE);
			logToScreen("Done with Camera Activity");
		} catch (Exception e) {
			Toast.makeText(this,
					getString(R.string.cant_start_activity) + ": " + e,
					Toast.LENGTH_LONG).show();
			Log.wtf(TAG, e);
		}
	};

	/** Called when an Activity we started for Result is complete */
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		logToScreen("In onActivityResult");
		super.onActivityResult(requestCode, resultCode, data);
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
				logToScreen("Pix Canceled!");
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
				logToScreen("Vox annuli!");
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
		// Calling super after populating the menu is necessary here to ensure that the
        // action bar helpers have a chance to handle this event.
        return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.settings:
			startActivity(new Intent(this, SettingsActivity.class));
			return true;
		case R.id.rerun_intro:
			Intent onboardIntent = new Intent(this, OnboardingActivity.class);
			onboardIntent.putExtra(SKIP_SKIP, true);
			startActivity(onboardIntent);
			return true;
		case R.id.suggest:
			startUrl("https://darwinsys.com/contact?subject='Software: Free Software Feedback'");
			return true;
		case R.id.privacy:
			startUrl("https://darwinsys.com/jpstrack/privacy.html");
			return true;
		case R.id.about:
			showDialog(DIALOG_ABOUT);
			return true;
		}
		return false;
	}

	public void startUrl(String url) {
		Uri uri = Uri.parse(url);
		try {
		startActivity(new Intent(Intent.ACTION_VIEW, uri));
		} catch (ActivityNotFoundException e) {
			Toast.makeText(this, "Can't start a browser for this URI: " + url, Toast.LENGTH_LONG).show();
		}
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

	public static File getDataDirectory() {
		return dataDir;
	}

	private void logToScreen(String string) {
		if (output == null) {
			Log.e(TAG, "output is NULL, unable to display: " + string);
			return;
		}
		output.append(string + "\n");
	}

}
