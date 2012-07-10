package jpstrack.android;

import android.content.Context;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

public class SettingsActivity extends PreferenceActivity {

	static final String DIRECTORY_NAME = "jpstrack";
	
	static final String OPTION_DIR = "dir";
	static final String OPTION_SEEN_EULA = "accepted_eula";	// NOT IN GUI FOR OBVIOUS REASONS
	static final String OPTION_SEEN_WELCOME = "seen_welcome"; // Ditto
	//static final String OPTION_FORMAT = "format";
	//static final String OPTION_OSM_USER = "osm_username";
	//static final String OPTION_OSM_PASS = "osm_password";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.settings);
	}
	
	public static String getDirectory(Context context) {
		return PreferenceManager.getDefaultSharedPreferences(context).getString(OPTION_DIR, null);
	}
	
	private static class Getter extends Thread {
		public Getter(Context context, String optionName) {
			super();
			this.context = context;
			this.optionName = optionName;
		}
		Context context;
		String optionName;
		boolean seen;
		
		@Override
		public void run() {
			seen = PreferenceManager.getDefaultSharedPreferences(context).
					getBoolean(optionName, false);
		}
		public boolean getSeen() {
			return seen;
		}
	}
	
	public static boolean hasSeenEula(final Context context) {		
		Getter t = new Getter(context, OPTION_SEEN_EULA);
		ThreadUtils.executeAndWait(t);
		return t.getSeen();
	}
	
	public static boolean hasSeenWelcome(final Context context) {		
		Getter t = new Getter(context, OPTION_SEEN_WELCOME);
		ThreadUtils.executeAndWait(t);
		return t.getSeen();
	}
	
	private static class Setter extends Thread {
		public Setter(Context context, String optionName, boolean seenValue) {
			super();
			this.context = context;
			this.optionName = optionName;
			this.seenValue = seenValue;
		}
		Context context;
		String optionName;
		boolean seenValue;
		@Override
		public void run() {
			PreferenceManager.getDefaultSharedPreferences(context).
			edit().putBoolean(optionName, seenValue).commit();
		}
	}

	public static void setSeenEula(final Context context, final boolean seenValue) {
		ThreadUtils.execute(new Setter(context, OPTION_SEEN_EULA, seenValue));
	}
	
	public static void setSeenWelcome(final Context context, final boolean seenValue) {
		ThreadUtils.execute(new Setter(context, OPTION_SEEN_WELCOME, seenValue));
	}

}
