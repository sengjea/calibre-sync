package net.sengjea.calibre;

import android.os.AsyncTask;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;

public class AsyncDownloader extends AsyncTask<String, Integer, String> {
    private String outputPath;
    private File oFile;
    @Override
    protected String doInBackground(String... stringList) {

        try {
            URL url = new URL(stringList[0]);
            URLConnection connection = url.openConnection();
            connection.connect();
            // this will be useful so that you can show a typical 0-100% progress bar
            int fileLength = connection.getContentLength();
            String raw = connection.getHeaderField("Content-Disposition");
            // raw = "attachment; filename=abc.jpg"
            outputPath = "tmp";
            if (raw != null && raw.indexOf("=") != -1) {
                String fileName[] = raw.split("=");
                outputPath = fileName[fileName.length - 1].replace("\"", "");
            }
            oFile = new File(stringList[1], outputPath);
            // download the file
            InputStream input = new BufferedInputStream(url.openStream());
            OutputStream output = new FileOutputStream(oFile);

            byte data[] = new byte[1024];
            long total = 0;
            int count;
            while ((count = input.read(data)) != -1) {
                total += count;
                // publishing the progress....
                publishProgress((int) (total * 100 / fileLength));
                output.write(data, 0, count);
            }


            output.flush();
            output.close();
            input.close();
            return outputPath;
        } catch (Exception e) {
           e.printStackTrace();
           this.onCancelled();
        }
        return null;
    }

    @Override
    protected void onCancelled() {
        super.onCancelled();
        if (oFile != null) {
            oFile.delete();
        }
    }
}