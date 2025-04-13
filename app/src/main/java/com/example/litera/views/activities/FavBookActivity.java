package com.example.litera.views.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.litera.R;
import com.example.litera.models.Book;
import com.example.litera.repositories.BookRepository;
import com.example.litera.views.adapters.BookAdapter;
import com.example.litera.viewmodels.MainViewModel;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.List;

public class FavBookActivity extends AppCompatActivity {
    private static final String TAG = "FavBookActivity";
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private TextView emptyView;
    private BookAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fav_book);

        // Initialize views
        recyclerView = findViewById(R.id.recyclerViewFavBooks);
        progressBar = findViewById(R.id.progressBar);
        emptyView = findViewById(R.id.emptyView);

        if (progressBar == null) {
            // Nếu không tìm thấy trong layout, tạo mới
            progressBar = new ProgressBar(this);
            progressBar.setVisibility(View.GONE);
        }

        if (emptyView == null) {
            // Nếu không tìm thấy trong layout, tạo mới
            emptyView = new TextView(this);
            emptyView.setText("Bạn chưa có sách yêu thích nào");
            emptyView.setVisibility(View.GONE);
        }

        // Initialize the back button
        ImageButton backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> onBackPressed());

        // Set up RecyclerView with GridLayoutManager
        GridLayoutManager layoutManager = new GridLayoutManager(this, 2);
        recyclerView.setLayoutManager(layoutManager);

        // Khởi tạo adapter với VIEW_TYPE_GRID
        MainViewModel viewModel = new ViewModelProvider(this).get(MainViewModel.class);
        adapter = new BookAdapter(BookAdapter.VIEW_TYPE_GRID, viewModel, this);
        recyclerView.setAdapter(adapter);

        // Load danh sách sách yêu thích
        loadFavoriteBooks();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Tải lại danh sách khi activity được resume
        loadFavoriteBooks();
    }

    private void loadFavoriteBooks() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Vui lòng đăng nhập để xem sách yêu thích", Toast.LENGTH_SHORT).show();
            if (emptyView != null) {
                emptyView.setVisibility(View.VISIBLE);
            }
            return;
        }

        if (progressBar != null) {
            progressBar.setVisibility(View.VISIBLE);
        }

        if (emptyView != null) {
            emptyView.setVisibility(View.GONE);
        }

        BookRepository.getInstance().getFavoriteBooks(currentUser.getUid())
                .thenAccept(books -> {
                    runOnUiThread(() -> {
                        adapter.submitList(books);

                        if (progressBar != null) {
                            progressBar.setVisibility(View.GONE);
                        }

                        if (emptyView != null) {
                            emptyView.setVisibility(books.isEmpty() ? View.VISIBLE : View.GONE);
                        }
                    });
                })
                .exceptionally(e -> {
                    Log.e(TAG, "Error loading favorite books", e);
                    runOnUiThread(() -> {
                        Toast.makeText(FavBookActivity.this, "Lỗi khi tải sách yêu thích: " + e.getMessage(), Toast.LENGTH_SHORT).show();

                        if (progressBar != null) {
                            progressBar.setVisibility(View.GONE);
                        }

                        if (emptyView != null) {
                            emptyView.setVisibility(View.VISIBLE);
                        }
                    });
                    return null;
                });
    }
}