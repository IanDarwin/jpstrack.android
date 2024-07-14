package jpstrack.android;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * The Settings or Preferences activity.
 * @author Ian Darwin
 */
public class SettingsActivity extends PreferenceActivity {

	protected static ExecutorService tPool = Executors.newSingleThreadExecutor();

	protected static SharedPreferences sharedPrefs;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		tPool.submit(() -> addPreferencesFromResource(R.layout.settings));
	}

	protected static synchronized SharedPreferences getSharedPrefs(Context context) {
		if (sharedPrefs == null) {
			sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
		}
		return sharedPrefs;
	}

	static final String DIRECTORY_NAME = "jpstrack";
	
	// Keys MUST agree with keys defined in settings.xml!

	static final String OPTION_SEEN_EULA = "accepted_eula";	// NOT IN GUI FOR OBVIOUS REASONS
	static final String OPTION_DIR = "dir";
	static final String OPTION_SEEN_WELCOME = "seen_welcome"; // Ditto
	//static final String OPTION_FORMAT = "format";
	static final String OPTION_OSM_USER = "osm_username";
	//static final String OPTION_OSM_PASS = "osm_password";
	private static String OPTION_ALWAYS_UPLOAD = "osm_alwaysUpload";
	private static String OPTION_USE_SANDBOX = "osm_useSandbox";

	// The rest of this code is just convenience routines
	// for the other parts of the application

	/** No set method, it is set by our PreferencesActivity subclass */
	public static String getDirectory(Context context) {
		return getSharedPrefs(context).getString(OPTION_DIR, MainActivity.dataDir.getAbsolutePath());
	}
	
	/** No set method, it is set by our PreferencesActivity subclass */
	public static String getOSMUserName(Context context) {
		return getSharedPrefs(context).getString(OPTION_OSM_USER, null);
	}
	
	/** No set method, it is set by our PreferencesActivity subclass */
	public static boolean isAlwaysUpload(Context context) {
		try {
			return tPool.submit(() -> getSharedPrefs(context).getBoolean(OPTION_ALWAYS_UPLOAD, false)).get();
		} catch (ExecutionException | InterruptedException e) {
			throw new RuntimeException();
		}
	}

	public static void setSeenEula(final Context context, final boolean seenValue) {
		tPool.submit(() -> getSharedPrefs(context).edit().putBoolean(OPTION_SEEN_EULA, seenValue).commit());
	}
	
	public static boolean hasSeenEula(final Context context) {
		try {
			return tPool.submit(() -> getSharedPrefs(context).getBoolean(OPTION_SEEN_EULA, false)).get();
		} catch (ExecutionException | InterruptedException e) {
			throw new RuntimeException();
		}
	}
	
	public static void setSeenWelcome(final Context context, final boolean seenValue) {
		tPool.submit(() -> getSharedPrefs(context).edit().putBoolean(OPTION_SEEN_WELCOME, seenValue).commit());
	}
	
	public static boolean hasSeenWelcome(final Context context) {
		try {
			return tPool.submit(() -> getSharedPrefs(context).getBoolean(OPTION_SEEN_WELCOME, false)).get();
		} catch (ExecutionException | InterruptedException e) {
			throw new RuntimeException();
		}
	}
	
	public static boolean useSandbox(final Context context) {
		try {
			return tPool.submit(() -> getSharedPrefs(context).getBoolean(OPTION_USE_SANDBOX, false)).get();
		} catch (ExecutionException | InterruptedException e) {
			throw new RuntimeException();
		}
	}
}
