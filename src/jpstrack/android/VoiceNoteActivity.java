package jpstrack.android;

import java.io.File;

import android.app.Activity;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Toast;

/**
 * Offer a "voice note" facility so the user can record comments.
 * We use this instead of the default Sound Record app since unlike
 * the standard Video Recorder, the standard Voice Recorder app
 * blatantly ignores Intent extra "Extra Output" for filename.
 * 
 * onCreate() calls startRecording(), since the user already pressed a button
 * with "start recording" function; we offer discard and save buttons only.
 * @author Ian Darwin
 */
public class VoiceNoteActivity extends Activity implements OnClickListener {
	MediaRecorder recorder  = null;
	private String soundFile;
	private boolean recording;
	
	class Wrapper {
		MediaRecorder recorder;
		String soundFile;
		@Override
		public String toString() {
			return "Wrapper[" + recorder + "; " + soundFile + "]";
		}
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (!Main.isSdWritable()) {
			Toast.makeText(this, "SD Card not writable", Toast.LENGTH_LONG).show();
			finish();
			return;
		}
		
		// View has only Save and Discard buttons, 
		setContentView(R.layout.voicenote);
		View saveButton = findViewById(R.id.voicenote_save_button);
		saveButton.setOnClickListener(this);
		View discardButton = findViewById(R.id.voicenote_discard_button);
		discardButton.setOnClickListener(this);
		Wrapper w = (Wrapper) getLastNonConfigurationInstance();
		Log.d(Main.TAG, "LastConConfigurationInstance gave us " + w);
		if (w == null) {
			startRecording();	// Start immediately - already pressed Voice Note button
		} else {
			continueRecording(w);
		}
	}
	
	@Override
	public void onClick(View v) {
		int source = v.getId();
		switch(source) {
		case R.id.voicenote_save_button:
			saveRecording();
			Toast.makeText(this, "Saved voice note into " + soundFile, Toast.LENGTH_SHORT).show();
			break;
		case R.id.voicenote_discard_button:
			discardRecording();
			break;
		default:
			Log.e(Main.TAG, "Unexpected click");
		}
		this.finish();		// Back to main!
	}
	
	@Override
	public void onBackPressed() {
		if (recording) {
			return;			// Can't back out, must save or cancel.
		}
		super.onBackPressed();
	}

	protected void startRecording() {
		recorder = new MediaRecorder();
		recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
		recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
		recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
		try {
			Uri soundUri = getIntent().getParcelableExtra(MediaStore.EXTRA_OUTPUT);			
			soundFile = soundUri.getPath();
			recorder.setOutputFile(soundFile);
			Log.d(Main.TAG, "outputting to " + soundUri.getPath());
			recorder.prepare();
			recorder.start();
			recording = true;
		} catch (Exception e) {
			final String message = "Could not create file:" + e;
			Log.e(Main.TAG, message);
			Toast.makeText(this, message, Toast.LENGTH_LONG).show();
			this.finish();
		}
	}
	
	/**
	 * Restore state after configuration change (rotation)
	 * @param w
	 */
	private void continueRecording(Wrapper w) {
		soundFile = w.soundFile;
		recorder = w.recorder;
	}

	
	protected void discardRecording() {
		recorder.stop();
		recording = false;
		recorder.release();
		new File(soundFile).delete();
	}

	protected void saveRecording() {
		recorder.stop();
		recording = false;
		recorder.release();
		// We don't tell the MediaStore about it as it's not music!
	}
	
	@Override
	public Object onRetainNonConfigurationInstance() {
		Wrapper w = new Wrapper();
		w.soundFile = soundFile;
		w.recorder = recorder;
		return w;
	}
}
