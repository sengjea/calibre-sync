package net.sengjea.calibre;

import android.app.Activity;
import android.content.*;
import android.lib.BarTab;
import android.lib.BarTabActivity;
import android.lib.BarTabManager;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentManager;

public class MainActivity extends BarTabActivity
        implements ConnectServerDialogFragment.ConnectServerDialogListener,
        NoServerDialogFragment.NoServerDialogListener,
        ConnectionFragment.ConnectionFragmentListener,
        CollectionFragment.CollectionFragmentListener,
        BookInfoDialogFragment.BookInfoDialogListener,
        OPDSBrowserFragment.OPDSFragmentListener,
        CalibreService.CalibreListener {
    public static final String EXTRA_TAB = "switch_tab";
    private FragmentManager mFragmentManager;
    private BarTabManager mBarTabManager;
    private static SharedPreferences mSettings;
    private static MetadataDatabaseHelper mDB;
    private BarTab tab_connection,tab_collection,tab_opds;
    private CalibreService mCalibreService;
    private boolean isBoundToService;
    private final Activity mActivity = this;
    private Intent calibreServiceIntent;
    private ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mCalibreService = ((CalibreService.CalibreBinder) service).getService();
            mCalibreService.setListener((CalibreService.CalibreListener) mActivity);
            isBoundToService = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isBoundToService = false;
            mCalibreService = null;
        }
    };

	/* Test Flow:
	 * 1. Condition: Options Erased, No Existing Folder
	 * 	  Open App
	 *    Connect to Server
	 *    Download books using Calibre
	 *    Download books using Content Server
	 *    Open Calibre-loaded book
	 *    Open Content server loaded book
	 *    Delete both books
	 *    Use Calibre to download more books
	 * 2. Condition: Existing Options, Existing Folder
	 * 	  Open App
	 *    Connect to Server
	 *    Change Root Folder to Non-Existing Folder
	 *    Open books from Root Folder
	 *    Delete books from Root Folder
	 *    Download books using Calibre 
	 *  
	 * 3. Condition: Existing Options, Existing Folder
	 * 	  Open App
	 * 	  Connect to Server
	 *    Change Root Folder to Existing Empty Folder
	 *    Download books using Calibre
	 *    Open books from Root Folder
	 * 4. Condition: Existing Options, No Existing Folder (Delete Root folder after initial setup)
	 * 	  Open App
	 *    Connect to Server
	 *    Download books using Calibre
	 *    Open books from Root Folder
	 * 5. Condition: Existing Options, Existing Folder, Close Calibre Server
	 * 	  Open App
	 */

    @Override
    public void onBackPressed() {
        OPDSBrowserFragment frag = (OPDSBrowserFragment) tab_opds.getFragment();
        if (frag == null || !frag.isVisible() || !frag.loadPreviousPage()) {
            super.onBackPressed();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Instantiate Main  UI Components
        setContentView(R.layout.tabs_main);
        mBarTabManager = getBarTabManager();

        //Diaglog Fragment Manager and their dialogs
        mFragmentManager = getSupportFragmentManager();

        mSettings = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        tab_connection = mBarTabManager.newTab(ConnectionFragment.TAB_TAG);
        tab_connection.setText(R.string.tab_connection)
                .setIcon(R.drawable.ic_tab_connection)
                .setFragmentClass(ConnectionFragment.class);
        mBarTabManager.addTab(tab_connection);

        tab_collection = mBarTabManager.newTab(CollectionFragment.TAB_TAG);
        tab_collection.setText(R.string.tab_collection)
                .setIcon(R.drawable.ic_tab_collection)
                .setFragmentClass(CollectionFragment.class);
        mBarTabManager.addTab(tab_collection);

        tab_opds = mBarTabManager.newTab(OPDSBrowserFragment.TAB_TAG);
        tab_opds.setText(R.string.tab_library)
                .setIcon(R.drawable.ic_tab_csync)
                .setFragmentClass(OPDSBrowserFragment.class);
        mBarTabManager.addTab(tab_opds);

        //Kickstart Calibre Service
        calibreServiceIntent = new Intent(this, CalibreService.class);
        startService(calibreServiceIntent);

        mDB = new MetadataDatabaseHelper(getApplicationContext());

    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        mDB.close();

    }

    @Override
    public void onStart() {
        super.onStart();
        mActivity.getApplicationContext().bindService(calibreServiceIntent, mServiceConnection, Context.BIND_AUTO_CREATE);
        isBoundToService = true;
        if (getIntent().hasExtra(EXTRA_TAB)) {
            mBarTabManager.switchTabByTag(getIntent().getStringExtra(EXTRA_TAB));
        }

    }

    @Override
    public void onStop() {
        super.onStop();
        if (isBoundToService && mCalibreService != null) {
            mCalibreService.setListener(null);
            mActivity.getApplicationContext().unbindService(mServiceConnection);
            isBoundToService = false;
        }
    }

    @Override
    public void onConnectClick(ConnectionInfo ci) {
        OPDSBrowserFragment frag = ((OPDSBrowserFragment) tab_opds.getFragment());

        if (mCalibreService != null) {
            mCalibreService.connectToServer(ci);
        }
        if (frag != null) {
            frag.loadURL(ci.getContentServerURL(),false);
        }
    }

    @Override
    public void onRetryClick() {
        if (mCalibreService != null)
            mCalibreService.discoverServers();
    }

    @Override
    public void onServerListSelect(Bundle bundle) {
        ConnectServerDialogFragment instCSDF = new ConnectServerDialogFragment();
        instCSDF.setArguments(bundle);
        instCSDF.show(mFragmentManager, "dlg-connect");

    }

    @Override
    public void onManualConnectionClick() {
        showManualConnectionDialog();

    }

    @Override
    public void noServerInPreferences() {
        showManualConnectionDialog();
    }

    private void showManualConnectionDialog() {
        ConnectServerDialogFragment instCSDF = new ConnectServerDialogFragment();
        instCSDF.show(mFragmentManager, "dlg-connect");
    }

    @Override
    public void onBookLongClick(Bundle bundle) {
        BookInfoDialogFragment instBIDF = new BookInfoDialogFragment();
        instBIDF.setArguments(bundle);
        instBIDF.show(mFragmentManager, "dlg-book-info");

    }

    @Override
    public void confirmDelete(int id) {
        CollectionFragment frag = (CollectionFragment) tab_collection.getFragment();
        if (frag != null)
            frag.deleteBook(id);
    }

    public void onConnectionStateChanged(CalibreService.ConnectionState connectionState) {
        ConnectionFragment frag = (ConnectionFragment) tab_connection.getFragment();
        if (frag != null)
            frag.setConnectionState(connectionState);
    }

    public void onDiscoveryStateChanged(CalibreService.DiscoveryState discoveryState) {
        ConnectionFragment frag = (ConnectionFragment) tab_connection.getFragment();
        if (frag != null) {
            frag.setDiscoveryState(discoveryState);
            switch (discoveryState) {
                case NO_SERVER:
                    if (frag.isResumed() &&
                            mFragmentManager.findFragmentByTag("dlg-connect") == null) {
                        NoServerDialogFragment instNSDF = new NoServerDialogFragment();
                        instNSDF.show(mFragmentManager, "dlg-no-server");
                    }
                    break;
            }
        }
    }

    @Override
    public void onErrorReported(CalibreService.ErrorType e) {
        ConnectionFragment frag = (ConnectionFragment) tab_connection.getFragment();
        if (frag != null)
            frag.setErrorMessage(e);
    }

    public void onBookListChanged() {
        mBarTabManager.switchTab(tab_collection);
        CollectionFragment frag = (CollectionFragment) tab_collection.getFragment();
        if (frag != null)
            frag.refreshBookList();

    }

}
