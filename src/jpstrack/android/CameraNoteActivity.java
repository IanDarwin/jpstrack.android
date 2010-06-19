package jpstrack.android;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Toast;

public class CameraNoteActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// Use an Intent to get the Camera app going.
		Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
		startActivityForResult(intent, 0);
	}
	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch(requestCode) {
		case 0: // take picture
			switch(resultCode) {
			case Activity.RESULT_OK:
				Bitmap ret = (Bitmap) data.getExtras().get("data");
				Toast.makeText(this, "Bitmap returned is " + ret.getWidth() + "x" + ret.getHeight(), Toast.LENGTH_LONG).show();
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
