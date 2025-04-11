package com.example.litera.views.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.litera.R;
import com.example.litera.views.adapters.AuthorAdapter;
import com.example.litera.views.adapters.BookAdapter;
import com.example.litera.viewmodels.MainViewModel;

import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class MainActivity extends AppCompatActivity {
    private FirebaseFirestore db;
    private ImageView imgMenu, imgProfile;
    private TextView tvHello;
    private EditText etSearch;
    private RecyclerView rvContinueReading, rvTrendingBooks, rvPopularAuthors;
    private TabLayout tabGenres;
    private MainViewModel mainViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Ánh xạ View
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

        tvViewAllTrendingBooks.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Navigate to AddToCartActivity
                Intent intent = new Intent(MainActivity.this, AddToCartActivity.class);
                startActivity(intent);
            }
        });

        // Xử lý sự kiện đăng xuất khi bấm vào avatar
        imgProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Chuyển đến activity_profile_user
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

        // Observe LiveData for Trending Books
        // Update Adapter
        mainViewModel.getTrendingBooks().observe(this, trendingBooksAdapter::submitList);

        // Setup RecyclerView for Continue Reading
        rvContinueReading.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        BookAdapter continueReadingAdapter = new BookAdapter();
        rvContinueReading.setAdapter(continueReadingAdapter);

        // Observe LiveData for Continue Reading
        // Update Adapter
        mainViewModel.getContinueReadingBooks().observe(this, continueReadingAdapter::submitList);

        // Setup RecyclerView for Popular Authors
        rvPopularAuthors.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        AuthorAdapter popularAuthorAdapter = new AuthorAdapter();
        rvPopularAuthors.setAdapter(popularAuthorAdapter);

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
                // Navigate to AddToCartActivity
                Intent intent = new Intent(MainActivity.this, BookListActivity.class);
                startActivity(intent);
            }
        });

        tvViewAllTrendingBooks.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Navigate to AddToCartActivity
                Intent intent = new Intent(MainActivity.this, BookListActivity.class);
                startActivity(intent);
            }
        });

        // Observe LiveData for Popular Authors
        mainViewModel.getPopularAuthors().observe(this, popularAuthorAdapter::submitList);


        // Thêm các xử lý khác nếu cần như:
        // - Thiết lập tab cho TabLayout
        // - Xử lý tìm kiếm,...
    }
}