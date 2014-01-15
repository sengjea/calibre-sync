package net.sengjea.calibre;

import android.app.Activity;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.view.*;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.TextView;

import java.io.File;

public class CollectionFragment extends Fragment {

	public interface CollectionFragmentListener {
		public void onBookLongClick(Bundle bundle);

        // public void onBookClick(String s);
    }
    public static String TAB_TAG = "tab_collection";
	private static ListView lvBooklist;
	private Cursor booklistCursor;
	private static MetadataDatabaseHelper mDB;
	private boolean isShowingTags = false;
	private CollectionFragmentListener mListener;
	
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        // Verify that the host activity implements the callback interface
        try {
            mListener = (CollectionFragmentListener) activity;
        } catch (ClassCastException e) {
            // The activity doesn't implement the interface, throw exception
            throw new ClassCastException(activity.toString()
                    + " must implement CollectionFragmentListener");
        }
    }
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
        mDB = new MetadataDatabaseHelper(getActivity());
		
	}
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.activity_collection, container, false);
		lvBooklist = (ListView) v.findViewById(R.id.col_list);
		booklistCursor = mDB.getNotNull(Book.COLUMNS, "lpath");

		return v;
	}
	@Override
	public void onDestroy() {
		super.onDestroy();
		mDB.close();
	}
	
	@Override
	public void onStart() {
		super.onStart();
        showBooks();
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
			inflater.inflate(R.menu.collection_fragment, menu);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            SearchManager searchManager = (SearchManager) this.getActivity().getSystemService(Context.SEARCH_SERVICE);
            SearchView searchView = (SearchView) menu.findItem(R.id.menu_search).getActionView();
            searchView.setSearchableInfo(searchManager.getSearchableInfo(this.getActivity().getComponentName()));
        }
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_collection_tags:
			isShowingTags = true;
			showTags();
			break;
		case R.id.menu_collection_books:
            isShowingTags = false;
            booklistCursor = mDB.getNotNull(Book.COLUMNS, "lpath");
            showBooks();

            break;
            case R.id.menu_search:
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
                this.getActivity().onSearchRequested();
                }
                break;
        }
        return super.onOptionsItemSelected(item);
	}
	
	private void showBooks() {
		BookCursorAdapter bca = new BookCursorAdapter(getActivity(), booklistCursor,false);
		lvBooklist.setAdapter(bca);
		lvBooklist.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int pos,
                                    long id) {

                TextView t_uuid = (TextView) view.findViewById(R.id.bi_uuid);
                startActivity(new Intent(getActivity(),OpenBookActivity.class)
                                .putExtra(OpenBookActivity.EXTRA_UUID, t_uuid.getText())
                );

            }
        });
		lvBooklist.setOnItemLongClickListener(new OnItemLongClickListener() {

            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view,
                                           int pos, long id) {
                Bundle bundle = new Bundle();
                bundle.putInt("id", (int) id);
                mListener.onBookLongClick(bundle);
                return true;
            }
        });
        refreshBookList();
	}
	private void showTags() {
		Cursor cs = mDB.getTagsWithBooks();
		TagCursorAdapter tca = new TagCursorAdapter(getActivity(),cs,false);
		lvBooklist.setAdapter(tca);
		lvBooklist.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int pos,
                                    long id) {
                isShowingTags = false;
                booklistCursor = mDB.getBooksWithTag(Book.COLUMNS, (int) id);
                showBooks();
            }

        });
	}
	public void deleteBook(int id) {

		try {
			Cursor cs = mDB.getById(new String[] {"lpath"}, id);
			if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED) && cs.moveToFirst()) {
                SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext());
                File root_dir = new File(Environment.getExternalStorageDirectory(),settings.getString("pref_root_dir", "eBooks/"));
				File fd = new File(root_dir,cs.getString(0));
				fd.delete();
				mDB.deleteBook(id);
				refreshBookList();
			}
		} catch (Throwable e) {
			e.printStackTrace();

		}
	}

	protected void refreshBookList() {
		if (isShowingTags) return;
        try {
		    ((BookCursorAdapter) lvBooklist.getAdapter()).getCursor().requery();
		    ((BookCursorAdapter) lvBooklist.getAdapter()).notifyDataSetChanged();
        } catch (Exception e) {
            e.printStackTrace();
        }
	}
	
}
