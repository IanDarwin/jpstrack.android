package jpstrack.android;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class OnboardingActivity extends Activity implements OnClickListener {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.onboarding);
		for (int id : new int[]{R.id.skipButton1, R.id.skipButton2, R.id.videoButton, R.id.webButton}) {
			System.out.println(id);
			Button b = (Button)findViewById(id);
			b.setOnClickListener(this);
		}
	}


	@Override
	public void onClick(View v) {
		int id = v.getId();
		switch(id) {
		case R.id.skipButton1:
		case R.id.skipButton2:
			// startActivity(new Intent(this, Main.class));
			finish();	// returns to main?
			break;
		case R.id.videoButton:
			startActivity(new Intent(this, ShowWelcomeVideo.class));
			break;
		case R.id.webButton:
			startActivity(new Intent(this, ShowWelcomePage.class));
			break;
		default:
		}
		SettingsActivity.setSeenWelcome(this, true);
	}
}
