package jpstrack.android;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

/** The Service is used to keep the GPS data flowing even
 * when our App is not current.
 * @author ian
 */
public class GpsService extends Service {

	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}

}
