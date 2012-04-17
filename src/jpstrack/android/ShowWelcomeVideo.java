package jpstrack.android;

import android.app.Activity;
import android.media.MediaPlayer;
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
		MediaPlayer player = MediaPlayer.create(this, R.raw.jpstrack_tutorial);
		if (player != null) {
			player.start();
		}
		MediaController controller = new MediaController(this);
		controller.setMediaPlayer(vw);
		vw.setMediaController(controller);
		
		Button b = (Button)findViewById(R.id.onboard_videoDoneButton);
		b.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				finish();
			}
		});
	}
}
