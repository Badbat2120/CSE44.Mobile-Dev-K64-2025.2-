package com.example.litera.views.activities;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.litera.R;
import com.example.litera.utils.DownloadAndProcessEpubTask;
import com.example.litera.utils.GoogleDriveUtils;
import com.example.litera.viewmodels.MainViewModel;

public class ReadBookActivity extends AppCompatActivity {
    private static final String TAG = "ReadBookActivity";

    private WebView webView;
    private ProgressBar progressBar;
    private ImageButton btnBack;
    private TextView bookTitle;
    private MainViewModel mainViewModel;

    private String bookId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_read_book);

        // Get bookId from intent
        bookId = getIntent().getStringExtra("bookId");

        // Initialize ViewModel
        mainViewModel = new ViewModelProvider(this).get(MainViewModel.class);

        // Initialize views
        initViews();

        // Load book content
        if (bookId != null && !bookId.isEmpty()) {
            loadBookContent();
        } else {
            Toast.makeText(this, "Invalid book ID", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void initViews() {
        webView = findViewById(R.id.webView);
        progressBar = findViewById(R.id.progressBar);
        btnBack = findViewById(R.id.btnBack);
        bookTitle = findViewById(R.id.bookTitle);

        // Configure WebView
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setAllowFileAccess(true);
        webSettings.setBuiltInZoomControls(true);
        webSettings.setDisplayZoomControls(false);
        webSettings.setDefaultTextEncodingName("UTF-8");

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                progressBar.setVisibility(View.GONE);
            }
        });

        // Setup back button
        btnBack.setOnClickListener(v -> finish());
    }

    private void loadBookContent() {
        // Show progress bar
        progressBar.setVisibility(View.VISIBLE);

        // Get book from ViewModel
        mainViewModel.getBookById(bookId).observe(this, book -> {
            if (book != null) {
                // Update book title
                bookTitle.setText(book.getTitle());
                Log.d(TAG, "Book loaded: " + book.getTitle());

                // Get content URL (remember, field in Firebase is "content" but getter is getContentUrl)
                String contentUrl = book.getContentUrl();
                Log.d(TAG, "Content URL: " + contentUrl);

                if (contentUrl != null && !contentUrl.isEmpty()) {
                    // Kiểm tra xem có phải URL Google Drive không
                    if (contentUrl.contains("drive.google.com")) {
                        // Chuyển đổi Google Drive URL thành URL trực tiếp
                        String directUrl = GoogleDriveUtils.convertToDirect(contentUrl);
                        Log.d(TAG, "Direct URL: " + directUrl);

                        // Tải và xử lý file EPUB
                        new DownloadAndProcessEpubTask(this, webView, progressBar).execute(directUrl);
                    } else {
                        // Thử tải trực tiếp URL (nếu đã là HTML)
                        webView.loadUrl(contentUrl);
                    }
                } else {
                    showError("Book content URL not available");
                }
            } else {
                showError("Failed to load book information");
            }
        });
    }

    private void showError(String message) {
        progressBar.setVisibility(View.GONE);
        Log.e(TAG, message);
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        finish();
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}