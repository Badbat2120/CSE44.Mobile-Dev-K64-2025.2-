package com.example.litera.views.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.example.litera.R;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;

public class MainActivity extends AppCompatActivity {

    private ImageView imgMenu, imgProfile;
    private TextView tvHello;
    private EditText etSearch;
    private RecyclerView rvContinueReading, rvTrendingBooks;
    private TabLayout tabGenres;

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
        tabGenres = findViewById(R.id.tabGenres);

        // Xử lý sự kiện đăng xuất khi bấm vào avatar
        imgProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FirebaseAuth.getInstance().signOut();
                startActivity(new Intent(MainActivity.this, LoginActivity.class));
                finish();
            }
        });

        // Thêm các xử lý khác nếu cần như:
        // - Load dữ liệu cho RecyclerView
        // - Thiết lập tab cho TabLayout
        // - Xử lý tìm kiếm,...
    }
}
