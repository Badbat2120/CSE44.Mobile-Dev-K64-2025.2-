package com.example.litera.repositories;

import android.util.Log;

import com.example.litera.models.Author;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class AuthorRepository {
    private static final String TAG = "AuthorRepository";
    private final FirebaseFirestore db;
    private static AuthorRepository instance;

    // Cache để tránh gọi Firestore nhiều lần
    private List<Author> authorsCache = null;

    // Constructor đơn giản chỉ khởi tạo Firebase
    public AuthorRepository() {
        db = FirebaseFirestore.getInstance();
    }

    // Singleton pattern giống như BookRepository
    public static synchronized AuthorRepository getInstance() {
        if (instance == null) {
            instance = new AuthorRepository();
        }
        return instance;
    }

    // Phương thức lấy tất cả tác giả
    public CompletableFuture<List<Author>> getAuthors() {
        CompletableFuture<List<Author>> future = new CompletableFuture<>();

        // Sử dụng cache nếu có
        if (authorsCache != null) {
            future.complete(authorsCache);
            return future;
        }

        db.collection("authors")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Author> authorList = new ArrayList<>();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        // Sử dụng toObject để chuyển đổi dữ liệu từ Firestore sang object
                        Author author = document.toObject(Author.class);
                        author.setId(document.getId());

                        // Thêm vào danh sách
                        authorList.add(author);
                    }
                    // Lưu vào cache
                    authorsCache = authorList;
                    future.complete(authorList);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching authors", e);
                    future.completeExceptionally(e);
                });

        return future;
    }

    // Phương thức lấy tác giả theo ID
    public CompletableFuture<Author> getAuthorById(String authorId) {
        CompletableFuture<Author> future = new CompletableFuture<>();

        // Kiểm tra cache trước
        if (authorsCache != null) {
            for (Author author : authorsCache) {
                if (author.getId() != null && author.getId().equals(authorId)) {
                    future.complete(author);
                    return future;
                }
            }
        }

        // Nếu không có trong cache thì gọi Firestore
        db.collection("authors").document(authorId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Author author = documentSnapshot.toObject(Author.class);
                        if (author != null) {
                            author.setId(documentSnapshot.getId());
                        }
                        future.complete(author);
                    } else {
                        future.complete(null);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching author by ID: " + authorId, e);
                    future.completeExceptionally(e);
                });

        return future;
    }

    // Phương thức lấy các tác giả nổi tiếng
    public CompletableFuture<List<Author>> getPopularAuthors() {
        CompletableFuture<List<Author>> future = new CompletableFuture<>();

        Log.d(TAG, "Fetching popular authors from Firebase...");

        db.collection("authors")
                .limit(10) // Giới hạn 10 tác giả
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Author> authors = new ArrayList<>();
                    Log.d(TAG, "Retrieved " + queryDocumentSnapshots.size() + " author documents");

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Author author = document.toObject(Author.class);
                        author.setId(document.getId());
                        authors.add(author);

                        Log.d(TAG, "Author: " + author.getName() + ", ID: " + author.getId());
                    }

                    Log.d(TAG, "Returning " + authors.size() + " authors");
                    future.complete(authors);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching authors", e);
                    future.completeExceptionally(e);
                });
        return future;
    }

    // Phương thức xóa cache
    public void clearCache() {
        authorsCache = null;
    }
}