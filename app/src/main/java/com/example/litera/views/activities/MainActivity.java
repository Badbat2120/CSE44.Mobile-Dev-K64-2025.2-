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

    private ImageView imgMenu, imgProfile;
    private TextView tvHello;
    private EditText etSearch;
    private RecyclerView rvContinueReading, rvTrendingBooks, rvPopularAuthors;
    private TabLayout tabGenres;
    private MainViewModel mainViewModel;
    private ProgressBar progressBar; // Add this to your layout

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    // Khai báo adapter ở mức class để có thể sử dụng trong các phương thức khác
    private BookAdapter trendingBooksAdapter;
    private BookAdapter continueReadingAdapter;
    private AuthorAdapter popularAuthorAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Initialize UI elements
        imgMenu = findViewById(R.id.imgMenu);
        tvHello = findViewById(R.id.tvHello);
        etSearch = findViewById(R.id.etSearch);
        rvContinueReading = findViewById(R.id.rvContinueReading);
        rvTrendingBooks = findViewById(R.id.rvTrendingBooks);
        rvPopularAuthors = findViewById(R.id.rvPopularAuthors);
        tabGenres = findViewById(R.id.tabGenres);
        TextView tvViewAllAuthors = findViewById(R.id.tvViewAllAuthors);
        TextView tvViewAllContinueReading = findViewById(R.id.tvViewAllContinueReading);
        TextView tvViewAllTrendingBooks = findViewById(R.id.tvViewAllTrendingBooks);

        // Add a progress bar in your layout and reference it here
        // progressBar = findViewById(R.id.progressBar);

        // Hiển thị tên người dùng
        loadUserNameAndDisplay();

        imgMenu.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            startActivity(intent);
            finish(); // Optional: Close MainActivity
        });

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
        setupNavigationListeners(tvViewAllAuthors, tvViewAllContinueReading, tvViewAllTrendingBooks);

        // Observe ViewModel data
        observeViewModel();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Tải lại tên người dùng mỗi khi quay lại MainActivity
        loadUserNameAndDisplay();
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
                                tvHello.setText("Xin chào " + name);
                            } else {
                                // Sử dụng phần đầu của email làm tên mặc định
                                String defaultName = email.split("@")[0];
                                tvHello.setText("Xin chào " + defaultName);
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
        trendingBooksAdapter = new BookAdapter(true, mainViewModel, this); // Thêm mainViewModel và this (LifecycleOwner)
        rvTrendingBooks.setAdapter(trendingBooksAdapter);

        // Setup RecyclerView for Continue Reading
        continueReadingAdapter = new BookAdapter(false, mainViewModel, this); // Thêm mainViewModel và this (LifecycleOwner)
        rvContinueReading.setAdapter(continueReadingAdapter);
        rvContinueReading.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));

        // Setup RecyclerView for Popular Authors
        popularAuthorAdapter = new AuthorAdapter();
        rvPopularAuthors.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvPopularAuthors.setAdapter(popularAuthorAdapter);
    }

    private void setupNavigationListeners(TextView viewAllAuthors, TextView viewAllContinueReading,
                                          TextView viewAllTrendingBooks) {
        // Handle navigation
        viewAllAuthors.setOnClickListener(v -> {
            // Navigate to AuthorListActivity
            Intent intent = new Intent(MainActivity.this, AuthorListActivity.class);
            startActivity(intent);
        });

        viewAllContinueReading.setOnClickListener(v -> {
            // Navigate to BookListActivity
            Intent intent = new Intent(MainActivity.this, BookListActivity.class);
            startActivity(intent);
        });

        viewAllTrendingBooks.setOnClickListener(v -> {
            // Navigate to BookListActivity
            Intent intent = new Intent(MainActivity.this, BookListActivity.class);
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