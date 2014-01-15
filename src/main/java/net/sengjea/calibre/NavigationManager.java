package net.sengjea.calibre;

import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;

public class NavigationManager {
	public static FragmentActivity mActivity;
	private FragmentManager mFragmentManager;
	public NavigationManager(FragmentActivity activity) {
		mActivity = activity;
		mFragmentManager = mActivity.getSupportFragmentManager();
	}
}
