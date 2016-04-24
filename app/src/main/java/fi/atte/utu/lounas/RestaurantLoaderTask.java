package fi.atte.utu.lounas;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.http.AndroidHttpClient;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.text.Html;
import android.util.Log;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * An asynchronous task for fetching and parsing the menu. Each task is capable
 * of fetching the menu for a single restaurant for a single day and parsing it
 * into a list of Course instances.
 */
public class RestaurantLoaderTask extends AsyncTask<Void, Void, RestaurantLoaderTask.Result> {
	@SuppressWarnings("unused")
	private static final String TAG = "Lounas/RestaurantLoaderTask";

	private static final int MAX_CACHE_ITEMS = 25;

	private static final AndroidHttpClient httpClient = AndroidHttpClient.newInstance("Lounaat (Android app)");
	private static final Pattern unicaPricePattern = Pattern.compile("\\d+(?:,\\d+)?");

	private static final Lock cacheLock = new ReentrantLock();

	private static final Map<String, Integer> dayNames = new HashMap<String, Integer>();
	private static final Pattern timePattern = Pattern.compile("(\\d{1,2})(?:[.:](\\d{2}))?\\s*-\\s*(\\d{1,2})(?:[.:](\\d{2}))?");
	private static final Map<String, Document> jsoupCache = new HashMap<String, Document>();
	private static final Map<String, JSONObject> jsonCache = new HashMap<String, JSONObject>();
	private final Context context;
	private final Restaurant restaurant;
	private final int dayOfWeek;
	private final List<Callback> callbacks;

	private RestaurantLoaderTask(final Context context, final Restaurant restaurant, final int dayOfWeek, final List<Callback> callbacks) {
		this.context = context;
		this.restaurant = restaurant;
		this.dayOfWeek = dayOfWeek;
		this.callbacks = callbacks;
	}

	public RestaurantLoaderTask(final Context context, final Restaurant restaurant, final int dayOfWeek, final Callback callback) {
		this(context, restaurant, dayOfWeek, Arrays.asList(callback));
	}

	private static String urlToFilename(final URL url) throws NoSuchAlgorithmException {
		final MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
		sha1.reset();
		sha1.update(url.toExternalForm().getBytes());

		final StringBuilder buffer = new StringBuilder();

		// Make sure cache isn't used on the wrong day
		final Calendar calendar = Calendar.getInstance();
		buffer.append(calendar.get(Calendar.YEAR));
		buffer.append('_');
		buffer.append(calendar.get(Calendar.DAY_OF_YEAR));
		buffer.append('_');

		final byte[] bytes = sha1.digest();
		for (final byte b : bytes) buffer.append(Integer.toHexString(0xFF & b));

		return buffer.toString();
	}

	private static void addDayNames(final int num, final String... names) {
		for (final String name : names)
			dayNames.put(name, num);
	}

	static {
		addDayNames(0, "ma", "må", "mo");
		addDayNames(1, "ti", "tu", "tu");
		addDayNames(2, "ke", "on", "we");
		addDayNames(3, "to", "to", "th");
		addDayNames(4, "pe", "fr", "fr");
		addDayNames(5, "la", "lö", "sa");
	}

	// Sodexo sometimes HTML encodes its strings multiple times
	private static String repetitiveHtmlDecode(String source) {
		String previousSource = null;
		for (int i = 0; i < 10 && !source.equals(previousSource); ++i) {
			previousSource = source;
			source = Html.fromHtml(source).toString();
		}
		return source;
	}

	private String getCache(final URL url) {
		cacheLock.lock();
		try {
			final File cacheDir = context.getCacheDir();
			if (cacheDir == null)
				return null;

			final File file = new File(cacheDir, urlToFilename(url));
			final Scanner scanner = new Scanner(file, "UTF-8");
			try {
				return scanner.useDelimiter("\\A").next();
			} finally {
				scanner.close();
			}
		} catch (final NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (final FileNotFoundException e) {
			// URL not in cache
		} finally {
			cacheLock.unlock();
		}

		return null;
	}

	private void setCache(final URL url, final String source) {
		cacheLock.lock();
		try {
			final File cacheDir = context.getCacheDir();
			if (cacheDir == null)
				return;

			final FileOutputStream stream = new FileOutputStream(new File(cacheDir, urlToFilename(url)));
			try {
				stream.write(source.getBytes());
			} finally {
				stream.close();
			}

			// Make sure cache size stays low
			final File[] files = cacheDir.listFiles();
			if (files != null && files.length > MAX_CACHE_ITEMS) {
				Arrays.sort(files, new Comparator<File>() {
					@Override
					public int compare(final File a, final File b) {
						return a.getName().compareTo(b.getName());
					}
				});

				for (int i = 0; i < files.length - MAX_CACHE_ITEMS; ++i) {
					if (!files[i].delete())
						Log.d(TAG, "Unable to delete: " + files[i].getName());
				}
			}
		} catch (final NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (final FileNotFoundException e) {
			// URL not in cache
		} catch (final IOException e) {
			e.printStackTrace();
		} finally {
			cacheLock.unlock();
		}
	}

	@Override
	protected Result doInBackground(final Void... voids) {
		final SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
		String lang = sharedPref.getString(SettingsActivity.LANGUAGE, "default");

		if ("default".equals(lang))
			lang = Locale.getDefault().getLanguage();

		if ("sv".equals(lang))
			lang = "se";
		else if (!"fi".equals(lang))
			lang = "en";

		Result out = getForLang(lang);
		if ("fi".equals(lang) || (out != null && out.courses.size() > 0))
			return out;

		return getForLang("fi");
	}

	private String getSource(final String urlString) {
		final URL url;
		try {
			url = new URL(urlString);
		} catch (final MalformedURLException e) {
			e.printStackTrace();
			return null;
		}

		String source = getCache(url);
		if (source == null || source.length() == 0) {
			try {
				final HttpResponse response = httpClient.execute(new HttpGet(url.toURI()));
				final Scanner scanner = new Scanner(response.getEntity().getContent(), "UTF-8");
				try {
					source = scanner.useDelimiter("\\A").next();
					setCache(url, source);
				} finally {
					scanner.close();
				}
			} catch (final IOException e) {
				e.printStackTrace();
			} catch (final URISyntaxException e) {
				e.printStackTrace();
			} catch (final Exception e) {
				e.printStackTrace();
				return "";
			}
		}

		return source;
	}

	private Result getForLang(final String lang) {
		// Build URL
		String urlString;
		if (restaurant.getSource() == Restaurant.Source.UNICA) {

			urlString = "http://www.unica.fi/";
			if ("fi".equals(lang))
				urlString += "fi/ravintolat/";
			else if ("se".equals(lang))
				urlString += "se/restauranger/";
			else
				urlString += "en/restaurants/";
			urlString += restaurant.getName().name().toLowerCase(Locale.US).replace('_', '-');
		} else {
			final Calendar calendar = Calendar.getInstance();
			calendar.set(Calendar.DAY_OF_WEEK, dayOfWeek);

			urlString = "http://www.sodexo.fi/ruokalistat/output/daily_json/54/";
			urlString += new SimpleDateFormat("yyyy/M/d", Locale.US).format(calendar.getTime());
			urlString += "/" + lang;
		}

		if (restaurant.getSource() == Restaurant.Source.UNICA) {
			Document doc = null;
			synchronized (jsoupCache) {
				if (jsoupCache.containsKey(urlString))
					doc = jsoupCache.get(urlString);
			}

			if (doc == null) {
				doc = Jsoup.parse(getSource(urlString));
				synchronized (jsoupCache) {
					jsoupCache.put(urlString, doc);
				}
			}

			return parseUnica(doc);
		} else {
			JSONObject json = null;
			synchronized (jsonCache) {
				if (jsonCache.containsKey(urlString))
					json = jsonCache.get(urlString);
			}

			if (json == null) {
				try {
					json = new JSONObject(repetitiveHtmlDecode(getSource(urlString)));
					synchronized (jsonCache) {
						jsonCache.put(urlString, json);
					}
				} catch (final JSONException e) {
					e.printStackTrace();
				}
			}

			return parseSodexo(json, lang);
		}
	}

	private int parseDayName(final String name) {
		try {
			final Integer out = dayNames.get(name.trim().substring(0, 2).toLowerCase(Locale.getDefault()));
			if (out == null)
				return -1;
			else
				return out;
		} catch (final IndexOutOfBoundsException e) {
			e.printStackTrace();
			return -1;
		}
	}

	private void parseTimes(final String[] out, final String dayString, final String timeString) {
		final String[] days = dayString.trim().split("\\s*\\-\\s*");
		if (days == null || days.length == 0 || days.length > 2 || timeString.trim().length() < 3)
			return;

		final int day1 = parseDayName(days[0]);

		int day2 = days.length == 1 ? day1 : parseDayName(days[1]);
		if (day2 < 0)
			day2 = day1;

		if (day1 < 0 || day2 < 0 || day2 < day1)
			return;

		// Fix time formatting if possible
		final StringBuilder builder = new StringBuilder();
		final Matcher matcher = timePattern.matcher(timeString);
		if (matcher.find()) {
			builder.append(matcher.group(1));
			builder.append(':');
			if (matcher.group(2) == null)
				builder.append("00");
			else
				builder.append(matcher.group(2));

			builder.append(" - ");

			builder.append(matcher.group(3));
			builder.append(':');
			if (matcher.group(4) == null)
				builder.append("00");
			else
				builder.append(matcher.group(4));
		} else
			builder.append(timeString.trim());

		final String fixedTime = builder.toString();
		for (int i = day1; i <= day2; ++i)
			out[i] = fixedTime;
	}

	private Result parseSodexo(final JSONObject json, final String lang) {
		final Result result = new Result();

		try {
			final JSONArray jsonCourses = json.getJSONArray("courses");
			if (jsonCourses.length() < 1)
				return result;

			for (int i = 0; i < jsonCourses.length(); ++i) {
				final JSONObject obj = jsonCourses.getJSONObject(i);
				result.courses.add(new Course(obj.getString("title_" + lang), obj.getString("price").split("\\s+/\\s+"), obj.optString("properties")));
			}

			final String pageURL = json.getJSONObject("meta").getString("ref_url");
			final String pageSource = pageURL == null ? null : getSource(pageURL);
			if (pageSource != null) {
				final Document doc = Jsoup.parse(pageSource);
				try {
					final Element el = doc.select(".block-facility-opening-hours .content > .part").last();
					final Elements rows = el.getElementsByClass("clearfix");
					for (final Element row : rows) {
						final Element days = row.getElementsByClass("days").first();
						final Element times = row.getElementsByClass("times").first();
						parseTimes(result.times, days.text(), times.text());
					}
				} catch (final RuntimeException e) {
					e.printStackTrace();
				}
			}
		} catch (final JSONException e) {
			e.printStackTrace();
		}

		return result;
	}

	private Result parseUnica(final Document doc) {
		final Result result = new Result();

		final Elements dayDivs = doc.select(".menu-list > .accord");
		for (final Element dayDiv : dayDivs) {
			final int divDayOfWeek = Integer.valueOf(dayDiv.select("> h4[data-dayofweek]").first().attr("data-dayofweek"));

			if (divDayOfWeek != dayOfWeek - 2)
				continue;

			final Elements courseRows = dayDiv.getElementsByTag("tr");
			for (final Element courseRow : courseRows) {
				try {
					final Element nameEl = courseRow.select("td.lunch").first();
					if (nameEl == null)
						continue;

					final String priceString = courseRow.select("td.price").first().text();
					final List<String> prices = new ArrayList<String>();

					final Matcher matcher = unicaPricePattern.matcher(priceString);
					for (int i = 0; i < 3; ++i) {
						if (matcher.find())
							prices.add(matcher.group());
					}

					final StringBuilder flags = new StringBuilder();
					final Elements lims = courseRow.select("td.limitations span");
					for (final Element lim : lims) {
						if (flags.length() > 0)
							flags.append(" ");
						flags.append(lim.text());
					}

					result.courses.add(new Course(nameEl.text().replaceAll("\\s+\\*\\s*", ""), prices, flags.toString()));
				} catch (final RuntimeException e) {
					e.printStackTrace();
				}
			}
		}

		try {
			final Element header = doc.select("#content > .head + div").first();
			final Element par = header.getElementsByClass("threecol").last().getElementsByTag("p").first();
			final List<TextNode> rows = par.textNodes();

			for (final TextNode row : rows) {
				final String[] parts = row.text().trim().split("\\s+", 2);
				if (parts.length == 2)
					parseTimes(result.times, parts[0], parts[1]);
			}
		} catch (final RuntimeException e) {
			e.printStackTrace();
		}

		return result;
	}

	@Override
	protected void onPostExecute(final Result result) {
		for (final Callback callback : this.callbacks)
			callback.done(result);
	}

	public static interface Callback {
		void done(Result result);
	}

	public static class Result {
		public final String[] times = new String[6];
		public final List<Course> courses = new ArrayList<Course>();
	}
}
