package edu.pdx.cycleor;

import java.util.List;
import java.util.Map;

import edu.pdx.cycleor.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.SQLException;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
    private final static int CONTEXT_RETRY = 0;
    private final static int CONTEXT_DELETE = 1;
	private TextView counter;
	private ListView listSavedTrips;
	private Activity activity;
	private Button startButton;

    //DbAdapter mDb;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		activity = this;

		counter = (TextView) findViewById(R.id.TextViewPreviousTrips);
		listSavedTrips = (ListView) findViewById(R.id.ListSavedTrips);

		// Let's handle some launcher lifecycle issues:

		// If we're recording or saving right now, jump to the existing activity.
		// (This handles user who hit BACK button while recording)

		Intent rService = new Intent(this, RecordingService.class);
		ServiceConnection sc = new ServiceConnection() {
			public void onServiceDisconnected(ComponentName name) {}
			public void onServiceConnected(ComponentName name, IBinder service) {
				IRecordService rs = (IRecordService) service;
				int state = rs.getState();
				if (state > RecordingService.STATE_IDLE) {
					if (state == RecordingService.STATE_FULL) {
						startActivity(new Intent(activity, SaveTripActivity.class));
					} else {  // RECORDING OR PAUSED:
						startActivity(new Intent(activity, RecordingActivity.class));
					}
					finish();
				} else {
					// Idle. First run? Switch to user prefs screen if there are no prefs stored yet
			        SharedPreferences settings = getSharedPreferences("PREFS", 0);
			        if (settings.getAll().isEmpty()) {
                        showWelcomeDialog();
			        }
					// Not first run - set up the list view of saved trips
					populateList();
				}
				unbindService(this); // race?  this says we no longer care
			}
		};

		// This needs to block until the onServiceConnected (above) completes.
		// Thus, we can check the recording status before continuing on.
		bindService(rService, sc, Context.BIND_AUTO_CREATE);

		// And set up the record button
		startButton = (Button) findViewById(R.id.ButtonStart);
		startButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				// TODO update to google play services location api
			    // Before we go to record, check GPS status
			    final LocationManager manager = (LocationManager) getSystemService( Context.LOCATION_SERVICE );
			    if ( !manager.isProviderEnabled( LocationManager.GPS_PROVIDER ) ) {
			        buildAlertMessageNoGps();
			    } else {
	                startActivity(new Intent(activity, RecordingActivity.class));
	                finish();
			    }
			}
		});
	}

    private void buildAlertMessageNoGps() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(getResources().getString(R.string.gps_disabled))
               .setCancelable(false)
               .setPositiveButton(getResources().getString(R.string.gps_settings), new DialogInterface.OnClickListener() {
                   public void onClick(final DialogInterface dialog, final int id) {
                       final ComponentName toLaunch = new ComponentName("com.android.settings","com.android.settings.SecuritySettings");
                       final Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                       intent.addCategory(Intent.CATEGORY_LAUNCHER);
                       intent.setComponent(toLaunch);
                       intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                       startActivityForResult(intent, 0);
                   }
               })
               .setNegativeButton(getResources().getString(R.string.cancel), new DialogInterface.OnClickListener() {
                   public void onClick(final DialogInterface dialog, final int id) {
                        dialog.cancel();
                   }
               });
        final AlertDialog alert = builder.create();
        alert.show();
    }

    private void showWelcomeDialog() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(getResources().getString(R.string.welcome_message))
               .setCancelable(false).setTitle(getResources().getString(R.string.welcome_title))
               .setPositiveButton(getResources().getString(R.string.okay), new DialogInterface.OnClickListener() {
                   public void onClick(final DialogInterface dialog, final int id) {
                       startActivity(new Intent(activity, UserInfoActivity.class));
                   }
               });

        final AlertDialog alert = builder.create();
        alert.show();
    }

	void populateList() {
		// Get list from the real phone database. W00t!
		DbAdapter mDb = new DbAdapter(activity);
		mDb.open();

		// Clean up any bad trips & coords from crashes
		int cleanedTrips = mDb.cleanTables();
		if (cleanedTrips > 0) {
		    Toast.makeText(getBaseContext(),cleanedTrips + getResources().getString(R.string.trips_removed), Toast.LENGTH_SHORT).show();
		}

		try {
			Cursor allTrips = mDb.fetchAllTrips();

			@SuppressWarnings("deprecation")
			SimpleCursorAdapter sca = new SimpleCursorAdapter(this,
				R.layout.twolinelist, allTrips,
				new String[] { "purp", "fancystart", "fancyinfo"},
				new int[] {R.id.text1, R.id.text2, R.id.text3}
			);

			listSavedTrips.setAdapter(sca);

			int numtrips = allTrips.getCount();
			switch (numtrips) {
			case 0:
				counter.setText(getResources().getString(R.string.saved_trips_0));
				break;
			case 1:
				counter.setText(getResources().getString(R.string.saved_trips_1));
				break;
			default:
				counter.setText(numtrips + getResources().getString(R.string.saved_trips_X));
			}
			// allTrips.close();
		} catch (SQLException sqle) {
			// Do nothing, for now!
		}
		mDb.close();

		listSavedTrips.setOnItemClickListener(new AdapterView.OnItemClickListener() {
		    public void onItemClick(AdapterView<?> parent, View v, int pos, long id) {
		        Intent i = new Intent(activity, ShowMap.class);
		        i.putExtra("showtrip", id);
		        startActivity(i);
		    }
		});
		registerForContextMenu(listSavedTrips);
	}

	@Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
	    menu.add(0, CONTEXT_RETRY, 0, getResources().getString(R.string.retry_upload));
	    menu.add(0, CONTEXT_DELETE, 0,  getResources().getString(R.string.delete));
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
	    AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
	    switch (item.getItemId()) {
	    case CONTEXT_RETRY:
	        retryTripUpload(info.id);
	        return true;
	    case CONTEXT_DELETE:
	        deleteTrip(info.id);
	        return true;
	    default:
	        return super.onContextItemSelected(item);
	    }
	}

	private void retryTripUpload(long tripId) {
	    TripUploader uploader = new TripUploader(activity);
        uploader.execute(tripId);
	}

	private void deleteTrip(long tripId) {
	    DbAdapter mDbHelper = new DbAdapter(activity);
        mDbHelper.open();
        mDbHelper.deleteAllCoordsForTrip(tripId);
        mDbHelper.deleteTrip(tripId);
        mDbHelper.close();
        listSavedTrips.invalidate();
        populateList();
    }

	/* Creates the menu items */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    /* Handles item selections */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.menu_help:
        	openHelp();
            return true;
        case R.id.menu_user_info:
        	openUserInfo();
            return true;
        }
    	return false;
    }

    private void openHelp() {
    	startActivity(new Intent(
   			Intent.ACTION_VIEW,
   			Uri.parse(getResources().getString(R.string.help_url))
   		));
    }

    private void openUserInfo() {
    	startActivity(new Intent(this, UserInfoActivity.class));
    }
}

class FakeAdapter extends SimpleAdapter {
	public FakeAdapter(Context context, List<? extends Map<String, ?>> data,
			int resource, String[] from, int[] to) {
		super(context, data, resource, from, to);
	}
}