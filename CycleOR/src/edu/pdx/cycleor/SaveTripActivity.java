package edu.pdx.cycleor;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.TimeZone;

import edu.pdx.cycleor.IconSpinnerAdapter.IconItem;
import edu.pdx.cycleor.R;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.text.Html;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class SaveTripActivity extends Activity {
	Activity activity;
	long tripid;

	private int selected_purpose_id = 0;
	private TextView purpose_description;
	private final ArrayList<IconItem> tripPurposes = new ArrayList<IconSpinnerAdapter.IconItem>();
	private final HashMap <Integer, String> purpDescriptions = new HashMap<Integer, String>();

	private String discarded;
	private String select_purpose;
	private Button prefsButton;
	private Button btnSubmit;
	private Button btnDiscard;
	private LinearLayout tripButtons;

	// Set up the purpose buttons to be one-click only
	void preparePurposeButtons() {

		purpose_description = (TextView) findViewById(R.id.TextPurpDescription);

		tripPurposes.add(new IconSpinnerAdapter.IconItem(null, "", 0));
		tripPurposes.add(new IconSpinnerAdapter.IconItem(getResources().getDrawable(R.drawable.commute), getResources().getString(R.string.trip_purpose_commute), R.string.trip_purpose_commute));
		tripPurposes.add(new IconSpinnerAdapter.IconItem(getResources().getDrawable(R.drawable.school), getResources().getString(R.string.trip_purpose_school), R.string.trip_purpose_school));
		tripPurposes.add(new IconSpinnerAdapter.IconItem(getResources().getDrawable(R.drawable.workrel), getResources().getString(R.string.trip_purpose_work_rel), R.string.trip_purpose_work_rel));
		tripPurposes.add(new IconSpinnerAdapter.IconItem(getResources().getDrawable(R.drawable.exercise), getResources().getString(R.string.trip_purpose_exercise), R.string.trip_purpose_exercise));
		tripPurposes.add(new IconSpinnerAdapter.IconItem(getResources().getDrawable(R.drawable.social), getResources().getString(R.string.trip_purpose_social), R.string.trip_purpose_social));
		tripPurposes.add(new IconSpinnerAdapter.IconItem(getResources().getDrawable(R.drawable.shopping), getResources().getString(R.string.trip_purpose_shopping), R.string.trip_purpose_shopping));
		tripPurposes.add(new IconSpinnerAdapter.IconItem(getResources().getDrawable(R.drawable.errands), getResources().getString(R.string.trip_purpose_errand), R.string.trip_purpose_errand));
		tripPurposes.add(new IconSpinnerAdapter.IconItem(getResources().getDrawable(R.drawable.other), getResources().getString(R.string.trip_purpose_other), R.string.trip_purpose_other));

		purpDescriptions.put(0, getResources().getString(R.string.select_trip_purpose));
		purpDescriptions.put(R.string.trip_purpose_commute, getResources().getString(R.string.trip_purpose_commute_details));
		purpDescriptions.put(R.string.trip_purpose_school, getResources().getString(R.string.trip_purpose_school_details));
		purpDescriptions.put(R.string.trip_purpose_work_rel, getResources().getString(R.string.trip_purpose_work_rel_details));
		purpDescriptions.put(R.string.trip_purpose_exercise, getResources().getString(R.string.trip_purpose_exercise_details));
		purpDescriptions.put(R.string.trip_purpose_social, getResources().getString(R.string.trip_purpose_social_details));
		purpDescriptions.put(R.string.trip_purpose_shopping, getResources().getString(R.string.trip_purpose_shopping_details));
		purpDescriptions.put(R.string.trip_purpose_errand, getResources().getString(R.string.trip_purpose_errand_details));
		purpDescriptions.put(R.string.trip_purpose_other, getResources().getString(R.string.trip_purpose_other_details));

		Spinner tripPurposeSpinner = (Spinner) findViewById(R.id.tripPurposeSpinner);
		tripPurposeSpinner.setAdapter(new IconSpinnerAdapter(this, tripPurposes));
		tripPurposeSpinner.setOnItemSelectedListener(new OnItemSelectedListener(){

			@Override
			public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
				IconSpinnerAdapter.IconItem selected_purpose = tripPurposes.get(position);
				selected_purpose_id = selected_purpose.id;
				purpose_description.setText(Html.fromHtml(purpDescriptions.get(selected_purpose_id)));
			}

			@Override
			public void onNothingSelected(AdapterView<?> parentView) {
			}
		});
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.save);

		this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

		activity = this;
		finishRecording();

		// Set up trip purpose buttons
		preparePurposeButtons();

		prefsButton = (Button) findViewById(R.id.ButtonPrefs);
		tripButtons = (LinearLayout) findViewById(R.id.trip_buttons);
		btnSubmit = (Button) findViewById(R.id.ButtonSubmit);
		btnDiscard = (Button) findViewById(R.id.ButtonDiscard);

		//if the users has not yet entered their profile information, require them to do so now
		SharedPreferences settings = getSharedPreferences("PREFS", 0);
		if (settings.getAll().size() >= 1) {
			prefsButton.setVisibility(View.GONE);

			discarded = getResources().getString(R.string.discarded);
			select_purpose = getResources().getString(R.string.select_purpose);

			// Discard btn
			btnDiscard.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
					Toast.makeText(getBaseContext(), discarded, Toast.LENGTH_SHORT).show();

					cancelRecording();

					Intent i = new Intent(activity, MainActivity.class);
					i.putExtra("keepme", true);
					startActivity(i);
					finish();
				}
			});

			// Submit btn
			btnSubmit.setEnabled(false);
		} else {
			tripButtons.setVisibility(View.GONE);

			prefsButton.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
					startActivity(new Intent(activity, UserInfoActivity.class));
				}
			});
		}
	}

	// submit btn is only activated after the service.finishedRecording() is completed.
	void activateSubmitButton() {
		final Button btnSubmit = (Button) findViewById(R.id.ButtonSubmit);
		final Intent xi = new Intent(this, ShowMap.class);
		btnSubmit.setEnabled(true);

		btnSubmit.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {

				TripData trip = TripData.fetchTrip(activity, tripid);
				trip.populateDetails();

				// Make sure trip purpose has been selected
				if (selected_purpose_id == 0) {
					// Oh no!  No trip purpose!
					Toast.makeText(getBaseContext(), select_purpose, Toast.LENGTH_SHORT).show();
					return;
				}

				EditText notes = (EditText) findViewById(R.id.NotesField);

				String fancyStartTime = DateFormat.getInstance().format(trip.startTime);

				// "3.5 miles in 26 minutes"
				SimpleDateFormat sdf = new SimpleDateFormat("m");
				sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
				String minutes = sdf.format(trip.endTime - trip.startTime);
				String fancyEndInfo = String.format("%1.1f miles, %s minutes.  %s",
						(0.0006212f * trip.distance),
						minutes,
						notes.getEditableText().toString());

				// Save the trip details to the phone database. W00t!
				trip.updateTrip(
						getResources().getString(selected_purpose_id),
						fancyStartTime, fancyEndInfo,
						notes.getEditableText().toString());
				trip.updateTripStatus(TripData.STATUS_COMPLETE);
				resetService();

				// Force-drop the soft keyboard for performance
				InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
				imm.hideSoftInputFromWindow(v.getWindowToken(), 0);

				// Now create the MainInput Activity so BACK btn works properly
				Intent i = new Intent(getApplicationContext(), MainActivity.class);
				startActivity(i);

				// And, show the map!
				xi.putExtra("showtrip", trip.tripid);
				xi.putExtra("uploadTrip", true);
				startActivity(xi);
				finish();
			}
		});

	}

	void cancelRecording() {
		Intent rService = new Intent(this, RecordingService.class);
		ServiceConnection sc = new ServiceConnection() {
			public void onServiceDisconnected(ComponentName name) {}
			public void onServiceConnected(ComponentName name, IBinder service) {
				IRecordService rs = (IRecordService) service;
				rs.cancelRecording();
				unbindService(this);
			}
		};
		// This should block until the onServiceConnected (above) completes.
		bindService(rService, sc, Context.BIND_AUTO_CREATE);
	}

	void resetService() {
		Intent rService = new Intent(this, RecordingService.class);
		ServiceConnection sc = new ServiceConnection() {
			public void onServiceDisconnected(ComponentName name) {}
			public void onServiceConnected(ComponentName name, IBinder service) {
				IRecordService rs = (IRecordService) service;
				rs.reset();
				unbindService(this);
			}
		};
		// This should block until the onServiceConnected (above) completes.
		bindService(rService, sc, Context.BIND_AUTO_CREATE);
	}

	void finishRecording() {
		Intent rService = new Intent(this, RecordingService.class);
		ServiceConnection sc = new ServiceConnection() {
			public void onServiceDisconnected(ComponentName name) {}
			public void onServiceConnected(ComponentName name, IBinder service) {
				IRecordService rs = (IRecordService) service;
				tripid = rs.finishRecording();
				activateSubmitButton();
				unbindService(this);
			}
		};
		// This should block until the onServiceConnected (above) completes.
		bindService(rService, sc, Context.BIND_AUTO_CREATE);
	}
}
