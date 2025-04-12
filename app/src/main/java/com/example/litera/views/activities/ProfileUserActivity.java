package com.example.litera.views.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.litera.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import de.hdodenhof.circleimageview.CircleImageView;

public class ProfileUserActivity extends AppCompatActivity {
    private static final String TAG = "ProfileUserActivity";

    // UI Components
    private CircleImageView userAvatar;
    private TextView userName;
    private TextView userEmail;
    private TextView userBalance;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_user);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // Initialize UI components
        initViews();

        // Load user data
        loadUserData();

        // Setup click listeners
        setupListeners();
    }

    private void initViews() {
        userAvatar = findViewById(R.id.userAvatar);
        userName = findViewById(R.id.userName);
        userEmail = findViewById(R.id.userEmail);
        userBalance = findViewById(R.id.userBalance);
        // Các button khác
    }

    private void loadUserData() {
        // Hiển thị loading
        Toast.makeText(this, "Loading user data...", Toast.LENGTH_SHORT).show();

        // Lấy user hiện tại
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser != null) {
            // Set email từ Firebase Auth
            String email = currentUser.getEmail();
            userEmail.setText(email);

            // Tìm document chứa email này trong collection users
            db.collection("users")
                    .whereEqualTo("email", email)
                    .get()
                    .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<QuerySnapshot> task) {
                            if (task.isSuccessful()) {
                                for (QueryDocumentSnapshot document : task.getResult()) {
                                    Log.d(TAG, document.getId() + " => " + document.getData());

                                    // Lấy và hiển thị tên
                                    String name = document.getString("name");
                                    if (name != null && !name.isEmpty()) {
                                        userName.setText(name);
                                    }

                                    // Lấy và hiển thị số dư
                                    String value = document.getString("value");
                                    if (value != null && !value.isEmpty()) {
                                        // Thêm dấu $ vào trước giá trị nếu chưa có
                                        if (!value.startsWith("$")) {
                                            value = "$" + value;
                                        }
                                        userBalance.setText(value);
                                    } else {
                                        userBalance.setText("$0.00");
                                    }

                                    // Nếu có field avatar, load avatar
                                    String avatarUrl = document.getString("avatar");
                                    if (avatarUrl != null && !avatarUrl.isEmpty()) {
                                        Glide.with(ProfileUserActivity.this)
                                                .load(avatarUrl)
                                                .placeholder(R.drawable.z6456262903514_8961d85cbd925e7e3f1929bd368cd347)
                                                .error(R.drawable.z6456262903514_8961d85cbd925e7e3f1929bd368cd347)
                                                .into(userAvatar);
                                    }
                                }
                            } else {
                                Log.w(TAG, "Error getting user documents.", task.getException());
                                Toast.makeText(ProfileUserActivity.this, "Failed to load user data", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
        } else {
            // Nếu không có user đăng nhập, hiển thị thông báo và chuyển về màn hình login
            Toast.makeText(this, "Please login to view profile", Toast.LENGTH_SHORT).show();
            // Giả sử có LoginActivity
            // Intent intent = new Intent(ProfileUserActivity.this, LoginActivity.class);
            // startActivity(intent);
            // finish();
        }
    }

    private void setupListeners() {
        ImageButton btnBack = findViewById(R.id.btnBack);
        Button btnFavourites = findViewById(R.id.btnFavourites);
        Button btnChangePass = findViewById(R.id.btnChangePass);
        Button btnTopUpWallet = findViewById(R.id.btnTopUpWallet);
        Button btnLogout = findViewById(R.id.btnLogout);

        // Navigate back to MainActivity
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ProfileUserActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
            }
        });

        // Navigate to FavBookActivity
        btnFavourites.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ProfileUserActivity.this, FavBookActivity.class);
                startActivity(intent);
            }
        });

        // Navigate to ChangePassActivity
        btnChangePass.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ProfileUserActivity.this, ChangePassActivity.class);
                startActivity(intent);
            }
        });

        // Handle Top Up Wallet button click
        btnTopUpWallet.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ProfileUserActivity.this, TopUpWalletActivity.class);
                startActivity(intent);
            }
        });

        // Handle Logout button click - CHỨC NĂNG MỚI
        btnLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Đăng xuất khỏi Firebase Auth
                FirebaseAuth.getInstance().signOut();

                // Hiển thị thông báo
                Toast.makeText(ProfileUserActivity.this, "Logged out successfully", Toast.LENGTH_SHORT).show();

                // Chuyển đến màn hình đăng nhập
                Intent intent = new Intent(ProfileUserActivity.this, LoginActivity.class);
                // Xóa tất cả activity khỏi stack
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }
        });
    }
}