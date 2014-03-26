package fi.atte.utu.lounas;

import android.app.Application;
import android.preference.PreferenceManager;

/**
 * The main application class.
 */
public class Lounas extends Application {
	@SuppressWarnings("unused")
	private static final String TAG = "Lounas/Lounas";

	@Override
	public void onCreate() {
		super.onCreate();
		PreferenceManager.setDefaultValues(this, R.xml.preferences, true);
	}
}
