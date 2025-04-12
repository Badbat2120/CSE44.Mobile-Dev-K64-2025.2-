package com.example.litera.views.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.litera.R;
import com.example.litera.models.User;
import com.example.litera.views.adapters.AuthorAdapter;
import com.example.litera.views.adapters.BookAdapter;
import com.example.litera.viewmodels.MainViewModel;

import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    private ImageView imgProfile;
    private TextView tvHello;
    private EditText etSearch;
    private RecyclerView rvContinueReading, rvTrendingBooks, rvPopularAuthors, rvAllBooks;
    private TabLayout tabGenres;
    private MainViewModel mainViewModel;
    private ProgressBar progressBar; // Add this to your layout

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    // Khai báo adapter ở mức class để có thể sử dụng trong các phương thức khác
    private BookAdapter trendingBooksAdapter;
    private BookAdapter continueReadingAdapter;
    private BookAdapter allBooksAdapter;
    private AuthorAdapter popularAuthorAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Initialize UI elements
        tvHello = findViewById(R.id.tvHello);
        etSearch = findViewById(R.id.etSearch);
        rvContinueReading = findViewById(R.id.rvContinueReading);
        rvTrendingBooks = findViewById(R.id.rvTrendingBooks);
        rvPopularAuthors = findViewById(R.id.rvPopularAuthors);
        rvAllBooks = findViewById(R.id.rvAllBooks);
        tabGenres = findViewById(R.id.tabGenres);
        TextView tvViewAllAuthors = findViewById(R.id.tvViewAllAuthors);
        TextView tvViewAllContinueReading = findViewById(R.id.tvViewAllContinueReading);
        TextView tvViewAllTrendingBooks = findViewById(R.id.tvViewAllTrendingBooks);
        TextView tvViewAllBooks = findViewById(R.id.tvViewAllBooks);

        // Add a progress bar in your layout and reference it here
        // progressBar = findViewById(R.id.progressBar);

        // Hiển thị tên người dùng
        loadUserNameAndDisplay();

        // Handle profile click
        ImageView imgProfile = findViewById(R.id.imgProfile);
        imgProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, ProfileUserActivity.class);
                startActivity(intent);
            }
        });

        // Initialize ViewModel
        mainViewModel = new ViewModelProvider(this).get(MainViewModel.class);

        // Setup adapters
        setupAdapters();

        // Handle navigation for view all buttons
        setupNavigationListeners(tvViewAllAuthors, tvViewAllContinueReading, tvViewAllTrendingBooks, tvViewAllBooks);

        // Observe ViewModel data
        observeViewModel();
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Tải lại tên người dùng mỗi khi quay lại MainActivity
        loadUserNameAndDisplay();

        // Làm mới dữ liệu sách
        if (mainViewModel != null) {
            // Xóa cache để đảm bảo lấy dữ liệu mới nhất từ Firestore
            mainViewModel.clearCache();

            // Tải lại tất cả dữ liệu
            mainViewModel.refreshData();
        }
    }

    /**
     * Tải tên người dùng từ Firestore và hiển thị lên tvHello
     */
    private void loadUserNameAndDisplay() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String email = currentUser.getEmail();

            // Tìm document chứa email này trong collection users
            db.collection("users")
                    .whereEqualTo("email", email)
                    .limit(1)
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful() && !task.getResult().isEmpty()) {
                            // Lấy document đầu tiên
                            String name = task.getResult().getDocuments().get(0).getString("name");
                            if (name != null && !name.isEmpty()) {
                                tvHello.setText("Hello " + name);
                            } else {
                                // Sử dụng phần đầu của email làm tên mặc định
                                String defaultName = email.split("@")[0];
                                tvHello.setText("Hello " + defaultName);
                            }
                        } else {
                            Log.w(TAG, "Error getting user document", task.getException());
                            // Hiển thị thông báo chào mặc định
                            tvHello.setText("Xin chào người đọc");
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error querying users collection", e);
                        tvHello.setText("Xin chào người đọc");
                    });
        } else {
            // Không có người dùng đăng nhập
            tvHello.setText("Xin chào người đọc");
        }
    }

    private void setupAdapters() {
        // Setup RecyclerView for Trending Books
        trendingBooksAdapter = new BookAdapter(BookAdapter.VIEW_TYPE_TRENDING, mainViewModel, this);
        rvTrendingBooks.setAdapter(trendingBooksAdapter);
        rvTrendingBooks.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));

        // Setup RecyclerView for Continue Reading
        continueReadingAdapter = new BookAdapter(BookAdapter.VIEW_TYPE_CONTINUE_READING, mainViewModel, this);
        rvContinueReading.setAdapter(continueReadingAdapter);
        rvContinueReading.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));

        // Setup RecyclerView for All Books - SETUP MỚI CHO PHẦN ALL BOOKS
        allBooksAdapter = new BookAdapter(BookAdapter.VIEW_TYPE_GRID, mainViewModel, this);
        GridLayoutManager gridLayoutManager = new GridLayoutManager(this, 3);
        rvAllBooks.setLayoutManager(gridLayoutManager);
        rvAllBooks.setAdapter(allBooksAdapter);

        // Setup RecyclerView for Popular Authors
        popularAuthorAdapter = new AuthorAdapter();
        rvPopularAuthors.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvPopularAuthors.setAdapter(popularAuthorAdapter);
    }

    private void setupNavigationListeners(TextView viewAllAuthors, TextView viewAllContinueReading,
                                          TextView viewAllTrendingBooks, TextView viewAllBooks) {
        // Handle navigation
        viewAllAuthors.setOnClickListener(v -> {
            // Navigate to AuthorListActivity
            Intent intent = new Intent(MainActivity.this, AuthorListActivity.class);
            startActivity(intent);
        });

        viewAllContinueReading.setOnClickListener(v -> {
            // Navigate to BookListActivity with continue reading mode
            Intent intent = new Intent(MainActivity.this, BookListActivity.class);
            intent.putExtra(BookListActivity.EXTRA_DISPLAY_MODE, BookListActivity.DISPLAY_CONTINUE_READING);
            startActivity(intent);
        });

        viewAllTrendingBooks.setOnClickListener(v -> {
            // Navigate to BookListActivity with trending books mode
            Intent intent = new Intent(MainActivity.this, BookListActivity.class);
            intent.putExtra(BookListActivity.EXTRA_DISPLAY_MODE, BookListActivity.DISPLAY_TRENDING_BOOKS);
            startActivity(intent);
        });

        viewAllBooks.setOnClickListener(v -> {
            // Navigate to BookListActivity with all books mode
            Intent intent = new Intent(MainActivity.this, BookListActivity.class);
            intent.putExtra(BookListActivity.EXTRA_DISPLAY_MODE, BookListActivity.DISPLAY_ALL_BOOKS);
            startActivity(intent);
        });
    }

    private void observeViewModel() {
        // Lấy dữ liệu từ ViewModel cho Continue Reading và Trending Books
        mainViewModel.getContinueReadingBooks().observe(this, books -> {
            if (books != null && !books.isEmpty()) {
                continueReadingAdapter.submitList(books);
            }
        });

        mainViewModel.getTrendingBooks().observe(this, books -> {
            if (books != null && !books.isEmpty()) {
                trendingBooksAdapter.submitList(books);
            }
        });

        // Observe ALL books - PHẦN MỚI CHO ALL BOOKS
        mainViewModel.getAllBooks().observe(this, books -> {
            if (books != null && !books.isEmpty()) {
                // Giới hạn chỉ hiển thị 6 cuốn sách đầu tiên trong trang chính
                int maxBooks = Math.min(6, books.size());
                allBooksAdapter.submitList(books.subList(0, maxBooks));
            }
        });

        // Observe popular authors
        mainViewModel.getPopularAuthors().observe(this, authors -> {
            if (authors != null) {
                popularAuthorAdapter.submitList(authors);
            }
        });

        // Observe loading state
        mainViewModel.getIsLoading().observe(this, isLoading -> {
            if (progressBar != null) {
                progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            }
        });

        // Observe error messages
        mainViewModel.getErrorMessage().observe(this, error -> {
            if (error != null && !error.isEmpty()) {
                Toast.makeText(MainActivity.this, error, Toast.LENGTH_LONG).show();
            }
        });
    }
}