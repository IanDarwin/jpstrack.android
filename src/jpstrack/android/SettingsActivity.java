package jpstrack.android;

import android.content.Context;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

public class SettingsActivity extends PreferenceActivity {

	static final String DIRECTORY_NAME = "jpstrack.android";
	
	static final String OPTION_DIR = "dir";
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

}
