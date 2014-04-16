package edu.pdx.cycleor;

import java.util.ArrayList;

import android.app.Activity;
import android.graphics.Point;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.Window;
import android.widget.TextView;

import edu.pdx.cycleor.R;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

public class ShowMap extends Activity {
	private GoogleMap map;
	private ArrayList<CyclePoint> gpspoints;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        getWindow().requestFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.mapview);

        Bundle cmds = getIntent().getExtras();

        if (cmds == null) return;

        //Get trip and details
        TripData trip = TripData.fetchTrip(this, cmds.getLong("showtrip"));
        gpspoints = trip.getPoints();

        //Upload the trip if it hasn't yet been sent
        if (trip.status < TripData.STATUS_SENT && cmds.getBoolean("uploadTrip", false)) {
    	    // And upload to the cloud database, too!  W00t W00t!
           TripUploader uploader = new TripUploader(ShowMap.this);
           uploader.execute(trip.tripid);
    	}

        // Show trip details
        ((TextView) findViewById(R.id.text1)).setText(trip.purp);
        ((TextView) findViewById(R.id.text2)).setText(trip.info);
        ((TextView) findViewById(R.id.text3)).setText(trip.fancystart);

        //Set up the map
        GoogleMap map = ((MapFragment) getFragmentManager().findFragmentById(R.id.map)).getMap();

        CyclePoint startPoint = null;
        CyclePoint endPoint = null;
        LatLngBounds.Builder tripBoundsBuilder = new LatLngBounds.Builder();

        PolylineOptions tripLine = new PolylineOptions().color(getResources().getColor(R.color.accent_color));

        for(CyclePoint cyclepoint : gpspoints) {
        	//Add point to boundary calculator
        	tripBoundsBuilder.include(cyclepoint.latLng);

        	//Add to the trip line
        	tripLine.add(cyclepoint.latLng);

        	if (startPoint == null) startPoint = cyclepoint;
        	endPoint = cyclepoint;
        }

        LatLngBounds tripBounds = tripBoundsBuilder.build();
        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);

        int minSize = Math.min(size.x, size.y);
        Log.i(getClass().getName(), String.valueOf(minSize));

        map.moveCamera(CameraUpdateFactory.newLatLngBounds(tripBounds, minSize, minSize, 0));
        map.addPolyline(tripLine);

        
        //Show the first and last markers
        map.addMarker(new MarkerOptions().icon(BitmapDescriptorFactory.fromResource(R.drawable.trip_start)).anchor(0.5f, 0.5f).position(startPoint.latLng));
        map.addMarker(new MarkerOptions().icon(BitmapDescriptorFactory.fromResource(R.drawable.trip_end)).anchor(0.5f, 0.5f).position(endPoint.latLng));
	}
}
