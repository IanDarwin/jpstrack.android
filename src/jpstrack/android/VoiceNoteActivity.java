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

/**
 * Offer a "voice note" facility so the user can record comments.
 * onCreate calls startRecording(), since the user already pressed a button
 * with "start recording" function; we offer discard and save buttons only.
 * @author Ian Darwin
 */
public class VoiceNoteActivity extends Activity implements OnClickListener {
	MediaRecorder recorder  = new MediaRecorder();
	private File soundFile;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (!Main.isSdWritable()) {
			Toast.makeText(this, "SD Card is not writable", Toast.LENGTH_LONG).show();
			finish();
			return;
		}
		
		// View has only Save and Discard buttons, 
		setContentView(R.layout.voicenote);
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
			Log.e(Main.TAG, "Unexpected click");
		}
		this.finish();		// Back to main!
	}

	protected void startRecording() {
		
		recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
		recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
		recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
		try {
			File soundDir = Main.getDataDir();
			soundDir.mkdirs();
			soundFile = new File(soundDir, FileNameUtils.getNextFilename("mp3"));
			soundFile.createNewFile();
			recorder.setOutputFile(soundFile.getAbsolutePath());
			recorder.prepare();
			recorder.start();
		} catch (Exception e) {
			final String message = "Could not save file:" + e;
			Log.e(Main.TAG, message);
			Toast.makeText(this, message, Toast.LENGTH_LONG);
			this.finish();
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
		values.put(MediaStore.Audio.Media.TITLE, Main.TAG + '-' + soundFile.getName());
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
