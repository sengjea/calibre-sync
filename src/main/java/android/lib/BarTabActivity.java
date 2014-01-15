/**
 * 
 */
package android.lib;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;

/**
 * @author sengjea
 *
 */
public abstract class BarTabActivity extends FragmentActivity {
	protected BarTabManager mBarTabManager;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mBarTabManager = BarTabManager.createInstance(this);
	}
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		mBarTabManager.onSaveInstanceState(outState);
	}
	
	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		mBarTabManager.onRestoreInstanceState(savedInstanceState);
	}
	
	protected BarTabManager getBarTabManager() {
		mBarTabManager.setup();
		return mBarTabManager;
	}
	
}
