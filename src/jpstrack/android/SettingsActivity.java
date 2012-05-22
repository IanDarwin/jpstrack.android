package jpstrack.android;

import android.content.Context;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

public class SettingsActivity extends PreferenceActivity {

	static final String DIRECTORY_NAME = "jpstrack.android";
	
	static final String OPTION_DIR = "dir";
	static final String OPTION_SEEN_WELCOME = "seen_welcome";
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
	
	public static boolean hasSeenWelcome(final Context context) {
		class Meh extends Thread implements Runnable {
			boolean seen;
			@Override
			public void run() {
				seen = PreferenceManager.getDefaultSharedPreferences(context).
						getBoolean(OPTION_SEEN_WELCOME, false);
			}
			public boolean getSeen() {
				return seen;
			}
		}
		Meh t = new Meh();
		t.start();
		try {
			t.join();
		} catch (InterruptedException e) {
			throw new RuntimeException("Interrupted? DOI! " + e);
		}
		return t.getSeen();
	}

	public static void setSeenWelcome(final Context context, final boolean seenWelcome) {
		ThreadUtils.execute(new Runnable() {
			@Override
			public void run() {
				PreferenceManager.getDefaultSharedPreferences(context).
				edit().putBoolean(OPTION_SEEN_WELCOME, seenWelcome).commit();
			}			
		});
	}

}
