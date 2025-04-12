package com.example.litera.repositories;

import android.util.Log;

import com.example.litera.models.Author;
import com.example.litera.models.Book;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class BookRepository {

    private static BookRepository instance;
    private static final String TAG = "BookRepository";
    private final AuthorRepository authorRepository;
    private final FirebaseFirestore db;

    // Lưu trữ sách để tránh phải gọi Firestore thường xuyên
    private List<Book> booksCache = null;

    private BookRepository() {
        db = FirebaseFirestore.getInstance();
        this.authorRepository = new AuthorRepository();
    }

    public static synchronized BookRepository getInstance() {
        if (instance == null) {
            instance = new BookRepository();
        }
        return instance;
    }

    public CompletableFuture<List<Book>> getBooks() {
        CompletableFuture<List<Book>> future = new CompletableFuture<>();

        // Return cached books if available
        if (booksCache != null) {
            future.complete(booksCache);
            return future;
        }

        db.collection("books")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Book> books = new ArrayList<>();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Book book = document.toObject(Book.class);
                        book.setId(document.getId());
                        books.add(book);
                    }
                    booksCache = books;
                    future.complete(books);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching books", e);
                    future.completeExceptionally(e);
                });
        return future;
    }

    public CompletableFuture<List<Book>> getTrendingBooks() {
        CompletableFuture<List<Book>> future = new CompletableFuture<>();

        // Sử dụng whereEqualTo để lọc trực tiếp trong Firestore
        db.collection("books")
                .whereEqualTo("trending", true)
                .limit(10)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Book> books = new ArrayList<>();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Book book = document.toObject(Book.class);
                        book.setId(document.getId());
                        books.add(book);
                        Log.d(TAG, "Found trending book: " + book.getTitle());
                    }
                    Log.d(TAG, "Total trending books found: " + books.size());
                    future.complete(books);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching trending books", e);
                    future.completeExceptionally(e);
                });
        return future;
    }

    public CompletableFuture<List<Book>> getContinueReadingBooks() {
        CompletableFuture<List<Book>> future = new CompletableFuture<>();

        // Hiện tại, chỉ cần trả về một tập hợp con sách
        // Sau này, bạn có thể triển khai theo dõi tiến trình đọc của người dùng cụ thể
        getBooks().thenAccept(allBooks -> {
            // Take first 5 books as "continue reading"
            List<Book> continueReading = new ArrayList<>();
            int count = Math.min(5, allBooks.size());
            for (int i = 0; i < count; i++) {
                continueReading.add(allBooks.get(i));
            }
            future.complete(continueReading);
        }).exceptionally(e -> {
            future.completeExceptionally(e);
            return null;
        });

        return future;
    }

    // Phương pháp lấy sách theo ID
    public CompletableFuture<Book> getBookById(String bookId) {
        CompletableFuture<Book> future = new CompletableFuture<>();

        db.collection("books").document(bookId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Book book = documentSnapshot.toObject(Book.class);
                        if (book != null) {
                            book.setId(documentSnapshot.getId());

                            // Lấy thông tin tác giả
                            String authorId = book.getAuthorId();
                            if (authorId != null && !authorId.isEmpty()) {
                                authorRepository.getAuthorById(authorId)
                                        .thenAccept(author -> {
                                            if (author != null) {
                                                book.setAuthor(author);
                                            }
                                            future.complete(book);
                                        })
                                        .exceptionally(e -> {
                                            Log.e(TAG, "Error fetching author for book: " + bookId, e);
                                            // Vẫn trả về book ngay cả khi không lấy được thông tin tác giả
                                            future.complete(book);
                                            return null;
                                        });
                            } else {
                                future.complete(book);
                            }
                        } else {
                            future.complete(null);
                        }
                    } else {
                        future.complete(null);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching book by ID: " + bookId, e);
                    future.completeExceptionally(e);
                });

        return future;
    }

    // Phương pháp lấy tác giả theo ID
    public CompletableFuture<Author> getAuthorById(String authorId) {
        return authorRepository.getAuthorById(authorId);
    }

    // Method to clear cache
    public void clearCache() {
        booksCache = null;
    }
}