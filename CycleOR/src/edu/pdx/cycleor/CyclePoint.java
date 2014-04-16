package edu.pdx.cycleor;

import com.google.android.gms.maps.model.LatLng;



class CyclePoint {
	double latitude;
	double longitude;
	LatLng latLng;
	double time;
	float accuracy;
	double altitude;
	float speed;

	public CyclePoint(double latitude, double longitude, double time, float accuracy, double altitude, float speed) {
    	this.latitude = latitude;
    	this.longitude = longitude;
		this.time = time;
		this.accuracy = accuracy;
		this.altitude = altitude;
		this.speed = speed;
		this.latLng = new LatLng(latitude, longitude);
	}
}
