package jpstrack.android;

import java.io.File;

import jpstrack.fileio.FileNameUtils;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Toast;

public class CameraNoteActivity extends Activity {

	private static final int ACTION_TAKE_PICTURE = 1;
	private static final int ACTION_EDIT_PICTURE = 1;

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
		startActivityForResult(intent, ACTION_TAKE_PICTURE);
	}
	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch(requestCode) {

		// The user asked to take a picture, and that's been done, see if they want to edit (e.g. crop?) it...
		case ACTION_TAKE_PICTURE:
			switch(resultCode) {
			case Activity.RESULT_OK:
				if (imageFile.exists())
					Toast.makeText(this, "Bitmap saved as " + imageFile.getAbsoluteFile(), Toast.LENGTH_LONG).show();
					boolean edit = false;
					if (edit) {
						try {
							Uri imageToEditUri = Uri.fromFile(imageFile);
							String imageToEditMimeType = "image/*";
							Intent launchEditor = new Intent();
							launchEditor.setAction(Intent.ACTION_EDIT);
							launchEditor.setDataAndType(imageToEditUri, imageToEditMimeType);

							startActivityForResult(launchEditor, ACTION_EDIT_PICTURE);
						}
						catch (ActivityNotFoundException e)
						{
							new AlertDialog.Builder(this).setTitle("Can't Edit").setMessage(
							"You need a graphics editor e.g. Photoshop Express 1.1").show();
						}
					}
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
				break;
			}
			break;

		// OK, the user asked to edit, and the edit is completed.
//		case ACTION_EDIT_PICTURE:
//			switch(resultCode) {
//			case Activity.RESULT_OK:
//				if (imageFile.exists()) {
//					Toast.makeText(this, "Bitmap saved as " + data.getData(), Toast.LENGTH_LONG).show();
//				}
//				break;
//			case Activity.RESULT_CANCELED:
//				//  no blather required!
//				break;
//			default:
//				Toast.makeText(this, "Unexpected resultCode: " + resultCode, Toast.LENGTH_LONG).show();
//				break;
//			}
//			break;
		default:
				Toast.makeText(this, "UNEXPECTED ACTIVITY COMPLETION", Toast.LENGTH_LONG).show();
		}
		finish();	// back to main app
	}
}
