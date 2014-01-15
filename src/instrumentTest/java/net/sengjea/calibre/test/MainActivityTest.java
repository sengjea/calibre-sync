package net.sengjea.calibre.test;

import android.annotation.TargetApi;
import android.app.Activity;
import android.os.Build;
import android.test.ActivityInstrumentationTestCase2;
import net.sengjea.calibre.MainActivity;

/**
 * This is a simple framework for a test of an Application.  See
 * {@link android.test.ApplicationTestCase ApplicationTestCase} for more information on
 * how to write and extend Application tests.
 * <p/>
 * To run this test, you can type:
 * adb shell am instrument -w \
 * -e class net.sengjea.calibre.MainActivityTest \
 * net.sengjea.calibre.tests/android.test.InstrumentationTestRunner
 */
public class MainActivityTest extends ActivityInstrumentationTestCase2<MainActivity> {
    private Activity mActivity;
    @TargetApi(Build.VERSION_CODES.FROYO)
    public MainActivityTest() {
        super(MainActivity.class);
    }
    public void testSomething() {
        launchActivity("net.sengjea.calibre",
                MainActivity.class, null);
    }

}
