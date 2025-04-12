package com.example.litera.views.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
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
import com.example.litera.models.User;
import com.example.litera.repositories.UserRepository;
import com.example.litera.repositories.BookRepository;
import com.example.litera.utils.GoogleDriveUtils;
import com.example.litera.viewmodels.MainViewModel;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.List;

public class AddToCartActivity extends AppCompatActivity {

    // UI elements
    private ImageButton btnBack;
    private ImageView bookCover;
    private TextView bookTitle, bookAuthor, bookPrice;
    private Button readButton, buyButton;
    private RatingBar ratingBar;
    private ImageButton btnFavorite, btnLike, btnShare;
    private MainViewModel mainViewModel;

    // Repository
    private UserRepository userRepository;
    private BookRepository bookRepository;

    // Data
    private String bookId;
    private boolean isFavorite = false;
    private boolean isLiked = false;
    private boolean hasReadBook = false;
    private boolean hasRatedBook = false;
    private int userRating = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_addtocart_book);

        // Get bookId from intent
        bookId = getIntent().getStringExtra("bookId");

        // Initialize ViewModel and repositories
        mainViewModel = new ViewModelProvider(this).get(MainViewModel.class);
        userRepository = new UserRepository();
        bookRepository = BookRepository.getInstance();

        // Initialize views
        initViews();

        // Setup listeners
        setupListeners();

        // Load book data
        if (bookId != null && !bookId.isEmpty()) {
            loadBookData(bookId);
            checkUserInteractions();
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

                // Đánh dấu sách đã được đọc
                markBookAsRead(bookId);
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

        // Rating bar - Chỉ cho phép rating nếu đã đọc sách
        ratingBar.setOnRatingBarChangeListener((ratingBar, rating, fromUser) -> {
            if (!fromUser) return;

            if (FirebaseAuth.getInstance().getCurrentUser() == null) {
                Toast.makeText(this, "Please login to rate this book", Toast.LENGTH_SHORT).show();
                ratingBar.setRating(userRating); // Reset về giá trị cũ
                return;
            }

            if (!hasReadBook) {
                Toast.makeText(this, "You need to read this book before rating", Toast.LENGTH_SHORT).show();
                ratingBar.setRating(userRating); // Reset về giá trị cũ
                return;
            }

            // Lưu đánh giá vào cơ sở dữ liệu
            submitRating(Math.round(rating));
        });

        // Setup listeners for optional buttons if they exist
        if (btnFavorite != null) {
            btnFavorite.setOnClickListener(v -> {
                Toast.makeText(this, "Added to favorites", Toast.LENGTH_SHORT).show();
            });
        }

        if (btnLike != null) {
            btnLike.setOnClickListener(v -> {
                Toast.makeText(this, "You liked this book", Toast.LENGTH_SHORT).show();
            });
        }

        if (btnShare != null) {
            btnShare.setOnClickListener(v -> {
                Toast.makeText(this, "Share feature coming soon", Toast.LENGTH_SHORT).show();
            });
        }
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

    private void checkUserInteractions() {
        // Kiểm tra xem người dùng đã đọc sách này chưa
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            userRepository.checkUserHasReadBook(bookId, hasRead -> {
                hasReadBook = hasRead;

                // Kiểm tra xem người dùng đã đánh giá sách này chưa
                userRepository.checkUserHasRatedBook(bookId, (hasRated, rating) -> {
                    hasRatedBook = hasRated;
                    userRating = rating;

                    // Nếu đã đánh giá, hiển thị đánh giá của người dùng
                    if (hasRated) {
                        ratingBar.setRating(rating);
                    }

                    // Đặt trạng thái cho RatingBar
                    ratingBar.setIsIndicator(!hasReadBook);
                });
            });
        } else {
            // Nếu chưa đăng nhập, không cho phép đánh giá
            ratingBar.setIsIndicator(true);
        }
    }

    private void markBookAsRead(String bookId) {
        // Kiểm tra người dùng đã đăng nhập chưa
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            return;
        }

        // Lấy thông tin người dùng hiện tại
        userRepository.getCurrentUser(new UserRepository.OnUserFetchListener() {
            @Override
            public void onSuccess(User user) {
                // Kiểm tra xem sách đã có trong danh sách continue chưa
                List<String> continueReading = user.getContinueReading();
                if (continueReading == null) {
                    continueReading = new ArrayList<>();
                }

                if (!continueReading.contains(bookId)) {
                    // Thêm sách vào danh sách continue reading
                    continueReading.add(bookId);
                    user.setContinueReading(continueReading);

                    // Cập nhật lên Firestore
                    userRepository.updateUser(user, new UserRepository.OnUserUpdateListener() {
                        @Override
                        public void onSuccess() {
                            hasReadBook = true;
                            ratingBar.setIsIndicator(false); // Cho phép đánh giá
                        }

                        @Override
                        public void onFailure(String error) {
                            Log.e("AddToCartActivity", "Failed to update continue reading: " + error);
                        }
                    });
                }
            }

            @Override
            public void onFailure(String error) {
                Log.e("AddToCartActivity", "Failed to get user: " + error);
            }
        });
    }

    private void submitRating(int rating) {
        if (bookId == null || bookId.isEmpty() || rating < 1 || rating > 5) {
            return;
        }

        // Hiển thị loading
        Toast.makeText(this, "Submitting your rating...", Toast.LENGTH_SHORT).show();

        // Lưu đánh giá vào user
        userRepository.rateBook(bookId, rating, new UserRepository.OnUserUpdateListener() {
            @Override
            public void onSuccess() {
                // Cập nhật trạng thái
                hasRatedBook = true;
                userRating = rating;

                // Cập nhật rating của sách
                if (hasRatedBook && userRating != rating) {
                    // Nếu đã đánh giá trước đó, cập nhật rating
                    bookRepository.updateBookRating(bookId, userRating, rating, FirebaseAuth.getInstance().getCurrentUser().getUid())
                            .thenAccept(success -> {
                                if (success) {
                                    Toast.makeText(AddToCartActivity.this,
                                            "Your rating has been updated", Toast.LENGTH_SHORT).show();

                                    // Xóa cache để đảm bảo làm mới dữ liệu
                                    BookRepository.getInstance().clearCache();

                                    // Làm mới dữ liệu trong MainViewModel
                                    if (mainViewModel != null) {
                                        mainViewModel.refreshBookData(bookId);
                                    }
                                }
                            })
                            .exceptionally(e -> {
                                Toast.makeText(AddToCartActivity.this,
                                        "Failed to update rating: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                return null;
                            });
                } else {
                    // Nếu chưa đánh giá, thêm mới
                    bookRepository.rateBook(bookId, rating, FirebaseAuth.getInstance().getCurrentUser().getUid())
                            .thenAccept(success -> {
                                if (success) {
                                    Toast.makeText(AddToCartActivity.this,
                                            "Thank you for your rating!", Toast.LENGTH_SHORT).show();

                                    // Xóa cache để đảm bảo làm mới dữ liệu
                                    BookRepository.getInstance().clearCache();

                                    // Làm mới dữ liệu trong MainViewModel
                                    if (mainViewModel != null) {
                                        mainViewModel.refreshBookData(bookId);
                                    }
                                }
                            })
                            .exceptionally(e -> {
                                Toast.makeText(AddToCartActivity.this,
                                        "Failed to submit rating: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                return null;
                            });
                }
            }

            @Override
            public void onFailure(String error) {
                Toast.makeText(AddToCartActivity.this,
                        "Failed to save your rating: " + error, Toast.LENGTH_SHORT).show();

                // Reset rating bar to previous value
                ratingBar.setRating(userRating);
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

        // Update buy button text with pricePhysic instead of price
        String pricePhysic = book.getPricePhysic();
        if (pricePhysic != null && !pricePhysic.isEmpty()) {
            // Make sure pricePhysic starts with "$" symbol
            if (!pricePhysic.startsWith("$")) {
                pricePhysic = "$" + pricePhysic;
            }
            updateBuyButtonText(pricePhysic);
        } else {
            // Fallback to default price if pricePhysic is not available
            updateBuyButtonText("$19.99");
        }

        // Set rating
        String ratingStr = book.getRating();
        if (ratingStr != null && !ratingStr.isEmpty()) {
            try {
                float ratingValue = Float.parseFloat(ratingStr);
                // Chỉ cập nhật rating bar nếu người dùng chưa đánh giá
                if (!hasRatedBook) {
                    ratingBar.setRating(ratingValue);
                }
            } catch (NumberFormatException e) {
                if (!hasRatedBook) {
                    ratingBar.setRating(0f); // Chưa có đánh giá
                }
            }
        } else {
            if (!hasRatedBook) {
                ratingBar.setRating(0f); // Chưa có đánh giá
            }
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
        ratingBar.setRating(0f);
        bookCover.setImageResource(R.drawable.z6456262903514_8961d85cbd925e7e3f1929bd368cd347);
        readButton.setText("Read");

        // Đặt RatingBar thành isIndicator vì đây là demo
        ratingBar.setIsIndicator(true);
    }
}