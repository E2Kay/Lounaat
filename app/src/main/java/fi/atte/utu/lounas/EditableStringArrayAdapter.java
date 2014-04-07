package fi.atte.utu.lounas;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;

import java.util.List;

/**
 * Array adapter for an editable list of strings
 */
public class EditableStringArrayAdapter extends ArrayAdapter<String> {
	@SuppressWarnings("unused")
	private static final String TAG = "Lounas/EditableStringArrayAdapter";
	private final int resource;
	private final List<String> strings;

	@SuppressWarnings("SameParameterValue")
	public EditableStringArrayAdapter(final Context context, final int resource, final List<String> objects) {
		super(context, resource, objects);
		this.resource = resource;
		this.strings = objects;
	}

	@Override
	public final View getView(final int position, View convertView, final ViewGroup parent) {
		final String text = getItem(position);

		final ViewHolder holder;
		if (convertView == null) {
			final LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			convertView = inflater.inflate(resource, parent, false);
			if (convertView == null) {
				Log.e(TAG, "Unable to inflate editable string array view");
				return null;
			}

			holder = new ViewHolder();
			holder.position = position;
			holder.editor = (EditText) convertView.findViewById(R.id.editable_string_row_editor);
			holder.delete = (ImageButton) convertView.findViewById(R.id.editable_string_row_delete);
			convertView.setTag(holder);

			holder.editor.addTextChangedListener(new TextWatcher() {
				@Override
				public void beforeTextChanged(final CharSequence charSequence, final int i, final int i2, final int i3) {
				}

				@Override
				public void onTextChanged(final CharSequence charSequence, final int i, final int i2, final int i3) {
				}

				@Override
				public void afterTextChanged(final Editable editable) {
					strings.set(holder.position, editable.toString());
				}
			});

			holder.delete.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(final View v) {
					remove(getItem(holder.position));
				}
			});
		} else
			holder = (ViewHolder) convertView.getTag();

		holder.editor.setText(text);

		return convertView;
	}

	@Override
	public boolean isEnabled(final int position) {
		return false;
	}

	private class ViewHolder {
		public int position;
		public EditText editor;
		public ImageButton delete;
	}
}
