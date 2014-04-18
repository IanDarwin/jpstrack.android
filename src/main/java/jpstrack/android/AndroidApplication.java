package jpstrack.android;

import java.util.HashMap;
import java.util.Map;

import android.app.Application;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.Tracker;

/**
 * The Android singleton for this application
 */
public class AndroidApplication extends Application {

	private static final String PROPERTY_ID_JPSTRACK = "UA-33064318-2";
	private Map<TrackerName, Tracker> mTrackers = new HashMap<>();

	synchronized Tracker getTracker(TrackerName trackerId) {
		if (!mTrackers.containsKey(trackerId)) {

			GoogleAnalytics analytics = GoogleAnalytics.getInstance(this);
			Tracker t;
			switch (trackerId) {
			case APP_TRACKER:
				t = analytics.newTracker(PROPERTY_ID_JPSTRACK);
				break;
			case COMPANY_TRACKER:
				t = analytics.newTracker(PROPERTY_ID_JPSTRACK);
				break;
			default:
				throw new IllegalArgumentException("Unhandled enum case: "
						+ trackerId);
			}
			mTrackers.put(trackerId, t);
		}
		return mTrackers.get(trackerId);
	}
}
