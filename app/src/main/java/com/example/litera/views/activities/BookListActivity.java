package com.example.litera.views.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.litera.R;
import com.example.litera.models.Book;
import com.example.litera.viewmodels.MainViewModel;
import com.example.litera.views.adapters.BookAdapter;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.util.ArrayList;
import java.util.List;

public class BookListActivity extends AppCompatActivity {

    private static final String TAG = "BookListActivity";
    private RecyclerView recyclerView;
    private BookAdapter bookAdapter;
    private MainViewModel viewModel;
    private TextView titleTextView;
    private LinearProgressIndicator progressIndicator;
    private View emptyStateLayout;

    // Constants for display modes
    public static final String EXTRA_DISPLAY_MODE = "display_mode";
    public static final String DISPLAY_ALL_BOOKS = "all_books";
    public static final String DISPLAY_TRENDING_BOOKS = "trending_books";
    public static final String DISPLAY_CONTINUE_READING = "continue_reading";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_book_list);

        // Initialize UI components
        titleTextView = findViewById(R.id.titleTextView);
        recyclerView = findViewById(R.id.recyclerView);
        progressIndicator = findViewById(R.id.progressIndicator);
        emptyStateLayout = findViewById(R.id.emptyStateLayout);
        ImageButton backButton = findViewById(R.id.backButton);

        // Setup back button
        backButton.setOnClickListener(v -> finish());

        // Initialize ViewModel
        viewModel = new ViewModelProvider(this).get(MainViewModel.class);

        // Setup RecyclerView
        setupRecyclerView();

        // Determine which books to display based on intent extras
        String displayMode = getIntent().getStringExtra(EXTRA_DISPLAY_MODE);
        if (displayMode == null) {
            displayMode = DISPLAY_ALL_BOOKS; // Default to showing all books
        }

        loadBooks(displayMode);
    }

    private void setupRecyclerView() {
        // Use a grid layout with 3 items per row
        GridLayoutManager layoutManager = new GridLayoutManager(this, 3);
        recyclerView.setLayoutManager(layoutManager);

        // Initialize adapter with GRID view type
        bookAdapter = new BookAdapter(BookAdapter.VIEW_TYPE_GRID, viewModel, this);
        recyclerView.setAdapter(bookAdapter);
    }

    private void loadBooks(String displayMode) {
        progressIndicator.setVisibility(View.VISIBLE);

        switch (displayMode) {
            case DISPLAY_TRENDING_BOOKS:
                titleTextView.setText(R.string.trending_books);
                observeTrendingBooks();
                break;
            case DISPLAY_CONTINUE_READING:
                titleTextView.setText(R.string.continue_reading);
                observeContinueReadingBooks();
                break;
            case DISPLAY_ALL_BOOKS:
            default:
                titleTextView.setText(R.string.all_books);
                observeAllBooks();
                break;
        }

        // Observe loading state
        viewModel.getIsLoading().observe(this, isLoading -> {
            progressIndicator.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        });

        // Observe errors
        viewModel.getErrorMessage().observe(this, error -> {
            if (error != null && !error.isEmpty()) {
                Toast.makeText(this, error, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void observeAllBooks() {
        viewModel.getAllBooks().observe(this, books -> {
            if (books != null) {
                displayBooks(books);
            }
        });
    }

    private void observeTrendingBooks() {
        viewModel.getTrendingBooks().observe(this, books -> {
            if (books != null) {
                displayBooks(books);
            }
        });
    }

    private void observeContinueReadingBooks() {
        viewModel.getContinueReadingBooks().observe(this, books -> {
            if (books != null) {
                displayBooks(books);
            }
        });
    }

    private void displayBooks(List<Book> books) {
        progressIndicator.setVisibility(View.GONE);
        if (books.isEmpty()) {
            // Show empty state if no books
            emptyStateLayout.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            emptyStateLayout.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
            bookAdapter.submitList(books);
        }
    }
}