package fi.atte.utu.lounas;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Shows all the restaurants and their menus for a single day.
 */
public class RestaurantListFragment extends ListFragment {
	private static final String TAG = "Lounas/RestaurantListFragment";
	private static final String ARG_DAY_OF_WEEK = "day_of_week";
	private final List<Restaurant> restaurants = new ArrayList<Restaurant>();
	public boolean hasLoaded = false;
	private RestaurantArrayAdapter adapter;

	public static RestaurantListFragment newInstance(final int dayOfWeek) {
		final Bundle args = new Bundle();
		args.putInt(ARG_DAY_OF_WEEK, dayOfWeek);

		final RestaurantListFragment fragment = new RestaurantListFragment();
		fragment.setArguments(args);

		return fragment;
	}

	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		restaurants.clear();
		for (final Restaurant.Name name : Restaurant.Name.values())
			restaurants.add(new Restaurant(name));

		adapter = new RestaurantArrayAdapter(getActivity(), R.layout.restaurant_row, restaurants, getArguments().getInt(ARG_DAY_OF_WEEK, -1));
		setListAdapter(adapter);
	}

	@Override
	public void onResume() {
		super.onResume();

		sortRestaurants();
	}

	@Override
	public void onActivityCreated(final Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		final ListView view = getListView();
		view.setVerticalScrollBarEnabled(false);
		view.setDividerHeight(0);
		view.setDivider(null);
	}

	public void updateRestaurants() {
		hasLoaded = true;

		sortRestaurants();
		for (final Restaurant restaurant : restaurants) {
			restaurant.update(getActivity(), getArguments().getInt(ARG_DAY_OF_WEEK, 0), new Restaurant.UpdateCallback() {
				@Override
				public void updated(final boolean ok) {
					if (!ok)
						Log.e(TAG, "Error updating " + RestaurantArrayAdapter.getDisplayName(getActivity(), restaurant));

					adapter.notifyDataSetChanged();
				}
			});
		}
	}

	void sortRestaurants() {
		// Update favorite statuses
		final SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getActivity());
		final String[] favorites = sharedPref.getString(SettingsActivity.FAVORITES, "").split("\\|");
		for (final Restaurant restaurant : restaurants)
			restaurant.setFavorite(Arrays.binarySearch(favorites, restaurant.getName().name()) >= 0);

		// Sort favorites first, then alphabetically
		Collections.sort(restaurants, new Comparator<Restaurant>() {
			@Override
			public int compare(final Restaurant a, final Restaurant b) {
				if (a.isFavorite() && !b.isFavorite())
					return -1;

				if (b.isFavorite() && !a.isFavorite())
					return 1;

				return RestaurantArrayAdapter.getDisplayName(getActivity(), a).compareToIgnoreCase(RestaurantArrayAdapter.getDisplayName(getActivity(), b));
			}
		});

		adapter.notifyDataSetChanged();
	}
}
