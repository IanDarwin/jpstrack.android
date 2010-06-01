package jpstrack.android;

import android.app.Activity;
import android.os.Bundle;
import android.hardware.Camera;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.View.OnClickListener;

public class CameraNote extends Activity implements SurfaceHolder.Callback, OnClickListener {

	private Camera cam;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// TODO Auto-generated method stub
	}
	
	@Override
	public void onClick(View v) {
		int id = v.getId();
		switch(id) {
		// case R.id.cameranote_takepicture_button:
		//	cam.takePicture(...);
		//	savePictureToDisk();
		//	break;
		default:
			break;
		}
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		// TODO Auto-generated method stub
		
	}

}
