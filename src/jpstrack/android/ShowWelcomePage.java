package jpstrack.android;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

public class ShowWelcomePage extends Activity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		TextView textView = new TextView(this);
		textView.setText("Not written yet");
		setContentView(textView);
	}
}
