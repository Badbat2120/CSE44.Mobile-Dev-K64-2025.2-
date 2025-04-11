package com.example.litera.views.activities;

import android.content.Intent;
import android.os.Bundle;
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
import com.example.litera.views.adapters.AuthorAdapter;
import com.example.litera.views.adapters.BookAdapter;
import com.example.litera.viewmodels.MainViewModel;

import com.google.android.material.tabs.TabLayout;

public class MainActivity extends AppCompatActivity {
    private ImageView imgMenu, imgProfile;
    private TextView tvHello;
    private EditText etSearch;
    private RecyclerView rvContinueReading, rvTrendingBooks, rvPopularAuthors;
    private TabLayout tabGenres;
    private MainViewModel mainViewModel;
    private ProgressBar progressBar; // Add this to your layout

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize UI elements
        imgMenu = findViewById(R.id.imgMenu);
        imgProfile = findViewById(R.id.imgProfile);
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

        tvViewAllTrendingBooks.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Navigate to AddToCartActivity
                Intent intent = new Intent(MainActivity.this, AddToCartActivity.class);
                startActivity(intent);
            }
        });

        imgMenu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                startActivity(intent);
                finish(); // Optional: Close MainActivity
            }
        });

        // Handle profile click
        imgProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Navigate to ProfileUserActivity
                Intent intent = new Intent(MainActivity.this, ProfileUserActivity.class);
                startActivity(intent);
            }
        });

        // Initialize ViewModel
        mainViewModel = new ViewModelProvider(this).get(MainViewModel.class);

        // Setup RecyclerView for Trending Books
        rvTrendingBooks.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        BookAdapter trendingBooksAdapter = new BookAdapter();
        rvTrendingBooks.setAdapter(trendingBooksAdapter);

        // Setup RecyclerView for Continue Reading
        rvContinueReading.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        BookAdapter continueReadingAdapter = new BookAdapter();
        rvContinueReading.setAdapter(continueReadingAdapter);

        // Setup RecyclerView for Popular Authors
        rvPopularAuthors.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        AuthorAdapter popularAuthorAdapter = new AuthorAdapter();
        rvPopularAuthors.setAdapter(popularAuthorAdapter);

        // Handle navigation
        tvViewAllAuthors.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Navigate to AuthorListActivity
                Intent intent = new Intent(MainActivity.this, AuthorListActivity.class);
                startActivity(intent);
            }
        });

        tvViewAllContinueReading.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Navigate to BookListActivity
                Intent intent = new Intent(MainActivity.this, BookListActivity.class);
                startActivity(intent);
            }
        });

        tvViewAllTrendingBooks.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Navigate to BookListActivity
                Intent intent = new Intent(MainActivity.this, BookListActivity.class);
                startActivity(intent);
            }
        });

        // Observe ViewModel data
        observeViewModel(trendingBooksAdapter, continueReadingAdapter, popularAuthorAdapter);
    }

    private void observeViewModel(BookAdapter trendingAdapter, BookAdapter continueReadingAdapter, AuthorAdapter authorAdapter) {
        // Observe trending books
        mainViewModel.getTrendingBooks().observe(this, trendingAdapter::submitList);

        // Observe continue reading books
        mainViewModel.getContinueReadingBooks().observe(this, continueReadingAdapter::submitList);

        
        // Observe popular authors
        mainViewModel.getPopularAuthors().observe(this, authorAdapter::submitList);

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