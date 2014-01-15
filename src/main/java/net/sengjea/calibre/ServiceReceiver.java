package net.sengjea.calibre;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;

public class ServiceReceiver extends BroadcastReceiver {
	public ServiceReceiver() {
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		Intent service_intent = new Intent(context,CalibreService.class);
		if(intent.getAction().equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
		    NetworkInfo networkInfo = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
		    if(networkInfo.isConnected()) {
		        context.startService(service_intent);
		    }
		}
	}
}
