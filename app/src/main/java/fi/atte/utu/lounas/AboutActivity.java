package fi.atte.utu.lounas;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.widget.TextView;

public class AboutActivity extends ActionBarActivity {
	private static final String TAG = "Lounas/AboutActivity";

	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_about);

		getSupportActionBar().setDisplayHomeAsUpEnabled(true);

		final TextView versionView = (TextView) findViewById(R.id.about_version);

		try {
			final PackageManager packageManager = getPackageManager();
			if ( packageManager == null ){
				Log.e(TAG, "Unable to get package manager");
				return;
			}

			final PackageInfo info = packageManager.getPackageInfo(getPackageName(), 0);
			versionView.setText("v." + info.versionName);
		} catch (final NameNotFoundException e) {
			e.printStackTrace();
		}
	}
}
