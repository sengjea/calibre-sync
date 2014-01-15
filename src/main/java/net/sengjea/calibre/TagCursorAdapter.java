package net.sengjea.calibre;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class TagCursorAdapter extends CursorAdapter {

	public TagCursorAdapter(Context context, Cursor c, boolean autoRequery) {
		super(context, c, autoRequery);
		// TODO Auto-generated constructor stub
	}

	@Override
	public void bindView(View view, Context context, Cursor cursor) {
		TextView tagtext = (TextView) view.findViewById(android.R.id.text1);
		tagtext.setText(cursor.getString(1));
	}

	@Override
	public View newView(Context context, Cursor cursor, ViewGroup parent) {
		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		return inflater.inflate(android.R.layout.simple_list_item_1, null);
	}

}
