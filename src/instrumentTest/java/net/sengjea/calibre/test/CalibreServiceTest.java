package net.sengjea.calibre.test;

import android.content.Intent;
import android.test.ServiceTestCase;
import net.sengjea.calibre.CalibreService;

import java.util.concurrent.CountDownLatch;

/**
 * User: sengjea
 * Date: 30/12/13
 * Time: 05:43
 */
public class CalibreServiceTest extends ServiceTestCase<CalibreService>
        implements CalibreService.CalibreListener {
    /**
     * Constructor
     *
     * @param serviceClass The type of the service under test.
     */
    private Intent intent;
    private CalibreService mService;
    final CountDownLatch signal = new CountDownLatch(1);
    public CalibreServiceTest() {
        super(CalibreService.class);

    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
    }
    private void serviceSetUp() {
        startService(intent);
        intent = new Intent(getContext(),
                CalibreService.class);
        //CalibreService.setClientTimeout(1);
        CalibreService.CalibreBinder calBinder = (CalibreService.CalibreBinder) bindService(intent);
        mService = calBinder.getService();
        mService.setListener(this);


    }
    public void testStartUp() {
        serviceSetUp();

        assertNotNull(mService);
        assertNotNull(mService.getListener());
        assertNotNull(mService.getRootDirectory());
        assertEquals(true, mService.getRootDirectory().canWrite());
        //assertEquals(true, mService.metadataThreadIsRunning());
        //assertEquals(true, mService.discoveryThreadIsRunning());

    }
    public void testTriggerDiscovery() throws InterruptedException {
        serviceSetUp();
        mService.discoverServers();

    }
    @Override
    public void onConnectionStateChanged(CalibreService.ConnectionState connectionState) {
        switch (connectionState) {
            case CONNECTING:
                assertNull("Must have connectionInfo", mService.getCurrentServer());
            break;
        }

    }

    @Override
    public void onDiscoveryStateChanged(CalibreService.DiscoveryState discoveryState) {
        switch (discoveryState) {
            case DONE_DISCOVERY:
                signal.countDown();
                assertNotNull("ListOfServers is Null", mService.getListOfServers());
                if (mService.getListOfServers().isEmpty()) {
                }
            break;
            case NO_SERVER:
            break;
        }
    }

    @Override
    public void onErrorReported(CalibreService.ErrorType e) {

    }
}
