package com.example.litera.repositories;

import android.util.Log;

import com.example.litera.models.Author;
import com.example.litera.models.Book;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class BookRepository {

    private static BookRepository instance;
    private static final String TAG = "BookRepository";
    private final AuthorRepository authorRepository;
    private final FirebaseFirestore db;

    // Cache for books to avoid frequent Firestore calls
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

    // Method to get a book by ID
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

    // Method to get author by ID
    public CompletableFuture<Author> getAuthorById(String authorId) {
        return authorRepository.getAuthorById(authorId);
    }

    // Method to clear cache
    public void clearCache() {
        booksCache = null;
    }

    public CompletableFuture<Boolean> rateBook(String bookId, int rating, String userId) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();

        // First check if user has already rated this book
        db.collection("users").document(userId)
                .get()
                .addOnSuccessListener(userDoc -> {
                    if (userDoc.exists()) {
                        // Get user's current ratings map
                        Map<String, Object> userRatings = (Map<String, Object>) userDoc.get("ratings");
                        boolean hasRatedBefore = userRatings != null && userRatings.containsKey(bookId);

                        // Now get the book and update its rating
                        getBookById(bookId).thenAccept(book -> {
                            if (book == null) {
                                future.completeExceptionally(new Exception("Book not found"));
                                return;
                            }

                            // Get the most up-to-date rating information directly from Firestore
                            db.collection("books").document(bookId)
                                    .get()
                                    .addOnSuccessListener(documentSnapshot -> {
                                        if (documentSnapshot.exists()) {
                                            // Get current rating count
                                            Object ratingCountObj = documentSnapshot.get("ratingCount");
                                            final int[] currentRatingCount = {0};

                                            if (ratingCountObj instanceof Long) {
                                                currentRatingCount[0] = ((Long) ratingCountObj).intValue();
                                            } else if (ratingCountObj instanceof Integer) {
                                                currentRatingCount[0] = (Integer) ratingCountObj;
                                            } else if (ratingCountObj instanceof Double) {
                                                currentRatingCount[0] = ((Double) ratingCountObj).intValue();
                                            } else if (ratingCountObj instanceof String) {
                                                try {
                                                    currentRatingCount[0] = Integer.parseInt((String) ratingCountObj);
                                                } catch (Exception e) {
                                                    Log.e(TAG, "Error parsing ratingCount", e);
                                                    currentRatingCount[0] = 0;
                                                }
                                            }

                                            Log.d(TAG, "Current ratingCount: " + currentRatingCount[0]);

                                            // Get current average rating
                                            String ratingStr = documentSnapshot.getString("rating");
                                            double currentAverage = 0.0;
                                            try {
                                                currentAverage = (ratingStr != null && !ratingStr.isEmpty()) ?
                                                        Double.parseDouble(ratingStr) : 0.0;
                                            } catch (NumberFormatException e) {
                                                Log.e(TAG, "Error parsing rating", e);
                                                currentAverage = 0.0;
                                            }

                                            // Calculate new rating values
                                            long newCount;
                                            double newAverage;

                                            if (hasRatedBefore) {
                                                // User has rated before - get old rating
                                                int oldRating = 0;
                                                if (userRatings != null) {
                                                    Object oldRatingObj = userRatings.get(bookId);
                                                    if (oldRatingObj instanceof Long) {
                                                        oldRating = ((Long) oldRatingObj).intValue();
                                                    } else if (oldRatingObj instanceof Integer) {
                                                        oldRating = (Integer) oldRatingObj;
                                                    } else if (oldRatingObj instanceof Double) {
                                                        oldRating = ((Double) oldRatingObj).intValue();
                                                    }
                                                }

                                                // Calculate new average by replacing old rating
                                                newCount = currentRatingCount[0]; // Count stays the same
                                                newAverage = currentRatingCount[0] > 0 ?
                                                        ((currentAverage * currentRatingCount[0]) - oldRating + rating) / currentRatingCount[0] :
                                                        rating;
                                            } else {
                                                // First time rating - increment count
                                                newCount = currentRatingCount[0] + 1;
                                                newAverage = ((currentAverage * currentRatingCount[0]) + rating) / newCount;
                                            }

                                            String newRating = String.format(Locale.US, "%.1f", newAverage);
                                            String newAverageStr = String.format(Locale.US, "%.1f", newAverage);

                                            Log.d(TAG, "New ratingCount: " + newCount + ", new rating: " + newRating);

                                            // Update book rating in Firestore
                                            Map<String, Object> updates = new HashMap<>();
                                            updates.put("rating", newRating);
                                            updates.put("ratingCount", newCount);
                                            updates.put("ratingAverage", newAverageStr);

                                            db.collection("books").document(bookId)
                                                    .update(updates)
                                                    .addOnSuccessListener(aVoid -> {
                                                        Log.d(TAG, "Rating updated successfully");
                                                        clearCache(); // Clear cache after update
                                                        future.complete(true);
                                                    })
                                                    .addOnFailureListener(e -> {
                                                        Log.e(TAG, "Error updating rating", e);
                                                        future.completeExceptionally(e);
                                                    });
                                        } else {
                                            future.completeExceptionally(new Exception("Book document does not exist"));
                                        }
                                    })
                                    .addOnFailureListener(e -> future.completeExceptionally(e));
                        }).exceptionally(e -> {
                            future.completeExceptionally(e);
                            return null;
                        });
                    } else {
                        future.completeExceptionally(new Exception("User document not found"));
                    }
                })
                .addOnFailureListener(e -> future.completeExceptionally(e));

        return future;
    }

    public CompletableFuture<Boolean> updateBookRating(String bookId, int oldRating, int newRating) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();

        // Lấy thông tin sách hiện tại
        getBookById(bookId).thenAccept(book -> {
            if (book == null) {
                future.completeExceptionally(new Exception("Book not found"));
                return;
            }

            // Lấy thông tin rating hiện tại
            int currentCount = book.getRatingCountAsInt();
            double currentAverage = 0;
            try {
                currentAverage = Double.parseDouble(book.getRating());
            } catch (NumberFormatException | NullPointerException e) {
                currentAverage = 0;
            }

            // Tính toán trung bình mới khi thay thế rating cũ bằng rating mới
            double newAverage = currentCount > 0 ?
                    ((currentAverage * currentCount) - oldRating + newRating) / currentCount :
                    newRating;

            String updatedRating = String.format(Locale.US, "%.1f", newAverage);
            String newAverageStr = String.format(Locale.US, "%.1f", newAverage); // Định dạng thành chuỗi

            // Cập nhật trong Firestore
            db.collection("books").document(bookId)
                    .update(
                            "rating", updatedRating,
                            "ratingAverage", newAverageStr  // Lưu dưới dạng String
                    )
                    .addOnSuccessListener(aVoid -> future.complete(true))
                    .addOnFailureListener(e -> future.completeExceptionally(e));
        }).exceptionally(e -> {
            future.completeExceptionally(e);
            return null;
        });

        return future;
    }

    // Phương thức để kiểm tra và cập nhật rating trong trường hợp người dùng đã đánh giá trước đó
    public CompletableFuture<Boolean> updateBookRating(String bookId, int oldRating, int newRating, String userId) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();

        // Lấy thông tin sách hiện tại
        getBookById(bookId).thenAccept(book -> {
            if (book == null) {
                future.completeExceptionally(new Exception("Book not found"));
                return;
            }

            // Lấy thông tin rating hiện tại
            int currentCount = book.getRatingCountAsInt();  // Sửa dòng này, dùng getRatingCountAsInt()
            double currentAverage = 0;
            try {
                currentAverage = Double.parseDouble(book.getRating());
            } catch (NumberFormatException | NullPointerException e) {
                currentAverage = 0;
            }

            // Tính toán trung bình mới khi thay thế rating cũ bằng rating mới
            double newAverage = currentCount > 0 ?
                    ((currentAverage * currentCount) - oldRating + newRating) / currentCount :
                    newRating;

            String updatedRating = String.format(Locale.US, "%.1f", newAverage);
            String newAverageStr = String.format(Locale.US, "%.1f", newAverage);

            // Cập nhật trong Firestore
            db.collection("books").document(bookId)
                    .update(
                            "rating", updatedRating,
                            "ratingAverage", newAverageStr
                    )
                    .addOnSuccessListener(aVoid -> future.complete(true))
                    .addOnFailureListener(e -> future.completeExceptionally(e));
        }).exceptionally(e -> {
            future.completeExceptionally(e);
            return null;
        });

        return future;
    }
}