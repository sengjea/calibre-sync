package net.sengjea.calibre;

import android.app.Activity;
import android.content.*;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.text.method.ScrollingMovementMethod;
import android.view.*;
import android.widget.*;
import android.widget.AdapterView.OnItemClickListener;

public class ConnectionFragment extends Fragment {




    public interface ConnectionFragmentListener {
		public void onManualConnectionClick();
		public void onServerListSelect(Bundle bundle);

	}
    public static String TAB_TAG ="tab_connection";
	private static TextView tvDump, tvConnectionTitle;
	private static ImageView ivIcon;
	private static ProgressBar pbProgress;
	private static ListView lvServers;
	private CalibreService mCalibreService;
	private boolean isBoundToService = false;
	private Intent service_intent;
	private ConnectionFragmentListener mListener;

	private static ArrayAdapter<ConnectionInfo> connectionInfoArrayAdapter;

	private ServiceConnection mServiceConnection = new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName arg0, IBinder binder) {
			Logger.d("MainActivity: Bound to Calibre Service");
			mCalibreService = ((CalibreService.CalibreBinder) binder).getService();
            mCalibreService.enquireConnectionState();
            connectionInfoArrayAdapter = new ArrayAdapter<ConnectionInfo>(getActivity(),
                    android.R.layout.simple_list_item_1,android.R.id.text1, mCalibreService.getListOfServers());
			lvServers.setAdapter(connectionInfoArrayAdapter);
		}

		@Override
		public void onServiceDisconnected(ComponentName arg0) {
			Logger.d("MainActivity: unBound from Calibre Service");
			mCalibreService = null;
		}

	};

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		// Verify that the host activity implements the callback interface
		try {
			mListener = (ConnectionFragmentListener) activity;
		} catch (ClassCastException e) {
			// The activity doesn't implement the interface, throw exception
			throw new ClassCastException(activity.toString()
					+ " must implement ConnectionFragmentListener");
		}
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setHasOptionsMenu(true);

		//Service related set up
		service_intent = new Intent(getActivity(),CalibreService.class);
	}

	@Override
	public View onCreateView (LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.activity_connection, container,false);

		//Instantiate Connection UI Components
		tvDump = (TextView) v.findViewById(R.id.dump);
		tvDump.setMovementMethod(new ScrollingMovementMethod());
		tvConnectionTitle = (TextView) v.findViewById(R.id.contitle);
		pbProgress = (ProgressBar) v.findViewById(R.id.progress);
		ivIcon = (ImageView) v.findViewById(R.id.conicon);
		lvServers = (ListView) v.findViewById(R.id.serverlist);
		lvServers.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int pos,
                                    long id) {
                ConnectionInfo selected_cinfo = connectionInfoArrayAdapter.getItem(pos);
                //if (mCalibreService != null && mCalibreService.attemptAutoconnect(selected_cinfo) == false) {
                    Bundle args = new Bundle();
                    args.putString("host", selected_cinfo.getHost());
                    args.putString("wd_port", selected_cinfo.getWirelessDevicePort());
                    args.putString("cs_port", selected_cinfo.getContentServerPort());
                    mListener.onServerListSelect(args);
                //}
            }
        });

		//Set Connection UI Components to default state
		ivIcon.setImageResource(R.drawable.csync2);
		tvConnectionTitle.setText(R.string.connection_disconnected);
		pbProgress.setVisibility(View.INVISIBLE);

		return v;
	}

    public void setConnectionState(CalibreService.ConnectionState connectionState) {
        if (ivIcon == null || tvConnectionTitle == null
                || mCalibreService == null || pbProgress == null) return;
        ConnectionInfo ci = mCalibreService.getCurrentServer();
        switch (connectionState) {
            case CONNECTING:
                ivIcon.setImageResource(R.drawable.csync2_connected);
                pbProgress.setVisibility(View.VISIBLE);
            tvConnectionTitle.setText(String.format(getString(R.string.connection_connecting), ci.toString()));
                break;
            case COMMUNICATING:
                ivIcon.setImageResource(R.drawable.csync2_connected);
                pbProgress.setVisibility(View.VISIBLE);
                tvConnectionTitle.setText(String.format(getString(R.string.connection_communicating), ci.toString()));
                break;
            case IDLING:
                ivIcon.setImageResource(R.drawable.csync2_connected);
                pbProgress.setVisibility(View.INVISIBLE);
                tvConnectionTitle.setText(String.format(getString(R.string.connection_connected), ci.toString()));

                break;
            case DISCONNECTED:
                ivIcon.setImageResource(R.drawable.csync2);
                pbProgress.setVisibility(View.INVISIBLE);
                tvConnectionTitle.setText(R.string.connection_disconnected);
                break;
            case WAITING_FOR_FOLDERSCAN:
                appendDebugText(R.string.log_wait_for_drive);
                break;

        }
    }
    public void setErrorMessage(CalibreService.ErrorType e) {
        switch (e) {
            case CALIBRE_BUSY:
                appendDebugText(R.string.connection_calibre_busy);
                break;
            case CANT_CONNECT:
                appendDebugText(R.string.log_unable_to_connect);
                break;
            case CANT_DOWNLOAD_FILE:
                appendDebugText(R.string.log_error_dlfile);
                break;
            case WRONG_PASSWORD:
                appendDebugText(R.string.log_err_wrong_password);
                break;
            case ROOT_NO_ACCESS:
                appendDebugText(R.string.log_error_root_folder_noread);
                break;
        }
    }
    public void setDiscoveryState(CalibreService.DiscoveryState discoveryState) {
        switch (discoveryState) {
            case BEGIN_DISCOVERY:
                appendDebugText(R.string.log_disc_working);
                if (pbProgress != null) pbProgress.setVisibility(View.VISIBLE);

                break;
            case DONE_DISCOVERY:
                if (pbProgress != null) pbProgress.setVisibility(View.INVISIBLE);
                break;
            case NEW_SERVER:
                if (connectionInfoArrayAdapter != null)
                    connectionInfoArrayAdapter.notifyDataSetChanged();
                else if (lvServers != null && mCalibreService != null) {
                    connectionInfoArrayAdapter = new ArrayAdapter<ConnectionInfo>(getActivity(),
                            android.R.layout.simple_list_item_1,android.R.id.text1, mCalibreService.getListOfServers());
                    lvServers.setAdapter(connectionInfoArrayAdapter);
                }

                break;
            case NO_SERVER:

                break;

        }
    }
    public void appendDebugText(int i) {
        appendDebugText(getString(i));
    }

    public void appendDebugText(String s) {
        if (tvDump != null) tvDump.append(s+"\n");
    }

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		inflater.inflate(R.menu.connection_fragment, menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem mitem) {
		switch (mitem.getItemId()) {
		case R.id.menu_settings:
			startActivity(new Intent(getActivity(), SettingsActivity.class));
			break;
		case R.id.menu_disconnect:
			if (mCalibreService == null) break;
			mCalibreService.disconnectFromServer();
			break;
		case R.id.menu_rediscover:
			if (mCalibreService == null) break;
            mCalibreService.discoverServers();
			break;
		case R.id.menu_manual_connection:
			mListener.onManualConnectionClick();
			break;
		}	
		return super.onOptionsItemSelected(mitem);
	}

	public void onStart() {
		super.onStart();
		this.getActivity().getApplicationContext().bindService(service_intent, mServiceConnection, Context.BIND_AUTO_CREATE);
		isBoundToService = true;

	}

	public void onStop() {
		super.onStop();
		if (isBoundToService && mCalibreService != null) {
			this.getActivity().getApplicationContext().unbindService(mServiceConnection);
			isBoundToService = false;
		}
	}
}
