package jpstrack.android;

import java.io.File;

import jpstrack.fileio.FileNameUtils;
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
import android.widget.Toast;

public class VoiceNoteActivity extends Activity implements OnClickListener {
	private static final String PROG_NAME = "VoiceNote";
	MediaRecorder recorder  = new MediaRecorder();
	private File soundDir;
	private File soundFile;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.voicenote);
		soundDir = Main.getDataDir();

		// Create a View with Pause, Save and Discard buttons, 
		View saveButton = findViewById(R.id.voicenote_save_button);
		saveButton.setOnClickListener(this);
		View discardButton = findViewById(R.id.voicenote_discard_button);
		discardButton.setOnClickListener(this);
		startRecording();	// Start immediately - already pressed Voice Note button
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
			Log.e(PROG_NAME, "Unexpected click");
		}
		this.finish();		// Back to main!
	}

	protected void startRecording() {
		recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
		recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
		recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
		try {
			soundDir.mkdirs();
			soundFile = new File(soundDir, FileNameUtils.getNextFilename(".mp3"));
			soundFile.createNewFile();
			recorder.setOutputFile(soundFile.getAbsolutePath());
			recorder.prepare();
			recorder.start();
		} catch (Exception e) {
			Log.e(PROG_NAME, "Could not save file:" + e);
			this.finish();
			throw new RuntimeException("Could not start: " + e);
		}
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
