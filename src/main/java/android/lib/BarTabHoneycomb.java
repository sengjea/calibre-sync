package android.lib;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.ActionBar.Tab;
import android.graphics.drawable.Drawable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;

@SuppressLint("NewApi")
public class BarTabHoneycomb extends BarTab implements ActionBar.TabListener {
	ActionBar.Tab mTab;
	
	Class mFragmentClass;
	
	public BarTabHoneycomb(FragmentActivity activity, String tag) {
		super(activity,tag);
		mTab = activity.getActionBar().newTab();
		mTab.setTabListener(this);
	}

	@Override
	public BarTab setText(int resId) {
		mTab.setText(resId);
		return this;
	}

	@Override
	public BarTab setIcon(int resId) {
		mTab.setIcon(resId);
		return this;
	}

	@Override
	public BarTab setFragmentClass(Class <? extends Fragment> fc) {
		mFragmentClass = fc;
		return this;
	}

	@Override
	public CharSequence getText() {
		return mTab.getText();
	}

	@Override
	public Drawable getIcon() {
		return mTab.getIcon();
	}

	@Override
	public Class getFragmentClass() {
		return mFragmentClass;
	}

	@Override
	public Object getTab() {
		return mTab;
	}


	public void onTabReselected(Tab tab, android.app.FragmentTransaction new_ft) {
		FragmentTransaction ft = mActivity.getSupportFragmentManager().beginTransaction();
		ft.disallowAddToBackStack();
		this.reselectTab(ft);
		ft.commit();
		
	}


	public void onTabSelected(Tab tab, android.app.FragmentTransaction new_ft) {
		FragmentTransaction ft = mActivity.getSupportFragmentManager().beginTransaction();
		ft.disallowAddToBackStack();
		this.selectTab(ft);
		ft.commit();
	}


	public void onTabUnselected(Tab tab, android.app.FragmentTransaction new_ft) {
		FragmentTransaction ft = mActivity.getSupportFragmentManager().beginTransaction();
		ft.disallowAddToBackStack();
		this.unselectTab(ft);
		ft.commit();
		
	}

}
