package com.example.litera.views.activities;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.RatingBar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.example.litera.R;
import com.example.litera.repositories.BookRepository;
import com.example.litera.utils.GoogleDriveUtils;
import com.example.litera.viewmodels.BookDetailViewModel;

public class BookDetailActivity extends AppCompatActivity {

    private static final String TAG = "BookDetailActivity";
    private BookDetailViewModel bookDetailViewModel;
    private ProgressBar progressBar;
    private RatingBar ratingBar;
    private TextView ratingText;

    @SuppressLint("DefaultLocale")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_book_detail);

        BookRepository.getInstance().fixRatingCountInconsistencies();
        BookRepository.getInstance().clearCache();


        // Initialize UI elements
        ImageView bookCover = findViewById(R.id.bookCover);
        TextView bookTitle = findViewById(R.id.bookTitle);
        TextView bookAuthor = findViewById(R.id.bookAuthor);
        TextView bookDescription = findViewById(R.id.tvDescription);
        TextView priceValue = findViewById(R.id.priceValue);
        Button addToCart = findViewById(R.id.btnAddToCart);
        ImageButton btnBack = findViewById(R.id.btnBack);
        ratingBar = findViewById(R.id.ratingBar);
        ratingText = findViewById(R.id.ratingText); // Đảm bảo có TextView này trong layout

        // Đặt ratingBar là chỉ hiển thị (không cho người dùng đánh giá trong màn hình này)
        ratingBar.setIsIndicator(true);

        // Initialize ViewModel
        bookDetailViewModel = new ViewModelProvider(this).get(BookDetailViewModel.class);

        // Observe LiveData
        bookDetailViewModel.getSelectedBook().observe(this, book -> {
            if (book != null) {
                bookTitle.setText(book.getTitle());
                if (book.getAuthor() != null) {
                    bookAuthor.setText(book.getAuthor().getName());
                } else {
                    bookAuthor.setText("Unknown author");
                }
                bookDescription.setText(book.getDescription());

                // Hiển thị giá tiền của sách điện tử
                String price = book.getPrice();
                if (price != null && !price.isEmpty()) {
                    // Thêm $ nếu chưa có
                    if (!price.startsWith("$")) {
                        price = "$" + price;
                    }
                    priceValue.setText(price);
                } else {
                    priceValue.setText("$0.00");
                }

                // Hiển thị rating
                String ratingStr = book.getRating();
                if (ratingStr != null && !ratingStr.isEmpty()) {
                    try {
                        float ratingValue = Float.parseFloat(ratingStr);
                        ratingBar.setRating(ratingValue);

                        // Hiển thị rating dưới dạng text
                        if (ratingText != null) {
                            int ratingCount = book.getRatingCountAsInt();  // Sử dụng phương thức helper
                            ratingText.setText(String.format("%.1f (%d %s)",
                                    ratingValue,
                                    ratingCount,
                                    ratingCount == 1 ? "rating" : "ratings"));
                        }
                    } catch (NumberFormatException e) {
                        ratingBar.setRating(0f);
                        if (ratingText != null) {
                            ratingText.setText("No ratings yet");
                        }
                    }
                } else {
                    ratingBar.setRating(0f);
                    if (ratingText != null) {
                        ratingText.setText("No ratings yet");
                    }
                }

                // Lấy URL gốc từ Firebase
                String originalUrl = book.getImageUrl();
                Log.d(TAG, "Original Google Drive URL: " + originalUrl);

                // Chuyển đổi URL Google Drive sang URL trực tiếp
                String directUrl = GoogleDriveUtils.convertToDirect(originalUrl);
                Log.d(TAG, "Direct URL for loading: " + directUrl);

                // Tải ảnh bằng Glide với URL đã được chuyển đổi
                if (directUrl != null) {
                    Glide.with(this)
                            .load(directUrl)
                            .placeholder(R.drawable.z6456262903514_8961d85cbd925e7e3f1929bd368cd347)
                            .error(R.drawable.sao)
                            .into(bookCover);
                } else {
                    // Sử dụng ảnh mặc định nếu không thể chuyển đổi URL
                    bookCover.setImageResource(R.drawable.z6456262903514_8961d85cbd925e7e3f1929bd368cd347);
                }
            }
        });

        // Observe loading state
        bookDetailViewModel.getIsLoading().observe(this, isLoading -> {
            if (progressBar != null) {
                progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            }
        });

        // Observe error messages
        bookDetailViewModel.getErrorMessage().observe(this, error -> {
            if (error != null && !error.isEmpty()) {
                Toast.makeText(this, error, Toast.LENGTH_LONG).show();
            }
        });

        // Get book ID from intent
        String bookId = getIntent().getStringExtra("bookId");
        if (bookId != null) {
            bookDetailViewModel.selectBook(bookId);
        } else {
            Toast.makeText(this, "Book ID not provided", Toast.LENGTH_SHORT).show();
            finish();
        }

        // Set up button click listener
        addToCart.setOnClickListener(v -> {
            Intent intent = new Intent(BookDetailActivity.this, AddToCartActivity.class);
            // Pass the book ID to the AddToCartActivity
            intent.putExtra("bookId", bookId);
            startActivity(intent);
        });

        // Back button
        btnBack.setOnClickListener(v -> finish());
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Khi quay lại màn hình, tải lại dữ liệu sách để có thông tin rating mới nhất
        String bookId = getIntent().getStringExtra("bookId");
        if (bookId != null) {
            // Xóa cache trước khi tải lại
            BookRepository.getInstance().clearCache();
            bookDetailViewModel.selectBook(bookId);

            // Khắc phục vấn đề không nhất quán
            BookRepository.getInstance().fixRatingCountInconsistencies();
            BookRepository.getInstance().clearCache();

            // Get bookId from intent
            bookId = getIntent().getStringExtra("bookId");

            // Debug rating system
            if (bookId != null && !bookId.isEmpty()) {
                BookRepository.getInstance().debugRatingSystem(bookId);
            }
        }
    }
}