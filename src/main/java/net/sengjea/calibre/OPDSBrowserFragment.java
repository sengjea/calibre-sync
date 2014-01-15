package net.sengjea.calibre;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.view.*;
import android.view.View.OnClickListener;
import android.widget.*;
import android.widget.AdapterView.OnItemClickListener;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Stack;

@SuppressLint("NewApi")
public class OPDSBrowserFragment extends Fragment {

    public interface OPDSFragmentListener {
        // public void onBookClick(String uuid, String url);
        public void noServerInPreferences();
    }
    private OPDSFragmentListener mListener;
    private static ListView lvList;
    private static ProgressBar pbProgress;
    private static TextView tvMore;
    private URL urlCSRoot;
    private OPDSPage currentPage;

    public static String TAB_TAG = "tab_opds";
    private Stack<URL> url_stack = new Stack<URL>();

    private final OPDSLoader mOPDS = new OPDSLoader() {
        public void onParseComplete(OPDSPage p) {
            pbProgress.setVisibility(View.GONE);
            //tvDump.setVisibility(View.GONE);
            lvList.setEnabled(true);
            if (p != null && currentPage != p) {
                currentPage = p;
                loadCurrentPage();
            } else {
                updateCurrentPage();
            }
        }

        @Override
        public void onStartParse() {
            pbProgress.setVisibility(View.VISIBLE);
            try {
                ((BaseAdapter) ((HeaderViewListAdapter) lvList.getAdapter()).getWrappedAdapter())
                        .notifyDataSetInvalidated();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        public void onParseProgress(int pcnt) {

        }
    };
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        // Verify that the host activity implements the callback interface
        try {
            mListener = (OPDSFragmentListener) activity;
        } catch (ClassCastException e) {
            // The activity doesn't implement the interface, throw exception
            throw new ClassCastException(activity.toString()
                    + " must implement OPDSFragmentListener");
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mOPDS.setContext(getActivity());
        setHasOptionsMenu(true);

        //Settings related initialisation
        SharedPreferences mSettings = PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext());
        try {
            urlCSRoot = new URL(mSettings.getString("pref_cs_url",""));
        } catch (MalformedURLException e) {
            urlCSRoot = null;
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.activity_opds,container,false);
        pbProgress = (ProgressBar) v.findViewById(R.id.col_prog);
        //tvDump = (TextView) v.findViewById(R.id.col_dump);
        //tvDump.setVisibility(TextView.VISIBLE);

        View vmore = inflater.inflate(R.layout.listfooter_more, null);
        vmore.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View arg0) {
                URL urlMore = null;
                try {
                    urlMore = new URL(urlCSRoot.getProtocol(),
                            urlCSRoot.getHost(),
                            urlCSRoot.getPort(),
                            currentPage.getNavigationalLinks().get("next"));
                    loadURL(urlMore, true);
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                }

            }
        });
        tvMore = (TextView) vmore.findViewById(R.id.listfooter_more);

        lvList = (ListView) v.findViewById(R.id.col_list);
        lvList.addFooterView(vmore);
        lvList.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position,
                                    long id) {

                try {

                    if (((HeaderViewListAdapter)parent.getAdapter())
                            .getWrappedAdapter().getClass() == BookCursorAdapter.class) {
                        Cursor cs = (Cursor) parent.getItemAtPosition(position);
                        URL tmp_url = new URL(urlCSRoot.getProtocol(),
                                urlCSRoot.getHost(),
                                urlCSRoot.getPort(),
                                cs.getString(Book.LPATH));
                        startActivity(new Intent(getActivity(), OpenBookActivity.class)
                                .putExtra(OpenBookActivity.EXTRA_UUID, cs.getString(Book.UUID))
                                .putExtra(OpenBookActivity.EXTRA_URL, tmp_url.toString())
                        );
                        //mListener.onBookClick(cs.getString(Book.UUID), tmp_url.toString());
                    } else if (((HeaderViewListAdapter)parent.getAdapter())
                            .getWrappedAdapter().getClass() == ArrayAdapter.class) {
                        String key = (String) parent.getItemAtPosition(position);
                        URL tmp_url = new URL(urlCSRoot.getProtocol(),
                                urlCSRoot.getHost(),
                                urlCSRoot.getPort(),
                                currentPage.getCatalogLinkFor(key));
                        loadURL(tmp_url,false);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

        });
        return v;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.opds_fragment, menu);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            SearchManager searchManager = (SearchManager) this.getActivity().getSystemService(Context.SEARCH_SERVICE);
            SearchView searchView = (SearchView) menu.findItem(R.id.menu_search).getActionView();
            searchView.setSearchableInfo(searchManager.getSearchableInfo(this.getActivity().getComponentName()));
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem mitem) {
        switch (mitem.getItemId()) {
            case android.R.id.home:
           //case R.id.menu_goback:
                loadPreviousPage();
                break;
            case R.id.menu_search:
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
                    this.getActivity().onSearchRequested();
                }
                break;
        }

        return super.onOptionsItemSelected(mitem);
    }
    public boolean loadPreviousPage() {
        if ((currentPage = mOPDS.getPreviousPage()) != null) {
            loadCurrentPage();
            return true;
        }
        return false;
    }
    @Override
    public void onStart() {
        super.onStart();
        if (mOPDS.isParsingXml()) {
            pbProgress.setVisibility(View.VISIBLE);

        } else if (currentPage == null) {
            if (urlCSRoot != null) {
                loadURL(urlCSRoot,false);
            } else {
                mListener.noServerInPreferences();
            }
        } else {
            loadCurrentPage();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
            getActivity().getActionBar().setDisplayHomeAsUpEnabled(false);
    }

    public void loadCurrentPage() {
        if (currentPage == null || lvList == null) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
            getActivity().getActionBar().setDisplayHomeAsUpEnabled((mOPDS != null && mOPDS.pageHasParent()));
        if (currentPage.hasCatalogLinks()) {
            ArrayAdapter<String> aa = new ArrayAdapter<String>(getActivity(),android.R.layout.simple_list_item_1);
            for (String s : currentPage.getCatalogLinks().keySet()) {
                aa.add(s);
            }
            lvList.setAdapter(aa);
        } else {
            lvList.setAdapter(new BookCursorAdapter(getActivity(),currentPage.getBooks(), false));
        }
        updateCurrentPage();
    }
    public void updateCurrentPage() {
        if (currentPage == null || tvMore == null || lvList == null) return;
        if (currentPage.getNavigationalLinks().containsKey("next")) {
            tvMore.setVisibility(TextView.VISIBLE);
            tvMore.setText(getString(R.string.opds_more));

        } else {
            tvMore.setVisibility(TextView.GONE);
        }
        try {
            ((BaseAdapter) ((HeaderViewListAdapter) lvList.getAdapter()).getWrappedAdapter()).notifyDataSetChanged();
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
    }

    public void loadURL(URL url, boolean more) {
        if (mOPDS.beginParsingXml(url, more)) {
            pbProgress.setVisibility(View.VISIBLE);
            //tvDump.setVisibility(View.VISIBLE);
            //tvDump.setText(String.format(getString(R.string.opds_loading), url.toString()));
        }
    }

}