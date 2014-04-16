package edu.pdx.cycleor;

import java.util.Map;
import java.util.Map.Entry;

import edu.pdx.cycleor.R;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Toast;

public class UserInfoActivity extends Activity {
	public final static int PREF_AGE = 1;
	public final static int PREF_ZIPHOME = 2;
	public final static int PREF_ZIPWORK = 3;
	public final static int PREF_ZIPSCHOOL = 4;
	public final static int PREF_EMAIL = 5;
	public final static int PREF_GENDER = 6;
	public final static int PREF_CYCLEFREQ = 7;
	public final static int PREF_ETHNICITY = 8;
	public final static int PREF_INCOME = 9;
	public final static int PREF_RIDERTYPE = 10;
	public final static int PREF_RIDERHISTORY = 11;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.userprefs);

		// Don't pop up the soft keyboard until taps on something
		this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

		SharedPreferences settings = getSharedPreferences("PREFS", 0);
		Map<String, ?> prefs = settings.getAll();
		for (Entry<String, ?> p : prefs.entrySet()) {
			int key = Integer.parseInt(p.getKey());

			switch (key) {
				case PREF_AGE:
					((Spinner) findViewById(R.id.ageSpinner)).setSelection(((Integer) p.getValue()).intValue());
					break;
				case PREF_ETHNICITY:
					((Spinner) findViewById(R.id.ethnicitySpinner)).setSelection(((Integer) p.getValue()).intValue());
					break;
				case PREF_INCOME:
					((Spinner) findViewById(R.id.incomeSpinner)).setSelection(((Integer) p.getValue()).intValue());
					break;
				case PREF_RIDERTYPE:
					((Spinner) findViewById(R.id.ridertypeSpinner)).setSelection(((Integer) p.getValue()).intValue());
					break;
				case PREF_RIDERHISTORY:
					((Spinner) findViewById(R.id.riderhistorySpinner)).setSelection(((Integer) p.getValue()).intValue());
					break;
				case PREF_CYCLEFREQ:
					((Spinner) findViewById(R.id.frequencySpinner)).setSelection(((Integer) p.getValue()).intValue());
					break;
				case PREF_ZIPHOME:
					((EditText) findViewById(R.id.TextZipHome)).setText((CharSequence) p.getValue());
					break;
				case PREF_ZIPWORK:
					((EditText) findViewById(R.id.TextZipWork)).setText((CharSequence) p.getValue());
					break;
				case PREF_ZIPSCHOOL:
					((EditText) findViewById(R.id.TextZipSchool)).setText((CharSequence) p.getValue());
					break;
				case PREF_EMAIL:
					((EditText) findViewById(R.id.TextEmail)).setText((CharSequence) p.getValue());
					break;
				case PREF_GENDER:
					int x = ((Integer) p.getValue()).intValue();
					if (x == 2) {
						((RadioButton) findViewById(R.id.ButtonMale)).setChecked(true);
					} else if (x == 1) {
						((RadioButton) findViewById(R.id.ButtonFemale)).setChecked(true);
					}
					break;
			}
		}
	}

	@Override
	public void onDestroy() {
		savePreferences();
		super.onDestroy();
	}

	private void savePreferences() {
		SharedPreferences settings = getSharedPreferences("PREFS", 0);
		SharedPreferences.Editor editor = settings.edit();

		editor.putInt(String.valueOf(PREF_AGE),
				((Spinner) findViewById(R.id.ageSpinner)).getSelectedItemPosition());
		editor.putInt(String.valueOf(PREF_ETHNICITY),
				((Spinner) findViewById(R.id.ethnicitySpinner)).getSelectedItemPosition());
		editor.putInt(String.valueOf(PREF_INCOME),
				((Spinner) findViewById(R.id.incomeSpinner)).getSelectedItemPosition());
		editor.putInt(String.valueOf(PREF_RIDERTYPE),
				((Spinner) findViewById(R.id.ridertypeSpinner)).getSelectedItemPosition());
		editor.putInt(String.valueOf(PREF_RIDERHISTORY),
				((Spinner) findViewById(R.id.riderhistorySpinner)).getSelectedItemPosition());
		editor.putInt(String.valueOf(PREF_CYCLEFREQ),
				((Spinner) findViewById(R.id.frequencySpinner)).getSelectedItemPosition());
		editor.putString(String.valueOf(PREF_ZIPHOME),
				((EditText) findViewById(R.id.TextZipHome)).getText().toString());
		editor.putString(String.valueOf(PREF_ZIPWORK),
				((EditText) findViewById(R.id.TextZipWork)).getText().toString());
		editor.putString(String.valueOf(PREF_ZIPSCHOOL),
				((EditText) findViewById(R.id.TextZipSchool)).getText().toString());
		editor.putString(String.valueOf(PREF_EMAIL),
				((EditText) findViewById(R.id.TextEmail)).getText().toString());
		editor.putInt(String.valueOf(PREF_GENDER),
				((RadioGroup) findViewById(R.id.genderGroup)).getCheckedRadioButtonId() == R.id.ButtonMale ? 1 : 2);
		editor.commit();

		Toast.makeText(getBaseContext(), getResources().getString(R.string.preferences_saved), Toast.LENGTH_SHORT).show();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.user_info, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.menu_save:
				savePreferences();
				this.finish();
				return true;
		}
		return false;
	}
}
