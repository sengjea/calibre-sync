package android.lib;

import android.graphics.drawable.Drawable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;

public abstract class BarTab {
	final FragmentActivity mActivity;
	final String mTag;
	BarTab.OnTabChangeListener mListener;

	protected BarTab(FragmentActivity activity, String tag) {
		mActivity = activity;
		mTag = tag;
	}
	public abstract BarTab setText(int resId);
	public abstract BarTab setIcon(int resId);

	public abstract BarTab setFragmentClass(Class <? extends Fragment > cls);
	public abstract CharSequence getText();
	public abstract Drawable getIcon();
	public abstract Class getFragmentClass();
	public abstract Object getTab();

	public interface OnTabChangeListener {
		public void onTabSelected(BarTab tab, FragmentTransaction ft);
		public void onTabUnselected(BarTab tab, FragmentTransaction ft);
		public void onTabReselected(BarTab tab, FragmentTransaction ft);
	}
	public BarTab setOnTabChangeListener(OnTabChangeListener callback) {
		mListener = callback;
		return this;
	}
	public OnTabChangeListener getOnTabChangeListener() {
		return mListener;
	}
	public void selectTab(FragmentTransaction ft) {
		Fragment frag = mActivity.getSupportFragmentManager().findFragmentByTag(mTag);
		if (frag == null) {
			frag = Fragment.instantiate(mActivity, getFragmentClass().getName());
			ft.add(android.R.id.tabcontent, frag, mTag);
		} else {
			ft.attach(frag);
		}
		if (mListener != null) {
			mListener.onTabSelected(this, ft);
		}

	}

	public void unselectTab(FragmentTransaction ft) {
		Fragment frag = mActivity.getSupportFragmentManager().findFragmentByTag(mTag);
		if (frag != null) {
			ft.detach(frag);
		}
		if (mListener != null) {
			mListener.onTabUnselected(this, ft);
		}
	}

	public void reselectTab(FragmentTransaction ft) {
		if (mListener != null) {
			mListener.onTabReselected(this, ft);
		}
	}
	public String getTag() {
		return mTag;
	}
	public Fragment getFragment() {
		return mActivity.getSupportFragmentManager().findFragmentByTag(mTag);
	}
}
