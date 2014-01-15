package net.sengjea.calibre;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.*;

public class ConnectServerDialogFragment extends DialogFragment {


    public interface ConnectServerDialogListener {
        public void onConnectClick(ConnectionInfo ci);
    }
    private EditText etHost, etPassword, etWirelessDevicePort, etContentServerPort;
    private CheckBox cbAutoConnect;
    private ProgressBar pbConnect;
    private Button bConnect;
    private ConnectionInfo ci;
    private boolean connectAfterResolve = false;
    private static SharedPreferences mSettings;

    public ConnectServerDialogFragment() {
        super();
        //Instantiate Preferences

    }

    @Override
    public void onResume() {
        super.onResume();
        if (getArguments() != null) {
            etHost.setText(getArguments().getString("host"));
            etWirelessDevicePort.setText(getArguments().getString("wd_port"));
            etContentServerPort.setText(getArguments().getString("cs_port"));
            etHost.setEnabled(false);
            etWirelessDevicePort.setEnabled(false);
            etContentServerPort.setEnabled(false);
            connectAfterResolve = false;
            resolveCI();
            etPassword.requestFocus();
        } else {
            etHost.requestFocus();
        }
    }

    ConnectServerDialogListener mListener;
    // Override the Fragment.onAttach() method to instantiate the NoticeDialogListener
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        // Verify that the host activity implements the callback interface
        try {
            // Instantiate the NoticeDialogListener so we can send events to the host
            mListener = (ConnectServerDialogListener) activity;
        } catch (ClassCastException e) {
            // The activity doesn't implement the interface, throw exception
            throw new ClassCastException(activity.toString()
                    + " must implement ConnectServerDialogListener");
        }
    }
    private void resolveCI() {
        new AsyncIPResolver().execute(etHost.getText().toString(),
                etWirelessDevicePort.getText().toString(),
                etContentServerPort.getText().toString());
    }
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        getDialog().setTitle(R.string.dialog_title_connection_settings);
        View v = inflater.inflate(R.layout.dialog_connect, container);
        mSettings = PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext());

        etHost = (EditText) v.findViewById(R.id.host);
        etPassword = (EditText) v.findViewById(R.id.password);
        etWirelessDevicePort = (EditText) v.findViewById(R.id.wd_port);
        etContentServerPort = (EditText) v.findViewById(R.id.cs_port);
        cbAutoConnect = (CheckBox) v.findViewById(R.id.autocon);
        pbConnect = (ProgressBar) v.findViewById(R.id.connect_dlg_progress);
        pbConnect.setVisibility(View.INVISIBLE);

        bConnect = (Button) v.findViewById(R.id.connect_server_connect);
        Button cancel_button = (Button) v.findViewById(R.id.connect_server_cancel);

        bConnect.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                if (mListener != null) {
                    if (ci != null) {
                        connectClick();
                    } else {
                        connectAfterResolve = true;
                        resolveCI();
                    }
                }

            }
        });
        cancel_button.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                getDialog().dismiss();
            }
        });
        return v;
    }
    private void connectClick() {
        ci.setPassword(etPassword.getText().toString());
        SharedPreferences.Editor mSettingsEditor = mSettings.edit();
        mSettingsEditor.putString("pref_wd_address", ci.toString());
        mSettingsEditor.putString("pref_cs_url", ci.getContentServerURL().toString());
        mSettingsEditor.putString("pref_password", ci.getPassword());
        mSettingsEditor.putBoolean("pref_autocon", cbAutoConnect.isChecked());
        mSettingsEditor.commit();
        getDialog().dismiss();
        mListener.onConnectClick(ci);
    }
    private class AsyncIPResolver extends AsyncTask<String,Integer,ConnectionInfo> {
        @Override
        protected ConnectionInfo doInBackground(String... params) {
            int wd_port, cs_port;
            if (ci == null) {
                try {
                    try {
                        wd_port = Integer.parseInt(params[1]);
                    } catch (Exception e) {
                        wd_port = 0;
                    }
                    try {
                        cs_port = Integer.parseInt(params[2]);
                    } catch (Exception e) {
                        cs_port = 0;
                    }
                    ci = new ConnectionInfo(params[0],wd_port,cs_port);
                    ci.resolveAddresses();

                } catch (Exception e) {
                    ci = null;
                    return null;
                }
            }
            return ci;
        }

        @Override
        protected void onPreExecute() {
            pbConnect.setVisibility(View.VISIBLE);
            etHost.setEnabled(false);
            etWirelessDevicePort.setEnabled(false);
            etContentServerPort.setEnabled(false);
            etPassword.setEnabled(false);
            cbAutoConnect.setEnabled(false);
            bConnect.setEnabled(false);

        }

        @Override
        protected void onPostExecute(ConnectionInfo ci) {
            if (connectAfterResolve == true && ci != null && mListener != null) { connectClick(); }
            else if (getDialog() != null) {
                pbConnect.setVisibility(View.INVISIBLE);
                if (ci == null) {
                    etHost.setEnabled(true);
                    etWirelessDevicePort.setEnabled(true);
                    etContentServerPort.setEnabled(true);
                    Toast.makeText(getActivity().getApplicationContext(), R.string.connection_unresolved_ip, Toast.LENGTH_SHORT).show();

                } else if (ci.toString().equals(mSettings.getString("pref_wd_address", ""))) {
                    etPassword.setText(mSettings.getString("pref_password", ""));
                    cbAutoConnect.setChecked(mSettings.getBoolean("pref_autocon", false));
                }
                etPassword.setEnabled(true);
                cbAutoConnect.setEnabled(true);
                bConnect.setEnabled(true);
            }
        }
    }
}