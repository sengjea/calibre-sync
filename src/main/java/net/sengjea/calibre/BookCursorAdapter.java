/**
 * 
 */
package net.sengjea.calibre;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * @author sengjea
 *
 */
public class BookCursorAdapter extends CursorAdapter {
	
	
	public BookCursorAdapter(Context context, Cursor cursor, boolean autoRequery) {
		super(context, cursor, autoRequery);

		// TODO Auto-generated constructor stub
	}

	@Override
	public void bindView(View view, Context context, Cursor cs) {
		try {
			Bitmap pic = null;
			ImageView thumb = (ImageView) view.findViewById(R.id.bi_thumbnail);	
			try {
				pic = BitmapFactory.decodeByteArray(cs.getBlob(Book.THUMB), 0, cs.getBlob(Book.THUMB).length);
				if (pic == null) throw new Exception();
				thumb.setImageBitmap(pic);
			} catch (Exception e) {
				thumb.setImageResource(R.drawable.dumbbook);
			}
			TextView title = (TextView) view.findViewById(R.id.bi_title);
			TextView author = (TextView) view.findViewById(R.id.bi_author);
			TextView uuid = (TextView) view.findViewById(R.id.bi_uuid);
			title.setText(cs.getString(Book.TITLE));
			author.setText(cs.getString(Book.AUTHOR));
			uuid.setText(cs.getString(Book.UUID));

		} catch (Exception e) {
			Logger.e(e);
		}
	}

	@Override
	public View newView(Context context, Cursor cs, ViewGroup parent) {
		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		return inflater.inflate(R.layout.book_item, null);
	}
	@Override
	protected void onContentChanged() {
		Logger.d("BookCursorAdapter.onContentChanged()");
	}

}
