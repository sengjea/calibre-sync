package android.lib;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.view.View;
import android.widget.TabHost;
import android.widget.TabHost.TabSpec;

public class BarTabManagerEclair extends BarTabManager implements TabHost.OnTabChangeListener {

	private TabHost mTabHost;
	BarTab mLastTab;
	public BarTabManagerEclair(FragmentActivity activity) {
		super(activity);
		mActivity = activity;
	}

	@Override
	public void addTab(BarTab tab) {
        super.addTab(tab);
		String tag = tab.getTag();
		TabSpec spec;
		if (tab.getIcon() != null) {
			spec = mTabHost.newTabSpec(tag).setIndicator(tab.getText(), tab.getIcon());
		} else {
			spec = mTabHost.newTabSpec(tag).setIndicator(tab.getText());
		}
		spec.setContent(new DummyTabFactory(mActivity));

		Fragment frag = mActivity.getSupportFragmentManager().findFragmentByTag(tag);
		//tab.setFragment(frag);

		if (frag != null && !frag.isDetached()) {
			FragmentTransaction ft = mActivity.getSupportFragmentManager().beginTransaction();
			ft.detach(frag);
			ft.commit();
		}

		mTabHost.addTab(spec);
	}

    @Override
    public void switchTab(BarTab thisTab) {
        if (!mTabHost.getCurrentTabTag().equals(thisTab.getTag()))
            mTabHost.setCurrentTabByTag(thisTab.getTag());
        FragmentTransaction ft = mActivity.getSupportFragmentManager().beginTransaction();
        ft.disallowAddToBackStack();
        if (mLastTab != thisTab) {
            if (mLastTab != null && mActivity.getSupportFragmentManager().findFragmentByTag(mLastTab.getTag()) != null) {
                mLastTab.unselectTab(ft);
            }
            if (thisTab != null) {
                thisTab.selectTab(ft);
            }
            mLastTab = thisTab;
        } else {
            thisTab.reselectTab(ft);
        }
        ft.commit();
        mActivity.getSupportFragmentManager().executePendingTransactions();
    }

    @Override
	protected void onSaveInstanceState(Bundle state) {
		state.putString("tab", mTabHost.getCurrentTabTag());

	}

	@Override
	protected void onRestoreInstanceState(Bundle state) {
		if (state != null) {
			mTabHost.setCurrentTabByTag(state.getString("tab"));
		}

	}

	@Override
	protected void setup() {
		if (mTabHost == null) {
			mTabHost = (TabHost) mActivity.findViewById(android.R.id.tabhost);
			mTabHost.setup();
			mTabHost.setOnTabChangedListener(this);
		}

	}

	@Override
	public void onTabChanged(String tabId) {
		BarTab thisTab = mTabs.get(tabId);
		switchTab(thisTab);
	}


	static class DummyTabFactory implements TabHost.TabContentFactory {
		private final Context mContext;

		public DummyTabFactory(Context context) {
			mContext = context;
		}
		@Override
		public View createTabContent(String tag) {
			View v = new View(mContext);
			v.setMinimumHeight(0);
			v.setMinimumWidth(0);
			return v;
		}

	}

}
