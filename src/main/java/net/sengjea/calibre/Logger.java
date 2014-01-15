package net.sengjea.calibre;

import android.util.Log;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;

/**
 * Created by sengjea on 1/14/14.
 */
public class Logger {
    private static String LOG_TAG = "CSync";
    private static File debugFile = null;
    public static void set(File f) {
        if (f != null && f.exists()) f.delete();
        d("debugFile is now "+ (f != null ? f.toString(): "(none)"));
        debugFile = f;
    }
    public static void d(String s) {
        Log.d(LOG_TAG, s);
        if (debugFile != null) {
            write(s, debugFile, true);
        }
    }
    public static void e(Throwable e) {
        StackTraceElement[] ste = e.getStackTrace();
        for (int i = 0; (i < ste.length) && (i < 32); i++) {
            d(ste[i].toString());
        }
    }
    private static synchronized void write(String s, File f, boolean append) {
        BufferedOutputStream fw;
        try {
            fw = new BufferedOutputStream(new FileOutputStream(f, append));
            fw.write(s.getBytes());
            fw.flush();
            fw.close();
        } catch (Throwable e) {
            Log.e(LOG_TAG,"Cant Even Write to Debug File!",e);
        }
    }
}
