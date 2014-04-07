package fi.atte.utu.lounas;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.TypedArray;
import android.preference.ListPreference;
import android.util.AttributeSet;
import android.util.Log;

import java.util.Arrays;

/**
 * Like ListPreference, but allows multiple selections.
 */
public class MultiListPreference extends ListPreference {
	@SuppressWarnings("unused")
	private static final String TAG = "Lounas/MultiListPreference";

	private final boolean[] checked;

	public MultiListPreference(final Context context, final AttributeSet attrs) {
		super(context, attrs);

		if (getEntries() == null) {
			Log.e(TAG, "No entries defined for MultiListPreference");
			checked = null;
		} else if (getEntryValues() == null) {
			Log.e(TAG, "No entry values defined for MultiListPreference");
			checked = null;
		} else
			checked = new boolean[getEntries().length];
	}

	public MultiListPreference(final Context context) {
		this(context, null);
	}

	@SuppressWarnings("NullableProblems")
	@Override
	protected void onPrepareDialogBuilder(final AlertDialog.Builder builder) {
		updateChecked();

		builder.setMultiChoiceItems(getEntries(), checked, new DialogInterface.OnMultiChoiceClickListener() {
			public void onClick(final DialogInterface dialog, final int pos, final boolean val) {
				checked[pos] = val;
			}
		});
	}

	@Override
	protected void onDialogClosed(final boolean positiveResult) {
		if (!positiveResult)
			return;

		final CharSequence[] values = getEntryValues();
		if (values == null)
			return;

		final StringBuilder builder = new StringBuilder();

		for (int i = 0; i < checked.length; ++i) {
			if (!checked[i])
				continue;

			if (builder.length() > 0)
				builder.append("|");

			builder.append(values[i]);
		}

		final String value = builder.toString();
		if (callChangeListener(value))
			setValue(value);
	}

	@SuppressWarnings("NullableProblems")
	@Override
	protected Object onGetDefaultValue(final TypedArray typedArray, final int index) {
		final CharSequence[] val = typedArray.getTextArray(index);
		if (val == null)
			return new CharSequence[0];
		else
			return val;
	}

	@Override
	protected void onSetInitialValue(final boolean restoreValue, final Object defaultValue) {
		final CharSequence[] defaultValues;
		if (defaultValue == null)
			defaultValues = new CharSequence[0];
		else
			defaultValues = (CharSequence[]) defaultValue;

		final StringBuilder builder = new StringBuilder();
		for (final CharSequence val : defaultValues) {
			if (builder.length() > 0)
				builder.append("|");

			builder.append(val);
		}

		final String value;
		if (restoreValue)
			value = getPersistedString(builder.toString());
		else
			value = builder.toString();

		if (callChangeListener(value))
			setValue(value);
	}

	private void updateChecked() {
		final String val = getValue();
		if (val == null)
			return;

		final CharSequence[] values = getEntryValues();
		if (values == null)
			return;

		final String[] selected = val.split("\\|");

		for (int i = 0; i < values.length; ++i)
			checked[i] = Arrays.binarySearch(selected, values[i].toString()) >= 0;
	}
}
