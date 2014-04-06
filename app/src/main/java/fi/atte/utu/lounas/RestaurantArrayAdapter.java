package fi.atte.utu.lounas;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Array adapter for showing Restaurant instances in a ListView.
 */
class RestaurantArrayAdapter extends ArrayAdapter<Restaurant> {
	private static final String TAG = "Lounas/RestaurantArrayAdapter";
	private final int resource;
	private final int dayOfWeek;

	@SuppressWarnings("SameParameterValue")
	public RestaurantArrayAdapter(final Context context, final int resource, final List<Restaurant> objects, final int dayOfWeek) {
		super(context, resource, objects);
		this.resource = resource;
		this.dayOfWeek = dayOfWeek;
	}

	private static String getDisplayNameByName(final Context context, final Restaurant.Name name) {
		final List<String> values = Arrays.asList(context.getResources().getStringArray(R.array.restaurant_values));
		final String[] names = context.getResources().getStringArray(R.array.restaurant_names);

		final int pos = values.indexOf(name.name());
		if (pos < 0 || pos >= names.length) {
			Log.e(TAG, "Name not found: " + name.name());
			return "Name not found!";
		}

		return names[pos];
	}

	public static String getDisplayName(final Context context, final Restaurant restaurant) {
		return getDisplayNameByName(context, restaurant.getName());
	}

	@Override
	public final View getView(final int position, View convertView, final ViewGroup parent) {
		final Restaurant restaurant = getItem(position);

		// Don't show restaurants with no courses for the day
		if (restaurant.isLoaded() && restaurant.getCourses().size() == 0)
			return new View(getContext());

		final LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);

		// View holder pattern
		ViewHolder holder = null;
		if (convertView != null)
			holder = (ViewHolder) convertView.getTag();

		if (holder == null) {
			convertView = inflater.inflate(resource, parent, false);
			if (convertView == null) {
				Log.e(TAG, "Unable to inflate restaurant array row layout");
				return null;
			}

			holder = new ViewHolder();
			holder.title = (TextView) convertView.findViewById(R.id.restaurant_row_title);
			holder.time = (TextView) convertView.findViewById(R.id.restaurant_row_time);
			holder.courses = (LinearLayout) convertView.findViewById(R.id.restaurant_row_course_list);

			holder.loading = new TextView(getContext());
			holder.loading.setText(getContext().getString(R.string.loading));

			convertView.setTag(holder);
		}

		// Clear out old data
		holder.title.setText(getDisplayName(getContext(), restaurant));
		holder.time.setVisibility(View.INVISIBLE);
		holder.courses.removeAllViews();

		// Abort early for incomplete items
		if (restaurant.getCourses().size() == 0) {
			holder.courses.addView(holder.loading);
			return convertView;
		}

		// Draw time if available
		final String timeString = restaurant.getTime(dayOfWeek - 2);
		if (timeString != null && timeString.length() > 0) {
			holder.time.setVisibility(View.VISIBLE);
			holder.time.setText(timeString);
		}

		// Prepare for drawing course list
		final SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getContext());
		final int priceType = Integer.valueOf(sharedPref.getString(SettingsActivity.PRICE_TYPE, "0"));
		final int highlightColor = getContext().getResources().getColor(R.color.yummy);

		// Load favorite foods
		final String[] yummies;
		final String yummyString = sharedPref.getString(SettingsActivity.YUMMY, null);
		if (yummyString == null || yummyString.length() == 0)
			yummies = new String[0];
		else
			yummies = yummyString.split("\n");

		// Lowercase all favorite foods
		for (int i = 0; i < yummies.length; ++i)
			yummies[i] = yummies[i].toLowerCase(Locale.US);

		// Draw course list
		for (final Course course : restaurant.getCourses()) {
			final View courseRow = inflater.inflate(R.layout.course_row, holder.courses, false);
			if (courseRow == null) {
				Log.e(TAG, "Unable to inflate course row layout");
				break;
			}

			final TextView courseTitle = (TextView) courseRow.findViewById(R.id.course_row_title);
			courseTitle.setText(course.getName());

			// Highlight favorite foods
			final String lowerName = course.getName().toLowerCase(Locale.US);
			for (final String needle : yummies) {
				if (needle.length() > 0 && lowerName.contains(needle)) {
					courseTitle.setTextColor(highlightColor);
					break;
				}
			}

			final TextView coursePrice = (TextView) courseRow.findViewById(R.id.course_row_price);
			try {
				coursePrice.setText(course.getPrice(priceType));
			} catch (final IndexOutOfBoundsException e) {
				Log.e(TAG, "No price for course");
				e.printStackTrace();
			}

			holder.courses.addView(courseRow);
		}

		return convertView;
	}

	@Override
	public boolean isEnabled(final int position) {
		return false;
	}

	@Override
	public boolean hasStableIds() {
		return true;
	}

	@Override
	public long getItemId(final int position) {
		return getItem(position).getName().ordinal();
	}

	private static class ViewHolder {
		public TextView title;
		public TextView time;
		public LinearLayout courses;
		public TextView loading;
	}
}
