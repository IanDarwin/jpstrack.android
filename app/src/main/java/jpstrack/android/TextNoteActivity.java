package jpstrack.android;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.Toast;

import jpstrack.fileio.FileNameUtils;

public class TextNoteActivity extends Activity implements OnClickListener {

	final static String TAG = TextNoteActivity.class.getSimpleName();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		if (!MainActivity.sdWritable) {
			Toast.makeText(this, TAG + ": SD Card is not writable", Toast.LENGTH_LONG).show();
			finish();
			return;
		}
		
		setContentView(R.layout.textnote);
		View saver = findViewById(R.id.textnote_save_button);
		saver.setOnClickListener(this);
		View killer = findViewById(R.id.textnote_discard_button);
		killer.setOnClickListener(this);
	}
	
	@Override
	public void onClick(View v) {
		int source = v.getId();
		switch(source) {
		case R.id.textnote_save_button:
			Log.d(TAG, "save");
			doSave();
			finish();
			break;
		case R.id.textnote_discard_button:
			Log.d(TAG, "discard");
			finish();
			break;
		default:
			Log.d(TAG, "Unexpected onClick from id " + source);
			break;
		}
	}

	/** Save the text file. Run in a thread pool */
	private void doSave() {
		Log.d(TAG, "in doSave");
		EditText tv = (EditText) findViewById(R.id.textnote_text);
		File f = new File(MainActivity.dataDir, FileNameUtils.getNextFilename("txt"));
		MainActivity.threadPool.submit( () -> {
			try {
				PrintWriter out = new PrintWriter(f);
				out.print(tv.getText().toString());
				out.close();
			} catch (IOException e) {
				final String message = "Can't create text file " + f + "(" + e + ")";
				Log.e(MainActivity.TAG, message);
				Toast.makeText(this, message, Toast.LENGTH_LONG).show();
				return;
			}
		});
		Toast.makeText(TextNoteActivity.this,
				"Saved text note into " + f, Toast.LENGTH_SHORT).show();
	}
}
