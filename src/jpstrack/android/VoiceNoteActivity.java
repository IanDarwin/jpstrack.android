package jpstrack.android;

import java.io.File;
import java.io.IOException;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;

public class VoiceNoteActivity extends Activity implements OnClickListener {
	private static final String PROG_NAME = "VoiceNote";
	MediaRecorder recorder  = new MediaRecorder();
	File soundDir = new File(SettingsActivity.getDirectory(this));
	File soundFile = null;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// Create a View with Pause, Save and Discard buttons, 
		View saveButton = findViewById(R.id.voicenote_save_button);
		saveButton.setOnClickListener(this);
		View discardButton = findViewById(R.id.voicenote_save_button);
		discardButton.setOnClickListener(this);
		startRecording();	// Start immediately - already pressed Voice Note button
	}
	
	@Override
	public void onClick(View v) {
		int source = v.getId();
		switch(source) {
		case R.id.voicenote_discard_button:
			discardRecording();
			break;
		case R.id.voicenote_save_button:
			saveRecording();
			break;
		default:
			Log.e(PROG_NAME, "Unexpected click");
		}
		this.finish();		// Back to main!
	}

	protected void startRecording() {
		recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
		recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
		recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
		try {
			soundFile = File.createTempFile("SOUNDFILE", ".mp3", soundDir);
		} catch (IOException e) {
			Log.e(PROG_NAME, "Could not save file");
			return;
		}

		recorder.setOutputFile(soundFile.getAbsolutePath());
		try {
			recorder.prepare();
		} catch (IOException e) {
			Log.e(PROG_NAME, "Could not prepare audio file");
		}
		recorder.start();
	}
	
	protected void discardRecording() {
		recorder.stop();
		recorder.release();
		soundFile.delete();
	}

	protected void saveRecording() {
		recorder.stop();
		recorder.release();
		processaudiofile();
	}

	protected void processaudiofile() {
		ContentValues values = new ContentValues(4);
		values.put(MediaStore.Audio.Media.TITLE, PROG_NAME + '-' + soundFile.getName());
		values.put(MediaStore.Audio.Media.MIME_TYPE, "audio/mp3");
		values.put(MediaStore.Audio.Media.DATA, soundFile.getAbsolutePath());
		values.put(MediaStore.Audio.Media.DATE_ADDED, (int) (System.currentTimeMillis() / 1000));
		ContentResolver contentResolver = getContentResolver();

		// XXX Start busy-notifier
		
		Uri base = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
		Uri newUri = contentResolver.insert(base, values);
		
		sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, newUri));
				
		// XXX Stop busy-notifier
	}
}
