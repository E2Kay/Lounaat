package fi.atte.utu.lounas;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.view.ViewGroup;

/**
 * Adapter for showing RestaurantListFragments in a ViewPager.
 */
public class RestaurantPagerAdapter extends FragmentStatePagerAdapter {
	@SuppressWarnings("unused")
	private static final String TAG = "Lounas/RestaurantPagerAdapter";

	private final String[] weekDayNames;
	private int previousPosition = -1;

	public RestaurantPagerAdapter(final FragmentManager fm, final String[] weekDayNames) {
		super(fm);
		this.weekDayNames = weekDayNames;
	}

	@Override
	public Fragment getItem(final int pos) {
		return RestaurantListFragment.newInstance(pos + 2);
	}

	@Override
	public int getCount() {
		return 6;
	}

	@Override
	public CharSequence getPageTitle(final int pos) {
		return weekDayNames[pos + 1];
	}

	@Override
	public void setPrimaryItem(final ViewGroup container, final int position, final Object object) {
		super.setPrimaryItem(container, position, object);

		if (position == previousPosition)
			return;
		previousPosition = position;

		final RestaurantListFragment frag = (RestaurantListFragment) object;
		if (!frag.hasLoaded)
			frag.updateRestaurants();
	}
}
