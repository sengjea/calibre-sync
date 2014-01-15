package android.lib;

import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;

import java.util.HashMap;

public abstract class BarTabManager {
	protected FragmentActivity mActivity;
    protected final HashMap<String, BarTab> mTabs = new HashMap<String, BarTab>();

	protected BarTabManager(FragmentActivity activity) {
		mActivity = activity;
	}
	
	public static BarTabManager createInstance(FragmentActivity activity) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			return new BarTabManagerHoneycomb(activity);
		} else {
			return new BarTabManagerEclair(activity);
		}
	}
	
	public BarTab newTab(String tag) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			return new BarTabHoneycomb(mActivity,tag);
		} else {
			return new BarTabEclair(mActivity,tag);
		}
	}
	
	public void addTab(BarTab tab) {
        mTabs.put(tab.getTag(),tab);
    }
    public void switchTabByTag(String s) {
       if (mTabs.containsKey(s)) {
           switchTab(mTabs.get(s));
       }
    }
    public abstract void switchTab(BarTab tab);
	
	protected abstract void onSaveInstanceState(Bundle state);
	
	protected abstract void onRestoreInstanceState(Bundle state);
	
	protected abstract void setup();
	
}
