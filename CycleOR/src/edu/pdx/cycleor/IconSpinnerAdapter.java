package edu.pdx.cycleor;

import java.util.ArrayList;

import edu.pdx.cycleor.R;

import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;


public class IconSpinnerAdapter extends ArrayAdapter<IconSpinnerAdapter.IconItem> {
    private final Activity activity;
	private final ArrayList<IconSpinnerAdapter.IconItem> data;

	public IconSpinnerAdapter(Activity activity, ArrayList<IconSpinnerAdapter.IconItem> data) {
        super(activity, 0, data);
        this.activity = activity;
        this.data = data;
    }

    @Override
    public View getDropDownView(int position, View convertView,ViewGroup parent) {
        return getCustomView(R.layout.icon_spinner_dropdown_item, position, convertView, parent);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        return getCustomView(R.layout.icon_spinner_item, position, convertView, parent);
    }

    public View getCustomView(int resource, int position, View convertView, ViewGroup parent) {
		View row = activity.getLayoutInflater().inflate(resource, parent, false);
		IconSpinnerAdapter.IconItem item = data.get(position);

		TextView label = (TextView) row.findViewById(android.R.id.text1);
		label.setText(item.label);

		ImageView icon = (ImageView) row.findViewById(android.R.id.icon1);
		icon.setImageDrawable(item.icon);

		return row;
    }

    static class IconItem {
		public Drawable icon;
		public String label;
		public int id;

		public IconItem(Drawable icon, String label) {
			this(icon, label, 0);
		}
		public IconItem(Drawable icon, String label, int id) {
			this.icon = icon;
			this.label = label;
			this.id = id;
		}
	}
}