package jpstrack.android;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

import jpstrack.fileio.FileNameUtils;
import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.Toast;

public class TextNoteActivity extends Activity implements OnClickListener {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		if (!Main.isSdWritable()) {
			Toast.makeText(this, "SD Card is not writable", Toast.LENGTH_LONG).show();
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
			doSave();
			finish();
			break;
		case R.id.textnote_discard_button:
			finish();
			break;
		default:
			break;
		}
	}

	private void doSave() {
		EditText tv = (EditText) findViewById(R.id.textnote_text);
		File f = new File(Main.getDataDir(), FileNameUtils.getNextFilename("txt"));
		try {
			PrintWriter out = new PrintWriter(f);
			out.print(tv.getText().toString());
			out.close();
			Toast.makeText(this, "Saved text note into " + f, Toast.LENGTH_SHORT).show();
		} catch (IOException e) {
			final String message = "Can't create text file " + f + "(" + e + ")";
			Log.e(Main.TAG, message);
			Toast.makeText(this, message, Toast.LENGTH_LONG).show();
			// Don't finish! Let them try later
		}
	}
}
