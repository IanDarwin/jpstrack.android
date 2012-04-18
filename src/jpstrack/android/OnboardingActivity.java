package jpstrack.android;

import android.app.Activity;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;

public class OnboardingActivity extends Activity implements OnClickListener {
	private static String TAG = Main.TAG + ".Onboarding";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.onboarding);
		
		// If called from Main's Menu, intent will have extra boolean
        // reminding us to skip the skip buttons
		boolean skipSkip = getIntent().getBooleanExtra(Main.SKIP_SKIP, false);
		if (skipSkip) {
			ViewGroup layout = (ViewGroup) findViewById(R.id.onboardLayout);
			layout.removeView(findViewById(R.id.skipButton1));
			layout.removeView(findViewById(R.id.skipButton2));
		}
		
		// For each button that's still around, add us as a click listener
		for (int id : new int[]{R.id.skipButton1, R.id.skipButton2, R.id.videoButton, R.id.webButton}) {
			Button b = (Button)findViewById(id);
			if (b != null) {
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
			startActivity(new Intent(this, ShowWelcomeVideo.class));
			finish(); // Ditto
			break;
		default:
		}
		SettingsActivity.setSeenWelcome(this, true);
	}
}
