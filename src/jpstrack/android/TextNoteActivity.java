package jpstrack.android;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

import jpstrack.fileio.FileNameUtils;
import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;

public class TextNoteActivity extends Activity implements OnClickListener {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
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
		} catch (IOException e) {
			throw new RuntimeException("Can't create text file " + f);
		}
	}
}
