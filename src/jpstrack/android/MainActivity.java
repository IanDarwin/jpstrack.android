package jpstrack.android;

import java.util.List;

import android.app.Activity;
import android.content.Intent;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.TextView;

public class Main extends Activity implements LocationListener, OnClickListener { 

	private static final int MIN_METRES = 1;
	private static final int MIN_SECONDS = 5;
	private static final String[] PROVIDER_STATUS_VALUES = { 
		"out of service",
		"temporarily unavailable", 
		"available"
	};

	private LocationManager mgr;
	private TextView output;
	private String preferred;
	private EditText latOutput, longOutput;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		output = (TextView) findViewById(R.id.output);
		
		mgr = (LocationManager) getSystemService(LOCATION_SERVICE); 
		for (String prov : mgr.getAllProviders()) {
			log("Provider present: " + prov);
		}

		Criteria criteria = new Criteria();
		criteria.setAccuracy(Criteria.ACCURACY_FINE);
		List<String> providers = mgr.getProviders(criteria, true);
		if (providers == null || providers.size() == 0) {
			log("CANNOT GET GPS SERVICE");
			return;
		}
 		preferred = providers.get(0);			// first == preferred
		log("Preferred provider is: " + preferred);

		final Location lastKnownLocation = mgr.getLastKnownLocation(preferred);
		if (lastKnownLocation != null) {
			printLocation("Last known location =", lastKnownLocation); 
		}
		
		// THE GUI
		latOutput = (EditText) findViewById(R.id.lat_output);
		latOutput.setEnabled(false);
		longOutput = (EditText) findViewById(R.id.lon_output);
		longOutput.setEnabled(false);
		View startButton = findViewById(R.id.start_button);
		startButton.setOnClickListener(this);
		TextView fileNameLabel = (TextView) findViewById(R.id.filename_label);
		fileNameLabel.setText("YYYYMMDDHHMM.gpx");	// xxx from Prefs or Model??
		View stopButton = findViewById(R.id.stop_button);
		stopButton.setOnClickListener(this);
		// textNoteButton
		// voiceNoteButton
		// takePictureButton

	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.start_button:
			log("Start");
			mgr.requestLocationUpdates(preferred, MIN_SECONDS * 1000, MIN_METRES, this);
			break;
		case R.id.stop_button:
			log("Stop");
			mgr.removeUpdates(this);
			break;
		default:
			log("Unexpected Click from " + v.getId());
			break;
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		MenuInflater inf = getMenuInflater();
		inf.inflate(R.menu.menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.settings:
			startActivity(new Intent(this, SettingsActivity.class));
			return true;
		}
		return false;
	}

	/** From LocationListener, called when the location changes, obviously */
	@Override
	public void onLocationChanged(Location location) {
		printLocation("Current Location", location);
		latOutput.setText(Double.toString(location.getLatitude()));
		longOutput.setText(Double.toString(location.getLongitude()));
	}

	/** From LocationListener, providing very bad news... */
	@Override
	public void onProviderDisabled(String provider) {
		log("Provider disabled: " + provider);
	}

	/** From LocationListener, things are looking up! */
	@Override
	public void onProviderEnabled(String provider) {
		log("Provider enabled: " + provider);
	}

	/** From LocationListener, something changed. */
	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
		String mesg = String.format("Provider %s status %s", provider, PROVIDER_STATUS_VALUES[status]);
		log(mesg);
	}

	private void log(String string) {
		output.append(string + "\n");
	}

	private void printLocation(String type, Location location) {
		if (location == null)
			log("Location[unknown]");
		else
			log(type + " " + "[" + location.getLatitude() + "," + location.getLongitude() + "]");
	}

}
