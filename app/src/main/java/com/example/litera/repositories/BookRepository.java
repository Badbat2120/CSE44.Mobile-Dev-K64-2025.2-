package com.example.litera.repositories;

import android.util.Log;

import com.example.litera.models.Author;
import com.example.litera.models.Book;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class BookRepository {

    private static BookRepository instance;
    private final FirebaseFirestore db;

    // Cache for books to avoid frequent Firestore calls
    private List<Book> booksCache = null;

    private BookRepository() {
        db = FirebaseFirestore.getInstance();
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
                        String id = document.getId();
                        String title = document.getString("title");
                        String author = document.getString("author");
                        String description = document.getString("description");
                        String imageUrl = document.getString("cover");

                        books.add(new Book(id, title, author, description, imageUrl));
                    }
                    booksCache = books;
                    future.complete(books);
                })
                .addOnFailureListener(e -> {
                    future.completeExceptionally(e);
                });
        return future;
    }

    public CompletableFuture<List<Book>> getTrendingBooks() {
        CompletableFuture<List<Book>> future = new CompletableFuture<>();

        db.collection("books")
                .whereEqualTo("trending", true) // Assuming you have a "trending" field in Firestore
                .limit(10)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Book> books = new ArrayList<>();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        String id = document.getId();
                        String title = document.getString("title");
                        String author = document.getString("author");
                        String description = document.getString("description");
                        String imageUrl = document.getString("cover");

                        books.add(new Book(id, title, author, description, imageUrl));
                    }
                    future.complete(books);
                })
                .addOnFailureListener(e -> {
                    future.completeExceptionally(e);
                });
        return future;
    }

    public CompletableFuture<List<Book>> getContinueReadingBooks() {
        CompletableFuture<List<Book>> future = new CompletableFuture<>();

        // For now, just return a subset of books
        // Later you can implement user-specific reading progress tracking
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

    public CompletableFuture<List<Author>> getPopularAuthors() {
        CompletableFuture<List<Author>> future = new CompletableFuture<>();

        Log.d("BookRepository", "Fetching popular authors from Firebase...");

        db.collection("authors")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Author> authors = new ArrayList<>();
                    Log.d("BookRepository", "Retrieved " + queryDocumentSnapshots.size() + " author documents");

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        String name = document.getString("name");
                        String imageUrl = document.getString("pfp");

                        Log.d("BookRepository", "Author: " + name + ", URL: " + imageUrl);
                        authors.add(new Author(name, imageUrl));
                    }

                    Log.d("BookRepository", "Returning " + authors.size() + " authors");
                    future.complete(authors);
                })
                .addOnFailureListener(e -> {
                    Log.e("BookRepository", "Error fetching authors", e);
                    future.completeExceptionally(e);
                });
        return future;
    }

    // Method to get a book by ID
    public CompletableFuture<Book> getBookById(String bookId) {
        CompletableFuture<Book> future = new CompletableFuture<>();

        db.collection("books").document(bookId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String id = documentSnapshot.getId();
                        String title = documentSnapshot.getString("title");
                        String author = documentSnapshot.getString("author");
                        String description = documentSnapshot.getString("description");
                        String imageUrl = documentSnapshot.getString("cover");

                        Book book = new Book(id, title, author, description, imageUrl);
                        future.complete(book);
                    } else {
                        future.complete(null);
                    }
                })
                .addOnFailureListener(future::completeExceptionally);

        return future;
    }
}