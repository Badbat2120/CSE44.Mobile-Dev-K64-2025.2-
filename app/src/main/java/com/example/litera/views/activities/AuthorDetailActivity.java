package com.example.litera.views.activities;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.litera.R;
import com.example.litera.models.Author;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class AuthorDetailActivity extends AppCompatActivity {

    private ImageView imgAuthor;
    private TextView tvAuthorName, tvAuthorEmail, tvAuthorDescription;
    private ImageButton btnBack;

    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_author_detail);

        // Khởi tạo Firestore
        db = FirebaseFirestore.getInstance();

        // Ánh xạ các thành phần giao diện
        initViews();

        // Nhận thông tin tác giả từ Intent
        String authorId = getIntent().getStringExtra("author_id");
        Log.d("AuthorDetail", "Received author ID: " + authorId);

        if (authorId != null && !authorId.isEmpty()) {
            loadAuthorDetails(authorId);
        } else {
            Toast.makeText(this, "Không thể tải thông tin tác giả", Toast.LENGTH_SHORT).show();
            Log.e("AuthorDetail", "Author ID is null or empty");
            finish();
        }

        // Xử lý sự kiện nút quay lại
        btnBack.setOnClickListener(v -> finish());
    }

    private void initViews() {
        imgAuthor = findViewById(R.id.img_author);
        tvAuthorName = findViewById(R.id.tv_author_name);
        tvAuthorEmail = findViewById(R.id.tv_author_email);
        tvAuthorDescription = findViewById(R.id.tv_author_description);
        btnBack = findViewById(R.id.btn_back);
    }

    private void loadAuthorDetails(String authorId) {
        DocumentReference docRef = db.collection("authors").document(authorId);
        docRef.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                if (documentSnapshot.exists()) {
                    Author author = documentSnapshot.toObject(Author.class);
                    if (author != null) {
                        displayAuthorDetails(author);
                    }
                } else {
                    Toast.makeText(AuthorDetailActivity.this, "Không tìm thấy thông tin tác giả", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(AuthorDetailActivity.this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private void displayAuthorDetails(Author author) {
        // Hiển thị tên tác giả
        tvAuthorName.setText(author.getName());

        // Hiển thị email tác giả
        if (author.getEmail() != null && !author.getEmail().isEmpty()) {
            tvAuthorEmail.setText(author.getEmail());
        } else {
            tvAuthorEmail.setText("Không có thông tin");
        }

        // Hiển thị mô tả tác giả
        if (author.getDescription() != null && !author.getDescription().isEmpty()) {
            tvAuthorDescription.setText(author.getDescription());
        } else {
            tvAuthorDescription.setText("Không có mô tả cho tác giả này");
        }

        // Hiển thị ảnh tác giả
        if (author.getImageUrl() != null && !author.getImageUrl().isEmpty()) {
            Glide.with(this)
                    .load(author.getImageUrl())
                    .placeholder(R.drawable.error)
                    .error(R.drawable.error)
                    .into(imgAuthor);
        } else {
            imgAuthor.setImageResource(R.drawable.error);
        }
    }
}