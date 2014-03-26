package fi.atte.utu.lounas;

import android.annotation.TargetApi;
import android.app.ActionBar;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.util.Log;

import java.util.Arrays;

/**
 * The primary settings activity for the application.
 */
@SuppressWarnings("deprecation")
public class SettingsActivity extends PreferenceActivity implements OnSharedPreferenceChangeListener {
	public static final String FAVORITES = "pref_favorites";
	public static final String YUMMY = "pref_yummy";
	public static final String PRICE_TYPE = "pref_priceType";
	public static final String LANGUAGE = "pref_language";
	@SuppressWarnings("unused")
	private static final String TAG = "Lounas/SettingsActivity";

	private SharedPreferences getSharedPrefs() {
		final PreferenceScreen preferenceScreen = getPreferenceScreen();
		if (preferenceScreen == null) {
			Log.e(TAG, "Unable to get preference screen");
			return null;
		}

		final SharedPreferences sharedPrefs = preferenceScreen.getSharedPreferences();
		if (sharedPrefs == null) {
			Log.e(TAG, "Unable to get shared preferences");
			return null;
		}

		return sharedPrefs;
	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences);

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			final ActionBar actionBar = getActionBar();
			if (actionBar == null)
				Log.w(TAG, "Unable to get action bar");
			else
				actionBar.setDisplayHomeAsUpEnabled(true);
		}

		updateItems(getSharedPrefs());
	}

	private void updateItems(final SharedPreferences prefs) {
		if (prefs == null)
			return;

		final Preference pricePref = findPreference(PRICE_TYPE);
		if (pricePref == null)
			Log.e(TAG, "Unable to get price preference");
		else
			pricePref.setSummary(getResources().getStringArray(R.array.pref_priceType_entries)[Integer.valueOf(prefs.getString(PRICE_TYPE, "0"))]);

		final int langIndex = Arrays.asList(getResources().getStringArray(R.array.pref_language_values)).indexOf(prefs.getString(LANGUAGE, "default"));
		if (langIndex >= 0) {
			final Preference langPref = findPreference(LANGUAGE);
			if (langPref == null)
				Log.e(TAG, "Unable to get language preference");
			else
				langPref.setSummary(getResources().getStringArray(R.array.pref_language_entries)[langIndex]);
		}
	}

	@Override
	public void onSharedPreferenceChanged(final SharedPreferences prefs, final String key) {
		updateItems(prefs);
	}

	@Override
	protected void onResume() {
		super.onResume();

		final SharedPreferences sharedPrefs = getSharedPrefs();
		if (sharedPrefs != null)
			sharedPrefs.registerOnSharedPreferenceChangeListener(this);
	}

	@Override
	protected void onPause() {
		super.onPause();

		final SharedPreferences sharedPrefs = getSharedPrefs();
		if (sharedPrefs != null)
			sharedPrefs.unregisterOnSharedPreferenceChangeListener(this);
	}
}
