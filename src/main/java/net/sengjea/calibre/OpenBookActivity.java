package net.sengjea.calibre;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.webkit.MimeTypeMap;
import android.widget.Toast;

import java.io.File;

/**
 * User: sengjea
 * Date: 23/12/13
 * Time: 15:54
 */
public class OpenBookActivity extends Activity {
    public static final String OPEN_BOOK = "net.sengjea.calibre.OPEN_BOOK";
    public static final String EXTRA_UUID = "uuid";
    public static final String EXTRA_URL = "url";
    private static MetadataDatabaseHelper mDB;
    private static Context mContext;
    private static File rootDirectory;
    private static String currentUuid = null;
    private ProgressDialog mProgressDialog;
    private AsyncDownloader downloadTask = new AsyncDownloader() {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mProgressDialog.setMessage(mContext.getString(R.string.dialog_downloading_book));
            mProgressDialog.setIndeterminate(true);
            mProgressDialog.setMax(100);
            mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            mProgressDialog.setProgress(0);
            mProgressDialog.setCancelable(false);
            mProgressDialog.show();
        }

        @Override
        protected void onProgressUpdate(Integer... progress) {
            super.onProgressUpdate(progress);
            if (mProgressDialog != null) {
                mProgressDialog.setIndeterminate(false);
                mProgressDialog.setProgress(progress[0]);
            }
        }

        @Override
        protected void onPostExecute(String lpath) {
            if (mDB != null && lpath != null && currentUuid != null
                    && mDB.insertBook(currentUuid,lpath)) {

                openBook();
            } else if (new File(rootDirectory,lpath).delete()) {
                Toast.makeText(mContext, R.string.toast_download_cancelled, Toast.LENGTH_LONG).show();
            }
            mProgressDialog.dismiss();
            finish();
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
            Toast.makeText(mContext, R.string.toast_download_cancelled, Toast.LENGTH_LONG).show();

        }
    };



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mDB = new MetadataDatabaseHelper(getApplicationContext());
        mContext = getApplicationContext();
        rootDirectory = new File(Environment.getExternalStorageDirectory(),
                PreferenceManager.getDefaultSharedPreferences(getApplicationContext())
                        .getString("pref_root_dir", "eBooks/"));
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (((currentUuid = getIntent().getStringExtra(EXTRA_UUID)) != null) &&
                !openBook() && getIntent().hasExtra(EXTRA_URL)) {
            mProgressDialog = new ProgressDialog(this);
            downloadTask.execute(getIntent().getStringExtra(EXTRA_URL), rootDirectory.toString());
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (downloadTask.getStatus() == AsyncDownloader.Status.RUNNING)
            downloadTask.cancel(true);
        mDB.close();
    }

    private boolean  openBook() {
       Cursor cs = mDB.getByUuid(new String[] {"lpath"}, currentUuid);
        if (!cs.moveToFirst() || cs.getString(0) == null) return false;
        try {
            Uri uri = Uri.fromFile(new File(rootDirectory,cs.getString(0)));
            String mimeExtension = MimeTypeMap.getFileExtensionFromUrl(uri.toString());
            Intent openBookIntent = new Intent(Intent.ACTION_VIEW);
            //openBookIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            openBookIntent.setDataAndType(uri, MimeTypeMap.getSingleton().getMimeTypeFromExtension(mimeExtension));
            startActivity(openBookIntent);
            AppRater.app_launched(getApplicationContext());
            finish();
        } catch (Exception e) {
            Toast.makeText(getApplicationContext(), R.string.toast_no_app_installed, Toast.LENGTH_LONG).show();
            return false;
        }
        return true;
    }
}
