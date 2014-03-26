package fi.atte.utu.lounas;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a single restaurant and its menu for a single day.
 */
public class Restaurant {
	private static final String TAG = "Lounas/Restaurant";
	private final Name name;
	private String[] times = new String[6];
	private List<Course> courses = new ArrayList<Course>();
	private boolean favorite = false;
	private boolean loaded = false;
	public Restaurant(final Name name) {
		this.name = name;
	}

	public Source getSource() {
		if (this.name == Name.ICT_TALO)
			return Source.SODEXO;
		else
			return Source.UNICA;
	}

	public Name getName() {
		return name;
	}

	public boolean isFavorite() {
		return favorite;
	}

	public void setFavorite(final boolean favorite) {
		this.favorite = favorite;
	}

	public boolean isLoaded() {
		return loaded;
	}

	public String getTime(final int pos) {
		if (pos < 0 || pos >= times.length)
			return null;
		return times[pos];
	}

	public List<Course> getCourses() {
		return this.courses;
	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	public void update(final Context context, final int dayOfWeek, final UpdateCallback callback) {
		if (dayOfWeek < 0) {
			Log.e(TAG, "Negative dayOfWeek");
			return;
		}

		final RestaurantLoaderTask task = new RestaurantLoaderTask(context, this, dayOfWeek, new RestaurantLoaderTask.Callback() {
			@Override
			public void done(final RestaurantLoaderTask.Result result) {
				if ( result == null ){
					callback.updated(false);
					return;
				}

				times = result.times;
				courses = result.courses;
				loaded = true;
				callback.updated(true);
			}
		});

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
			task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
		else
			task.execute();
	}

	public enum Name {
		// @formatter:off
		ASSARIN_ULLAKKO,
		BRYGGE,
		DELICA,
		DELI_PHARMA,
		DENTAL,
		ICT_TALO,
		MACCIAVELLI,
		MYSSY_SILINTERI,
		MIKRO,
		NUTRITIO,
		RUOKAKELLO,
		TOTTISALMI
		// @formatter:on
	}

	public enum Source {
		UNICA, SODEXO
	}

	public interface UpdateCallback {
		void updated(boolean ok);
	}
}
