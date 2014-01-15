package net.sengjea.calibre;

import android.annotation.SuppressLint;
import android.database.Cursor;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.util.ArrayList;

/**
 * Created by sengjea on 1/14/14.
 */
@SuppressLint("NewApi")
public class BookCrawler {
    private MetadataDatabaseHelper mDB;
    private File rootDirectory;
    private JSONObject driveInfo;
    private ArrayList<JSONObject> newBooks;
    private Thread metadata_thread;
    private String[] mExtensions;
    private Runnable metadataRunnable = new Runnable() {
        public void run() {
            try {
                if (rootDirectory == null) return;
                parseExistingMetadataFile();
                parseExistingDriveInfoFile();
                findNewBooks(rootDirectory);
                clearDeletedBooks();

            } catch (Throwable e) {
                Logger.e(e);
            }

        }

    };
    public BookCrawler(MetadataDatabaseHelper db, String[] exts) {
        mDB = db;
        mExtensions = exts;
        newBooks = new ArrayList<JSONObject>();
        driveInfo = null;
    }
    public boolean isCrawling() {
        return (metadata_thread != null) && metadata_thread.isAlive();
    }

    public void waitForScan() {
        try {
            if (isCrawling()) metadata_thread.join();
        } catch (InterruptedException e) {
            Logger.e(e);
        }
    }

    public JSONObject getDriveInfo() {
        return (driveInfo != null ? driveInfo : new JSONObject());
    }
    public void setDriveInfo(JSONObject dI) {
        if (isCrawling()) return;
        driveInfo = dI;
        if (rootDirectory != null && dI != null) {
            File f = new File(rootDirectory, "driveinfo.calibre");
            writeToFile(new StringBuilder(driveInfo.toString()), f, false);
        }
    }
    private void parseExistingMetadataFile() throws JSONException {
        JSONArray tmp_metadata;
        File cf = new File(rootDirectory, "metadata.calibre");
        if (cf.exists()) {
            tmp_metadata = new JSONArray(readFromFile(cf).toString());

            for (int i = 0; i < tmp_metadata.length(); i++) {
                JSONObject tmp_json = tmp_metadata.getJSONObject(i);
                if ((new File(rootDirectory, tmp_json.getString("lpath"))).exists()) {
                    mDB.insertJSON(tmp_json);
                }
            }
        }
    }
    private void parseExistingDriveInfoFile() {
        File cf = new File(rootDirectory, "driveinfo.calibre");
        if (cf.exists()) {
            String di = readFromFile(cf).toString();
            try {
                driveInfo = new JSONObject(di);
            } catch (JSONException e) {
                Logger.d("JSONException while reading dI: " + di);
            }
        }
    }
    private void clearDeletedBooks() {
        Cursor cs;
        cs = mDB.getNotNull(new String[]{"_id", "lpath"}, "lpath");
        cs.moveToFirst();
        while (!cs.isAfterLast()) {
            if (!new File(rootDirectory, cs.getString(1)).exists()) {
                mDB.deleteBook(cs.getInt(0));
                Logger.d("Deleted " + cs.getString(1));
            }
            cs.moveToNext();
        }
        cs.close();
    }

    private void findNewBooks(File dir) {
        JSONObject new_book;
        if (!dir.toString().startsWith(rootDirectory.toString())) return;

        for (File f : dir.listFiles()) {
            if (f.isDirectory()) {
                findNewBooks(f);
            } else if (fileHasAcceptedExtension(f)) {
                String filename = f.toString().substring(rootDirectory.toString().length() + 1);
                Cursor cs = mDB.getByPath(new String[]{"uuid"}, filename);
                if (!cs.moveToFirst()) {
                    try {
                        new_book = new JSONObject()
                                .put("_new_book_", true)
                                .put("lpath", filename);
                        Logger.d("New Book Found: " + f.toString());
                        newBooks.add(new_book);
                    } catch (JSONException e) {
                        Logger.d("Could not add Book: " + f.toString());
                    }
                }
                cs.close();
            }
        }

    }

    private boolean fileHasAcceptedExtension(File f) {
        for (String e : mExtensions) {
            if (f.toString().endsWith(e)) return true;
        }
        return false;
    }

    public boolean scanFolder(File f) {
        if (isCrawling()) {
            Logger.d("Already Running Crawler");
            return false;
        }
        rootDirectory = f;
        newBooks.clear();
        metadata_thread = new Thread(metadataRunnable);
        metadata_thread.start();
        return true;
    }
    private void writeToFile(StringBuilder s, File f, boolean append) {
        BufferedOutputStream fw;
        try {
            fw = new BufferedOutputStream(new FileOutputStream(f, append));
            fw.write(s.toString().getBytes());
            fw.flush();
            fw.close();
        } catch (Throwable e) {
            //logAndDie(e);
        }
    }
    private static StringBuilder readFromFile(File f) {
        StringBuilder data = new StringBuilder();
        byte[] fileBuf = new byte[8192];
        int c;
        BufferedInputStream fr;
        try {
            fr = new BufferedInputStream(new FileInputStream(f));
            while ((c = fr.read(fileBuf, 0, fileBuf.length)) > 0) {
                data.append(new String(fileBuf, 0, c, "utf-8"));
            }
            fr.close();
        } catch (Throwable e) {
            Logger.e(e);
        }
        return data;
    }

    public int numNewBooks() {
        return ((!isCrawling() && newBooks != null) ? newBooks.size() : 0);
    }

    public ArrayList<JSONObject> getNewBooks() {
        return (!isCrawling() ? newBooks : null);
    }

    public void setAcceptedExtensions(String[] exts) {
        if (isCrawling()) {
            Logger.d("Not Changing Accepted Exts while Crawling");
            return;
        }
        mExtensions = exts;
    }
}
