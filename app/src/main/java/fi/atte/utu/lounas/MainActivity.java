package fi.atte.utu.lounas;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Debug;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import java.util.Arrays;
import java.util.Calendar;

/**
 * The main activity of the application
 */
public class MainActivity extends ActionBarActivity {
	@SuppressWarnings("unused")
	private static final String TAG = "Lounas/MainActivity";

	private static final int CODE_SETTINGS = 1;

	private ViewPager viewPager;

	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		final ListView tabList = (ListView) findViewById(R.id.main_tabs);

		final String[] weekDayNames = getResources().getStringArray(R.array.days_of_week);
		final RestaurantPagerAdapter pagerAdapter = new RestaurantPagerAdapter(getSupportFragmentManager(), weekDayNames);

		viewPager = (ViewPager) findViewById(R.id.main_pager);
		viewPager.setOffscreenPageLimit(pagerAdapter.getCount());
		viewPager.setAdapter(pagerAdapter);
		viewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
			@Override
			public void onPageSelected(final int page) {
				getSupportActionBar().setTitle(weekDayNames[page + 1]);
				if (tabList != null)
					tabList.setItemChecked(page, true);
			}
		});

		final Calendar calendar = Calendar.getInstance();
		final int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) - 2;

		// Show current day (Saturday on Sunday to avoid confusion)
		if (dayOfWeek >= 0)
			viewPager.setCurrentItem(dayOfWeek);
		else
			viewPager.setCurrentItem(5);

		// On tablets, set up tabs on left side
		if (tabList != null) {
			final ArrayAdapter<String> tabAdapter = new ArrayAdapter<String>(this, R.layout.main_tab, Arrays.copyOfRange(weekDayNames, 1, weekDayNames.length));
			tabList.setAdapter(tabAdapter);
			tabList.setItemChecked(viewPager.getCurrentItem(), true);
			tabList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
				@Override
				public void onItemClick(final AdapterView<?> parent, final View view, final int position, final long id) {
					viewPager.setCurrentItem(position);
				}
			});
		}
	}

	@Override
	protected void onResume() {
		super.onResume();

		// Make sure an internet connection exists
		final ConnectivityManager conMan = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		final NetworkInfo netInfo = conMan.getActiveNetworkInfo();
		if (netInfo == null || !netInfo.isConnected()) {
			Toast.makeText(this, getString(R.string.network_needed), Toast.LENGTH_LONG).show();
			finish();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(final Menu menu) {
		final MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main_activity_actions, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(final MenuItem item) {
		switch (item.getItemId()) {
			case R.id.main_action_preferences:
				final Intent intent = new Intent(this, SettingsActivity.class);
				startActivityForResult(intent, CODE_SETTINGS);
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	@Override
	protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
		if (requestCode == CODE_SETTINGS) {
			if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
				final Intent intent = new Intent(this, MainActivity.class);
				finish();
				startActivity(intent);
			} else
				recreate();
		}
	}
}
