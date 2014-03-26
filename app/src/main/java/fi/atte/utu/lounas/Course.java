package fi.atte.utu.lounas;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Represents a single course on a menu.
 */
class Course {
	private final static Pattern pricePattern = Pattern.compile("^\\d+([,.]\\d+)?$");

	private final String name;
	private final List<String> prices = new ArrayList<String>();
	//private final String flags;

	@SuppressWarnings("UnusedParameters")
	public Course(final String name, final List<String> prices, final String flags) {
		this.name = name;
		//this.flags = flags == null ? "" : flags;

		// Only take well former prices
		for (String price : prices) {
			price = price.trim();
			if (pricePattern.matcher(price).matches())
				this.prices.add(price);
		}

		// Pad prices from the left in case not all were listed
		while (this.prices.size() < 3)
			this.prices.add(0, prices.get(0));
	}

	public Course(final String name, final String[] prices, final String flags) {
		this(name, Arrays.asList(prices), flags);
	}

	public String getName() {
		return name;
	}

	public String getPrice(final int i) {
		return prices.get(i);
	}

}
