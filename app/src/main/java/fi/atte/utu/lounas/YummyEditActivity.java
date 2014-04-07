package fi.atte.utu.lounas;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.view.View;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Activity for editing favorite foods using a nice UI
 */
public class YummyEditActivity extends ActionBarActivity {
	final private List<String> yummies = new ArrayList<>();
	private EditableStringArrayAdapter adapter;

	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_yummy_edit);

		getSupportActionBar().setDisplayHomeAsUpEnabled(true);

		final ListView listView = (ListView) findViewById(R.id.yummy_list);
		listView.setItemsCanFocus(true);

		adapter = new EditableStringArrayAdapter(this, R.layout.editable_string_array_row, yummies);
		listView.setAdapter(adapter);
	}

	@Override
	protected void onResume() {
		super.onResume();
		loadList();
	}

	@Override
	protected void onPause() {
		super.onPause();
		saveList();
	}

	@SuppressWarnings("UnusedParameters")
	public void addButtonClick(final View v) {
		adapter.add("");
	}

	private void loadList() {
		final SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
		final String yummyString = sharedPref.getString(SettingsActivity.YUMMY, null);

		yummies.clear();
		if (yummyString != null && yummyString.length() > 0)
			yummies.addAll(Arrays.asList(yummyString.split("\n")));
	}

	private void saveList() {
		final StringBuilder builder = new StringBuilder();
		for (final String yum : yummies) {
			if (yum.length() == 0)
				continue;

			if (builder.length() > 0)
				builder.append('\n');
			builder.append(yum);
		}

		final SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
		sharedPref.edit().putString(SettingsActivity.YUMMY, builder.toString()).commit();
	}
}
