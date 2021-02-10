package jpstrack.android;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

public class OnboardingActivity extends Activity implements OnClickListener {
	private final static String TAG = MainActivity.TAG + ".Onboarding";
	private final String VIDEO_TUTORIAL_URL = "http://darwinsys.com/jpstrack/tutorialvideo-url.txt";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.onboarding);
		
		// If called from Main's Menu, intent will have extra boolean
        // reminding us to skip the skip buttons
		boolean skipSkip = getIntent().getBooleanExtra(MainActivity.SKIP_SKIP, false);
		if (skipSkip) {
			ViewGroup layout = (ViewGroup) findViewById(R.id.onboardLayout);
			layout.removeView(findViewById(R.id.skipButton1));
			layout.removeView(findViewById(R.id.skipButton2));
		}
		
		// For each button that's still around, add us as a click listener
		for (int id : new int[]{R.id.skipButton1, R.id.skipButton2, R.id.videoButton, R.id.webButton}) {
			final View view = findViewById(id);
			if (view != null && view instanceof Button) {
				Button b = (Button)view;
				b.setOnClickListener(this);
			}
		}
	}


	@Override
	public void onClick(View v) {
		int id = v.getId();
		switch(id) {
		case R.id.skipButton1:
		case R.id.skipButton2:
			Log.d(TAG, "Skip Tutorial");
			finish();
			break;
		case R.id.webButton:
			Log.d(TAG, "Web Tutorial");
			startActivity(new Intent(this, ShowWelcomePage.class));
			finish(); // So user goes back to main, not to here.
			break;
		case R.id.videoButton:
			Log.d(TAG, "Video Tutorial");
			// The implementation of video is:
			// 1) Download the variable URI for the video from a fixed location.
			// 2) Start an activity to view whatever it points to.
			// This gives me maximal flexibility to change the video, something
			// YouTube would never do (you can not remove or replace on YouTube).
			// Open the URL and get a Reader from it. Do in a thread.
			Thread t = new Thread(new Runnable() {
				@Override
				public void run() {
					try {
						final BufferedReader is = 
								new BufferedReader(new InputStreamReader(
										new URL(VIDEO_TUTORIAL_URL).openStream()));
						String line = is.readLine();
						is.close();
						Log.d(TAG, "Video URL is: " + line);
						Uri uri = Uri.parse(line);
						try {
							startActivity(new Intent(Intent.ACTION_VIEW, uri));
						} catch (ActivityNotFoundException e) {
							Toast.makeText(OnboardingActivity.this, "Can't start activity for this URI: " + uri, Toast.LENGTH_LONG).show();
						}
					} catch (final IOException e) {
						Log.e(TAG, "Failure", e);
						runOnUiThread(new Runnable() {
							@Override
							public void run() {
								Toast.makeText(OnboardingActivity.this, "Video failure: " + e, Toast.LENGTH_LONG).show();			
							}	
						});	
					}
				}					
			});
			t.start();
			try {
				t.join();
			} catch (InterruptedException stupidException) {
				// empty - canthappen
			}
			finish(); // Ditto
			break;
		default:
		}
		SettingsActivity.setSeenWelcome(this, true);
	}
}
