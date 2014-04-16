package edu.pdx.cycleor;

import java.util.ArrayList;

import android.content.Context;
import android.database.Cursor;
import android.location.Location;
import android.util.Log;

public class TripData {
	long tripid;
	double startTime = 0;
	double endTime = 0;
	int numpoints = 0;
	double latestLatitude, latestLongitude;
	int status;
	float distance;
	String purp, fancystart, info;
	//private ItemizedOverlayTrack gpspoints;
	CyclePoint startpoint, endpoint;
	double totalPauseTime = 0;
	double pauseStartedAt = 0;

	DbAdapter mDb;

    public static int STATUS_INCOMPLETE = 0;
    public static int STATUS_COMPLETE = 1;
    public static int STATUS_SENT = 2;

	public static TripData createTrip(Context c) {
		TripData t = new TripData(c.getApplicationContext(), 0);
		t.createTripInDatabase(c);
        t.initializeData();
		return t;
	}

	public static TripData fetchTrip(Context c, long tripid) {
		TripData t = new TripData(c.getApplicationContext(), tripid);
		t.populateDetails();
		return t;
	}

	public TripData (Context ctx, long tripid) {
		Context context = ctx.getApplicationContext();
		this.tripid = tripid;
		mDb = new DbAdapter(context);
	}

	void initializeData() {
		startTime = System.currentTimeMillis();
		endTime = System.currentTimeMillis();
        numpoints = 0;
        latestLatitude = 200;
        latestLongitude = 200;
        distance = 0;

		purp = fancystart = info = "";

		updateTrip();
	}

    // Get lat/long extremes, etc, from trip record
	void populateDetails() {

	    mDb.openReadOnly();

	    Cursor tripdetails = mDb.fetchTrip(tripid);
	    startTime = tripdetails.getDouble(tripdetails.getColumnIndex("start"));
	    status =  tripdetails.getInt(tripdetails.getColumnIndex("status"));
	    endTime = tripdetails.getDouble(tripdetails.getColumnIndex("endtime"));
        distance = tripdetails.getFloat(tripdetails.getColumnIndex("distance"));

        purp = tripdetails.getString(tripdetails.getColumnIndex("purp"));
        fancystart = tripdetails.getString(tripdetails.getColumnIndex("fancystart"));
        info = tripdetails.getString(tripdetails.getColumnIndex("fancyinfo"));

	    tripdetails.close();

		Cursor points = mDb.fetchAllCoordsForTrip(tripid);
		if (points != null) {
	        numpoints = points.getCount();
	        points.close();
		}

	    mDb.close();
	}

	void createTripInDatabase(Context c) {
		mDb.open();
		tripid = mDb.createTrip();
		mDb.close();
	}

	void dropTrip() {
	    mDb.open();
		mDb.deleteAllCoordsForTrip(tripid);
		mDb.deleteTrip(tripid);
		mDb.close();
	}

	ArrayList<CyclePoint> getPoints() {

		ArrayList<CyclePoint> cyclepoints = new ArrayList<CyclePoint>();

		try {
			mDb.openReadOnly();

			Cursor points = mDb.fetchAllCoordsForTrip(tripid);
            int COL_LAT = points.getColumnIndex(DbAdapter.K_POINT_LAT);
            int COL_LGT = points.getColumnIndex(DbAdapter.K_POINT_LGT);
            int COL_TIME = points.getColumnIndex(DbAdapter.K_POINT_TIME);
            int COL_ACC  = points.getColumnIndex(DbAdapter.K_POINT_ACC);
            int COL_SPEED  = points.getColumnIndex(DbAdapter.K_POINT_SPEED);

			while (!points.isAfterLast()) {

                double latitude = points.getDouble(COL_LAT);
                double longitude = points.getDouble(COL_LGT);
                double time = points.getDouble(COL_TIME);
                float accuracy = points.getFloat(COL_ACC);
                double altitude = points.getDouble(COL_ACC);
                float speed = points.getFloat(COL_SPEED);

                cyclepoints.add(new CyclePoint(latitude, longitude, time, accuracy, altitude, speed));
				points.moveToNext();
			}
			points.close();
			mDb.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

		return cyclepoints;
	}

	boolean addPointNow(Location loc, double currentTime, float dst) {
		double latitude = loc.getLatitude();
		double longitude = loc.getLongitude();

		// Skip duplicates
		if (latestLatitude == latitude && latestLongitude == longitude)
			return true;

		float accuracy = loc.getAccuracy();
		double altitude = loc.getAltitude();
		float speed = loc.getSpeed();

		Log.i(getClass().getName(), currentTime + ": " + latitude + ", " + longitude + " (" + accuracy + ")");

		CyclePoint pt = new CyclePoint(latitude, longitude, currentTime, accuracy, altitude, speed);

        numpoints++;
        endTime = currentTime - this.totalPauseTime;
        distance = dst;

        latestLatitude = latitude;
		latestLongitude = longitude;

        mDb.open();
        boolean rtn = mDb.addCoordToTrip(tripid, pt);
        rtn = rtn && mDb.updateTrip(tripid, "", startTime, "", "", "", distance);
        mDb.close();

        return rtn;
	}

	public boolean updateTripStatus(int tripStatus) {
		boolean rtn;
		mDb.open();
		rtn = mDb.updateTripStatus(tripid, tripStatus);
		mDb.close();
		return rtn;
	}

	public boolean getStatus(int tripStatus) {
		boolean rtn;
		mDb.open();
		rtn = mDb.updateTripStatus(tripid, tripStatus);
		mDb.close();
		return rtn;
	}

	public void updateTrip() { updateTrip("","","",""); }
	public void updateTrip(String purpose, String fancyStart, String fancyInfo, String notes) {
		// Save the trip details to the phone database. W00t!
		mDb.open();
		mDb.updateTrip(tripid, purpose,	startTime, fancyStart, fancyInfo, notes, distance);
		mDb.close();
	}
}
