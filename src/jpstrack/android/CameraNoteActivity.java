package jpstrack.android;

import java.io.File;

import jpstrack.fileio.FileNameUtils;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.widget.Toast;

public class CameraNoteActivity extends Activity {

	private File imageFile;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// Use an Intent to get the Camera app going.
		Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
		// Set up file to save image into.
		imageFile = new File(Main.getDataDir(), FileNameUtils.getNextFilename("jpg"));
		Uri uri = Uri.fromFile(imageFile);
		intent.putExtra(MediaStore.EXTRA_OUTPUT, uri);
		intent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 1);
		// And away we go!
		startActivityForResult(intent, 0);
	}
	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch(requestCode) {
		case 0: // take picture
			switch(resultCode) {
			case Activity.RESULT_OK:
				if (imageFile.exists())
					Toast.makeText(this, "Bitmap saved as " + imageFile.getAbsoluteFile(), Toast.LENGTH_LONG).show();
				else {
					AlertDialog.Builder alert = new AlertDialog.Builder(this);
					alert.setTitle("Inconnu").setMessage("Camera Intent returned OK but image not created!").show();
				}
				break;
			case Activity.RESULT_CANCELED:
				//  no blather required!
				break;
			default:
				Toast.makeText(this, "Unexpected resultCode: " + resultCode, Toast.LENGTH_LONG).show();
			}
			break;
		default:
				Toast.makeText(this, "UNEXPECTED ACTIVITY COMPLETION", Toast.LENGTH_LONG).show();
		}
		finish();	// back to main app
	}

}
