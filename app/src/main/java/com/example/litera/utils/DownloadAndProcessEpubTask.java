package com.example.litera.utils;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;
import android.widget.ProgressBar;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class DownloadAndProcessEpubTask extends AsyncTask<String, Integer, String> {

    private static final String TAG = "DownloadEpubTask";
    private final Context context;
    private final WebView webView;
    private final ProgressBar progressBar;

    public DownloadAndProcessEpubTask(Context context, WebView webView, ProgressBar progressBar) {
        this.context = context;
        this.webView = webView;
        this.progressBar = progressBar;
    }

    @Override
    protected String doInBackground(String... params) {
        String fileUrl = params[0];
        String htmlContent = null;

        try {
            // Step 1: Download EPUB file
            File epubFile = downloadEpub(fileUrl);
            if (epubFile != null) {
                // Step 2: Convert EPUB to HTML
                htmlContent = EpubConverter.convertEpubToHtml(epubFile);

                // Delete temporary EPUB file after conversion
                epubFile.delete();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error processing EPUB file", e);
            return null;
        }

        return htmlContent;
    }

    @Override
    protected void onPostExecute(String htmlContent) {
        if (htmlContent != null && !htmlContent.isEmpty()) {
            // Load the HTML content into WebView
            webView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null);
        } else {
            progressBar.setVisibility(View.GONE);
            Toast.makeText(context, "Failed to load book content", Toast.LENGTH_SHORT).show();
        }
    }

    private File downloadEpub(String fileUrl) {
        try {
            // Create URL object
            URL url = new URL(fileUrl);

            // Open connection
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.connect();

            // Check HTTP response code
            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                Log.e(TAG, "Server returned HTTP " + responseCode);
                return null;
            }

            // Get file size
            int fileSize = connection.getContentLength();

            // Create temporary file
            File tempFile = File.createTempFile("book_", ".epub", context.getCacheDir());

            // Download file
            InputStream input = connection.getInputStream();
            OutputStream output = new FileOutputStream(tempFile);

            byte[] data = new byte[4096];
            long total = 0;
            int count;

            while ((count = input.read(data)) != -1) {
                total += count;
                // Publish progress
                if (fileSize > 0) {
                    publishProgress((int) (total * 100 / fileSize));
                }
                output.write(data, 0, count);
            }

            // Close streams
            output.flush();
            output.close();
            input.close();

            return tempFile;

        } catch (IOException e) {
            Log.e(TAG, "Error downloading EPUB file", e);
            return null;
        }
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        // Update progress bar if needed
    }
}