package net.sengjea.calibre;

import android.app.Activity;
import android.app.SearchManager;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

/**
 * User: sengjea
 * Date: 25/12/13
 * Time: 21:40
 */
public class BookSearchActivity extends Activity {
    private static MetadataDatabaseHelper mDB;
    private String query;
    private BookCursorAdapter mBCA;
    private static ListView lvBookList;
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.activity_collection);
        // Get the intent, verify the action and get the query
        Intent intent = getIntent();
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            query = intent.getStringExtra(SearchManager.QUERY);
        }


    }

    @Override
    protected void onStart() {
        super.onStart();
        mDB = new MetadataDatabaseHelper(getApplicationContext());
        mBCA = new BookCursorAdapter(this, mDB.search(Book.COLUMNS, query), false);
        lvBookList = (ListView) findViewById(R.id.col_list);
        lvBookList.setAdapter(mBCA);
        lvBookList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                TextView t_uuid = (TextView) view.findViewById(R.id.bi_uuid);
                startActivity(new Intent(getApplicationContext(), OpenBookActivity.class)
                        .putExtra(OpenBookActivity.EXTRA_UUID, t_uuid.getText())
                );
                finish();
            }
        });

    }

    @Override
    protected void onStop() {
        super.onStop(); 
        mDB.close();
    }


}