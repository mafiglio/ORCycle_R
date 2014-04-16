package edu.pdx.cycleor;

import java.util.Timer;
import java.util.TimerTask;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;

import edu.pdx.cycleor.R;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;

public class RecordingService extends Service implements
	GooglePlayServicesClient.ConnectionCallbacks,
	GooglePlayServicesClient.OnConnectionFailedListener,
	LocationListener {

	private LocationClient locationClient;
	private LocationRequest locationRequest;

	RecordingActivity recordActivity;
	DbAdapter mDb;

	// Bike bell variables
	static int BELL_FIRST_INTERVAL = 20 * 60 * 1000; //20 minutes
	static int BELL_NEXT_INTERVAL = 5 * 60 * 1000; //5 minutes
    Timer timer;
	SoundPool soundpool;
	int bikebell;
    final Handler mHandler = new Handler();
    final Runnable mRemindUser = new Runnable() {
        public void run() { remindUser(); }
    };

	// Aspects of the currently recording trip
	double latestUpdate;
	Location lastLocation;
	float distanceTraveled;
	float curSpeed, maxSpeed;
	TripData trip;

	public final static int STATE_IDLE = 0;
	public final static int STATE_RECORDING = 1;
	public final static int STATE_PAUSED = 2;
	public final static int STATE_FULL = 3;

	public final static int RECORDING_SPEED = 2 * 1000; //2 second intervals

	int state = STATE_IDLE;
	private final MyServiceBinder myServiceBinder = new MyServiceBinder();

	// BEGIN SERVICE METHODS
	@Override
	public IBinder onBind(Intent arg0) {
		return myServiceBinder;
	}

	@Override
	public void onCreate() {
		super.onCreate();
	    soundpool = new SoundPool(1,AudioManager.STREAM_NOTIFICATION,0);
	    bikebell = soundpool.load(this.getBaseContext(), R.raw.bikebell,1);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
        cancelTimer();
	}

	public class MyServiceBinder extends Binder implements IRecordService {
		public int getState() {
			return state;
		}
		public void startRecording(TripData trip) {
			RecordingService.this.startRecording(trip);
		}
		public void cancelRecording() {
			RecordingService.this.cancelRecording();
		}
		public void pauseRecording() {
			RecordingService.this.pauseRecording();
		}
		public void resumeRecording() {
			RecordingService.this.resumeRecording();
		}
		public long finishRecording() {
			return RecordingService.this.finishRecording();
		}
		public long getCurrentTrip() {
			if (RecordingService.this.trip != null) {
				return RecordingService.this.trip.tripid;
			}
			return -1;
		}
		public void reset() {
			RecordingService.this.state = STATE_IDLE;
		}
		public void setListener(RecordingActivity ra) {
			RecordingService.this.recordActivity = ra;
			notifyListeners();
		}
	}
	// END SERVICE METHODS

	// BEGIN RECORDING METHODS
	public void startRecording(TripData trip) {
		this.state = STATE_RECORDING;
		this.trip = trip;

	    curSpeed = maxSpeed = distanceTraveled = 0.0f;
	    lastLocation = null;

	    // Add the notify bar and blinking light
		setNotification();

		// Start listening for GPS updates!
		locationClientInit();

		// Set up timer for bike bell
		setupTimer();
	}

	public void pauseRecording() {
		this.state = STATE_PAUSED;
		locationClientStopRecording();
	}

	public void resumeRecording() {
		this.state = STATE_RECORDING;
		locationClientStartRecording();
	}

	public long finishRecording() {
		this.state = STATE_FULL;
		locationClientStopRecording();

		clearNotifications();

		return trip.tripid;
	}

	public void cancelRecording() {
		if (trip != null) {
			trip.dropTrip();
		}

		locationClientStopRecording();

		clearNotifications();
		this.state = STATE_IDLE;
	}

	public void registerUpdates(RecordingActivity r) {
		this.recordActivity = r;
	}

	public TripData getCurrentTrip() {
		return trip;
	}
	// END RECORDING METHODS

	// BEGIN LOCATION METHODS
	private void locationClientInit() {
		locationClient = new LocationClient(this, this, this);
		locationClient.connect();
	}

	private void locationClientStartRecording() {
		if (locationClient != null) {
			locationClient.requestLocationUpdates(locationRequest, this);
		}
	}

	private void locationClientStopRecording() {
		if (locationClient != null) {
			locationClient.removeLocationUpdates(this);
		}
	}

	@Override
	public void onLocationChanged(Location loc) {
		if (loc != null) {
			// Only save one point per second.
			double currentTime = System.currentTimeMillis();
			if (currentTime - latestUpdate >= 1000) {

				latestUpdate = currentTime;
				updateTripStats(loc);
				boolean rtn = trip.addPointNow(loc, currentTime, distanceTraveled);
				if (!rtn) {
	                //Log.e("FAIL", "Couldn't write to DB");
				}
				// Update the status page every time, if we can.
				notifyListeners();
			}
		}
	}

	final float spdConvert = 2.2369f; //Meters per second to miles per hour
    private void updateTripStats(Location newLocation) {
    	//Update if accuracy is within 50 meters
    	if (newLocation.getAccuracy() > 50) return;

		//Convert speed TODO consider keeping at meters per second and only convert when displayed
		curSpeed = newLocation.getSpeed() * spdConvert;

		//Get out of the car and back on the bike
        if (curSpeed < 60) {
        	maxSpeed = Math.max(maxSpeed, curSpeed);
        }

        if (lastLocation != null) {
            float segmentDistance = lastLocation.distanceTo(newLocation);
            distanceTraveled = distanceTraveled + segmentDistance;
        }

        lastLocation = newLocation;
    }

    void notifyListeners() {
    	if (recordActivity != null) {
    		recordActivity.updateStatus(trip.numpoints, distanceTraveled, curSpeed, maxSpeed);
    	}
    }

	@Override
	public void onConnected(Bundle bundle) {
		locationRequest = new LocationRequest();
		locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY).setInterval(RECORDING_SPEED);
		locationClientStartRecording();
	}

	@Override
	public void onConnectionFailed(ConnectionResult arg0) {
	}

	@Override
	public void onDisconnected() {
	}
	// END LOCATION METHODS

	// BEGIN BELL FUNCTIONS
	public void remindUser() {
	    soundpool.play(bikebell, 1.0f, 1.0f, 1, 0, 1.0f);

		NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		int icon = R.drawable.ic_notification;
        long when = System.currentTimeMillis();
        int minutes = (int) (when - trip.startTime) / 60000;
		CharSequence tickerText = getResources().getString(R.string.still_recording, minutes);

		Notification notification = new Notification(icon, tickerText, when);
		notification.flags |=
				Notification.FLAG_ONGOING_EVENT |
				Notification.FLAG_SHOW_LIGHTS;
		notification.ledARGB = 0xffff00ff;
		notification.ledOnMS = 300;
		notification.ledOffMS = 3000;

		Context context = this;
		CharSequence contentTitle = getResources().getString(R.string.notification_title);
		CharSequence contentText = getResources().getString(R.string.notification_subtitle);
		Intent notificationIntent = new Intent(context, RecordingActivity.class);
		PendingIntent contentIntent = PendingIntent.getActivity(context, 0, notificationIntent, 0);
		notification.setLatestEventInfo(context, contentTitle, contentText,	contentIntent);
        final int RECORDING_ID = 1;
		mNotificationManager.notify(RECORDING_ID, notification);
	}

	private void setNotification() {
		NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		int icon = R.drawable.ic_notification;
		CharSequence tickerText = getResources().getString(R.string.recording);
		long when = System.currentTimeMillis();

		Notification notification = new Notification(icon, tickerText, when);

		notification.ledARGB = 0xffff00ff;
		notification.ledOnMS = 300;
		notification.ledOffMS = 3000;
		notification.flags = notification.flags |
				Notification.FLAG_ONGOING_EVENT |
				Notification.FLAG_SHOW_LIGHTS |
				Notification.FLAG_INSISTENT |
				Notification.FLAG_NO_CLEAR;

		Context context = this;
		CharSequence contentTitle = getResources().getString(R.string.notification_title);
		CharSequence contentText = getResources().getString(R.string.notification_subtitle);
		Intent notificationIntent = new Intent(context, RecordingActivity.class);
		PendingIntent contentIntent = PendingIntent.getActivity(context, 0, notificationIntent, 0);
		notification.setLatestEventInfo(context, contentTitle, contentText,	contentIntent);
		final int RECORDING_ID = 1;
		mNotificationManager.notify(RECORDING_ID, notification);
	}

	private void clearNotifications() {
		NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		mNotificationManager.cancelAll();

		cancelTimer();
	}

	private void setupTimer() {
		cancelTimer();
        timer = new Timer();
        timer.schedule (new TimerTask() {
            @Override public void run() {
                mHandler.post(mRemindUser);
            }
        }, BELL_FIRST_INTERVAL, BELL_NEXT_INTERVAL);
	}

	private void cancelTimer() {
		if (timer!=null) {
            timer.cancel();
            timer.purge();
		}
	}
	// END BELL FUNCTIONS
}