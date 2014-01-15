package android.lib;

import android.graphics.drawable.Drawable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;

public class BarTabEclair extends BarTab {

	private CharSequence mText;
	private Drawable mIcon;
	private Class mFragmentClass;
	protected BarTabEclair(FragmentActivity activity, String tag) {
		super(activity, tag);
	}

	@Override
	public BarTab setText(int resId) {
		mText = mActivity.getResources().getText(resId);
		return this;
	}

	@Override
	public BarTab setIcon(int resId) {
		mIcon = mActivity.getResources().getDrawable(resId);
		return this;
	}

	@Override
	public BarTab setFragmentClass(Class <? extends Fragment> cls) {
		mFragmentClass = cls;
		return this;
	}

	@Override
	public CharSequence getText() {
		return mText;
	}

	@Override
	public Drawable getIcon() {
		return mIcon;
	}

	@Override
	public Class getFragmentClass() {
		return mFragmentClass;
	}

	@Override
	public Object getTab() {
		return null;
	}
}
