package jpstrack.android;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.MediaController;
import android.widget.VideoView;

public class ShowWelcomeVideo extends Activity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.onboard_video);
		VideoView vw = (VideoView) findViewById(R.id.onboard_videoView);
		// This URL fetches a file from the res/raw folder.
		String VIDEO_URL = "android.resource://" + getPackageName() +  "/" + R.raw.welcomevideo;
		vw.setVideoURI(Uri.parse(VIDEO_URL));
		vw.setMediaController(new MediaController(this));
		vw.start();
		
		Button b = (Button)findViewById(R.id.onboard_videoDoneButton);
		b.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				finish();
			}
		});
	}
}
