package android.lib;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;

@SuppressLint("NewApi")
public class BarTabManagerHoneycomb extends BarTabManager {
	ActionBar mActionBar;
	public BarTabManagerHoneycomb(FragmentActivity activity) {
		super(activity);
	}

	@Override
	public void addTab(BarTab tab) {
        super.addTab(tab);
		String tag = tab.getTag();
		Fragment frag = mActivity.getSupportFragmentManager().findFragmentByTag(tag);
		//tab.setFragment(frag);
		if (frag != null && !frag.isDetached()) {
			FragmentTransaction ft = mActivity.getSupportFragmentManager().beginTransaction();
			ft.detach(frag);
			ft.commit();
		}
		mActionBar.addTab((ActionBar.Tab) tab.getTab());

	}

    @Override
    public void switchTab(BarTab tab) {
        mActionBar.selectTab((ActionBar.Tab) tab.getTab());
    }

    @Override
	protected void onSaveInstanceState(Bundle state) {
		int position = mActionBar.getSelectedTab().getPosition();
		state.putInt("tab_position", position);

	}

	@Override
	protected void onRestoreInstanceState(Bundle state) {
		int position = state.getInt("tab_position");
		mActionBar.setSelectedNavigationItem(position);

	}

	@Override
	protected void setup() {
		if (mActionBar == null) {
			mActionBar = mActivity.getActionBar();
			mActionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
		}

	}

}
