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

    private static final String TAG = "DownloadEpubTask"; // Thẻ log để debug
    private final Context context; // Ngữ cảnh ứng dụng
    private final WebView webView; // WebView để hiển thị nội dung HTML
    private final ProgressBar progressBar; // ProgressBar để hiển thị tiến trình tải

    /**
     * Hàm khởi tạo để khởi tạo task với các thành phần cần thiết.
     *
     * @param context     Ngữ cảnh ứng dụng.
     * @param webView     WebView để hiển thị nội dung HTML.
     * @param progressBar ProgressBar để hiển thị tiến trình tải.
     */
    public DownloadAndProcessEpubTask(Context context, WebView webView, ProgressBar progressBar) {
        this.context = context;
        this.webView = webView;
        this.progressBar = progressBar;
    }

    /**
     * Thực hiện tác vụ nền để tải file EPUB và chuyển đổi nó sang HTML.
     *
     * @param params Tham số đầu tiên là URL của file EPUB.
     * @return Nội dung HTML của file EPUB, hoặc null nếu có lỗi xảy ra.
     */
    @Override
    protected String doInBackground(String... params) {
        String fileUrl = params[0];
        String htmlContent = null;

        try {
            // Bước 1: Tải file EPUB
            File epubFile = downloadEpub(fileUrl);
            if (epubFile != null) {
                // Bước 2: Chuyển đổi EPUB sang HTML
                htmlContent = EpubConverter.convertEpubToHtml(epubFile);

                // Xóa file EPUB tạm sau khi chuyển đổi
                epubFile.delete();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error processing EPUB file", e);
            return null;
        }

        return htmlContent;
    }

    /**
     * Được gọi sau khi tác vụ nền hoàn thành.
     *
     * @param htmlContent Nội dung HTML của file EPUB, hoặc null nếu có lỗi xảy ra.
     */
    @Override
    protected void onPostExecute(String htmlContent) {
        if (htmlContent != null && !htmlContent.isEmpty()) {
            // Tải nội dung HTML vào WebView
            webView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null);
        } else {
            // Ẩn ProgressBar và hiển thị thông báo lỗi
            progressBar.setVisibility(View.GONE);
            Toast.makeText(context, "Failed to load book content", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Tải file EPUB từ URL được cung cấp.
     *
     * @param fileUrl URL của file EPUB.
     * @return Đối tượng File trỏ đến file EPUB đã tải, hoặc null nếu có lỗi xảy ra.
     */
    private File downloadEpub(String fileUrl) {
        try {
            // Tạo đối tượng URL
            URL url = new URL(fileUrl);

            // Mở kết nối
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.connect();

            // Kiểm tra mã phản hồi HTTP
            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                Log.e(TAG, "Server returned HTTP " + responseCode);
                return null;
            }

            // Lấy kích thước file
            int fileSize = connection.getContentLength();

            // Tạo file tạm
            File tempFile = File.createTempFile("book_", ".epub", context.getCacheDir());

            // Tải file
            InputStream input = connection.getInputStream();
            OutputStream output = new FileOutputStream(tempFile);

            byte[] data = new byte[4096];
            long total = 0;
            int count;

            while ((count = input.read(data)) != -1) {
                total += count;
                // Cập nhật tiến trình
                if (fileSize > 0) {
                    publishProgress((int) (total * 100 / fileSize));
                }
                output.write(data, 0, count);
            }

            // Đóng luồng
            output.flush();
            output.close();
            input.close();

            return tempFile;

        } catch (IOException e) {
            Log.e(TAG, "Error downloading EPUB file", e);
            return null;
        }
    }

    /**
     * Cập nhật tiến trình của tác vụ tải.
     *
     * @param values Giá trị tiến trình, giá trị đầu tiên là phần trăm hoàn thành.
     */
    @Override
    protected void onProgressUpdate(Integer... values) {
        // Cập nhật ProgressBar nếu cần
    }
}