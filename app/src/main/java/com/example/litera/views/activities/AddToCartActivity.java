package com.example.litera.views.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.example.litera.R;
import com.example.litera.models.Book;
import com.example.litera.utils.GoogleDriveUtils;
import com.example.litera.viewmodels.MainViewModel;
import com.example.litera.views.fragments.ReadBookActivity;
import com.google.android.material.snackbar.Snackbar;

public class AddToCartActivity extends AppCompatActivity {

    // UI elements
    private ImageButton btnBack;
    private ImageView bookCover;
    private TextView bookTitle, bookAuthor, bookPrice;
    private Button readButton, buyButton;
    private RatingBar ratingBar;
    private ImageButton btnFavorite, btnLike, btnShare;
    private MainViewModel mainViewModel;

    // Data
    private String bookId;
    private boolean isFavorite = false;
    private boolean isLiked = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_addtocart_book);

        // Get bookId from intent
        bookId = getIntent().getStringExtra("bookId");

        // Initialize ViewModel
        mainViewModel = new ViewModelProvider(this).get(MainViewModel.class);

        // Initialize views
        initViews();

        // Setup listeners
        setupListeners();

        // Load book data
        if (bookId != null && !bookId.isEmpty()) {
            loadBookData(bookId);
        } else {
            displayDemoBookData();
        }
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        bookCover = findViewById(R.id.bookCover);
        bookTitle = findViewById(R.id.bookTitle);
        bookAuthor = findViewById(R.id.bookAuthor);
        bookPrice = findViewById(R.id.bookPrice);
        readButton = findViewById(R.id.readButton);
        buyButton = findViewById(R.id.buyButton);
        ratingBar = findViewById(R.id.ratingBar);
        btnFavorite = findViewById(R.id.btnFavorite);
        btnLike = findViewById(R.id.btnLike);
        btnShare = findViewById(R.id.btnShare);
    }

    private void setupListeners() {
        // Back button
        btnBack.setOnClickListener(v -> finish());

        // Read button
        readButton.setOnClickListener(v -> {
            if (bookId != null && !bookId.isEmpty()) {
                // Navigate to read book screen
                Intent intent = new Intent(AddToCartActivity.this, ReadBookActivity.class);
                intent.putExtra("bookId", bookId);
                startActivity(intent);
            } else {
                Toast.makeText(this, "Cannot load book content", Toast.LENGTH_SHORT).show();
            }
        });

        // Buy button
        buyButton.setOnClickListener(v -> {
            // Show animation for adding to cart
            buyButton.setEnabled(false);
            buyButton.setText("Added to cart");

            // Show success message
            Snackbar.make(v, "Book added to cart", Snackbar.LENGTH_LONG)
                    .setAction("VIEW CART", view -> {
                        // Navigate to cart screen (if available)
                        Toast.makeText(AddToCartActivity.this, "Cart feature is under development", Toast.LENGTH_SHORT).show();
                    })
                    .show();

            // Re-enable button after delay
            v.postDelayed(() -> {
                buyButton.setEnabled(true);
                updateBuyButtonText(bookPrice.getText().toString());
            }, 2000);
        });

        // Favorite button
        btnFavorite.setOnClickListener(v -> {
            isFavorite = !isFavorite;
            btnFavorite.setImageResource(R.drawable.sao);

            Toast.makeText(this, isFavorite ?
                            "Added to favorites" : "Removed from favorites",
                    Toast.LENGTH_SHORT).show();
        });

        // Like button
        btnLike.setOnClickListener(v -> {
            isLiked = !isLiked;
            btnLike.setImageResource(R.drawable.yeu);

            Toast.makeText(this, isLiked ?
                            "You liked this book" : "You unliked this book",
                    Toast.LENGTH_SHORT).show();
        });

        // Share button
        btnShare.setOnClickListener(v -> {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Share book from Litera");
            shareIntent.putExtra(Intent.EXTRA_TEXT, "Check out this book on Litera: " +
                    (bookTitle != null ? bookTitle.getText() : "Great book") +
                    " - Download Litera app today!");
            startActivity(Intent.createChooser(shareIntent, "Share via"));
        });
    }

    private void loadBookData(String bookId) {
        // Load book data from ViewModel
        mainViewModel.getBookById(bookId).observe(this, book -> {
            if (book != null) {
                displayBookData(book);
            } else {
                displayDemoBookData();
                Toast.makeText(this, "Unable to load book information", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void displayBookData(Book book) {
        if (book == null) {
            displayDemoBookData();
            return;
        }

        // Set book title
        bookTitle.setText(book.getTitle());

        // Set author name
        String authorId = book.getAuthorId();

        if (authorId != null && !authorId.isEmpty()) {
            // Show "Loading..." while waiting for author information
            bookAuthor.setText("Loading author...");

            // Get author information from MainViewModel
            mainViewModel.getAuthorById(authorId).observe(this, author -> {
                if (author != null) {
                    bookAuthor.setText(author.getName());
                } else {
                    bookAuthor.setText("Unknown author");
                }
            });
        } else {
            bookAuthor.setText("Unknown author");
        }

        // Set price (directly use the price from the book object - already in USD)
        String price = book.getPrice();
        if (price != null && !price.isEmpty()) {
            // Make sure price starts with "$" symbol
            if (!price.startsWith("$")) {
                price = "$" + price;
            }
            bookPrice.setText(price);
        } else {
            bookPrice.setText("$19.99"); // Default price
        }

        // Update buy button text with price
        updateBuyButtonText(bookPrice.getText().toString());

        // Set rating
        String ratingStr = book.getRating();
        if (ratingStr != null && !ratingStr.isEmpty()) {
            try {
                float ratingValue = Float.parseFloat(ratingStr);
                ratingBar.setRating(ratingValue);
            } catch (NumberFormatException e) {
                ratingBar.setRating(4.5f); // Default value
                throw new RuntimeException("displayBookData fail: ", e);
            }
        } else {
            ratingBar.setRating(4.5f); // Default value
        }

        // Load book cover image
        if (book.getImageUrl() != null && !book.getImageUrl().isEmpty()) {
            String directUrl = GoogleDriveUtils.convertToDirect(book.getImageUrl());

            Glide.with(this)
                    .load(directUrl)
                    .placeholder(R.drawable.z6456262903514_8961d85cbd925e7e3f1929bd368cd347)
                    .error(R.drawable.z6456262903514_8961d85cbd925e7e3f1929bd368cd347)
                    .into(bookCover);
        } else {
            // Set default image
            bookCover.setImageResource(R.drawable.z6456262903514_8961d85cbd925e7e3f1929bd368cd347);
        }
    }

    // Helper method to update buy button text with price
    private void updateBuyButtonText(String price) {
        buyButton.setText("Buy physical book - " + price);
    }

    private void displayDemoBookData() {
        bookTitle.setText("Book Title");
        bookAuthor.setText("Author");
        bookPrice.setText("$19.99");
        updateBuyButtonText("$19.99");
        ratingBar.setRating(4.5f);
        bookCover.setImageResource(R.drawable.z6456262903514_8961d85cbd925e7e3f1929bd368cd347);
        readButton.setText("Read");
    }

    private void showLoading(boolean isLoading) {
        // Phương thức giữ lại nhưng không sử dụng
    }
}