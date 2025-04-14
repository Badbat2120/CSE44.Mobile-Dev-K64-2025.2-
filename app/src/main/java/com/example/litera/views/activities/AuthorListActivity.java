package com.example.litera.views.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.litera.R;
import com.example.litera.models.Author;
import com.example.litera.views.adapters.AuthorAdapter;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class AuthorListActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private AuthorAdapter adapter;
    private ArrayList<Author> authorList;
    private LinearProgressIndicator progressIndicator;
    private View emptyLayout;
    private TextView title;
    private ImageButton backButton; // Nút quay lại

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_author_list);

        // Khởi tạo view
        recyclerView = findViewById(R.id.authorRecyclerView);
        progressIndicator = findViewById(R.id.authorProgressIndicator);
        emptyLayout = findViewById(R.id.emptyAuthorLayout);
        title = findViewById(R.id.authorTitleTextView);
        backButton = findViewById(R.id.backButton);  // Khởi tạo nút quay lại

        // RecyclerView setup với GridLayoutManager (3 tác giả mỗi hàng)
        recyclerView.setLayoutManager(new GridLayoutManager(this, 3));  // Hiển thị 3 tác giả mỗi hàng
        authorList = new ArrayList<>();
        adapter = new AuthorAdapter(this, authorList);
        recyclerView.setAdapter(adapter);

        // Cài đặt sự kiện cho nút quay lại
        backButton.setOnClickListener(v -> onBackPressed());

        // Tải dữ liệu tác giả
        loadAuthors();
    }

    private void loadAuthors() {

        // Lấy dữ liệu từ Firebase
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Authors");
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                progressIndicator.setVisibility(View.GONE);
                authorList.clear();

                for (DataSnapshot ds : snapshot.getChildren()) {
                    Author author = ds.getValue(Author.class);
                    if (author != null) {
                        authorList.add(author);
                    }
                }

                // Kiểm tra nếu không có tác giả nào
                if (authorList.isEmpty()) {
                    emptyLayout.setVisibility(View.VISIBLE);
                    recyclerView.setVisibility(View.GONE);
                } else {
                    emptyLayout.setVisibility(View.GONE);
                    recyclerView.setVisibility(View.VISIBLE);
                    adapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                progressIndicator.setVisibility(View.GONE);
                Toast.makeText(AuthorListActivity.this, "Lỗi khi tải tác giả", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
