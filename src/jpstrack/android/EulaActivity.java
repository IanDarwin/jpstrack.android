package jpstrack.android;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;

public class EulaActivity extends Activity {
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.eula);
		WebView  wv = (WebView) findViewById(R.id.eula_text);
		wv.loadUrl("file:///android_asset/eula.html");
	}

	public void accept(View v) {
		Log.d(Main.TAG, "User accepted EULA!");
		SettingsActivity.setSeenEula(this, true);
		finish();
	}
	
	public void reject(View v) {
		Log.d(Main.TAG, "User REJECTED EULA!");	
		System.exit(-1);
	}
}
