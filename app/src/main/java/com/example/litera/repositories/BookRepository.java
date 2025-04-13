package com.example.litera.repositories;

import android.util.Log;

import com.example.litera.models.Author;
import com.example.litera.models.Book;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
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

        // Tham chiếu đến các tài liệu
        DocumentReference bookRef = db.collection("books").document(bookId);
        DocumentReference userRef = db.collection("users").document(userId);

        // Sử dụng transaction
        db.runTransaction(transaction -> {
            // Lấy dữ liệu
            DocumentSnapshot bookDoc = transaction.get(bookRef);
            DocumentSnapshot userDoc = transaction.get(userRef);

            if (!bookDoc.exists()) {
                throw new FirebaseFirestoreException("Book not found",
                        FirebaseFirestoreException.Code.NOT_FOUND);
            }

            // === THÊM LOG ĐỂ KIỂM TRA TOÀN BỘ DỮ LIỆU SÁCH ===
            Log.d(TAG, "Book data: " + bookDoc.getData());

            // Xử lý đặc biệt cho ratingCount (giờ là String)
            String ratingCountStr = bookDoc.getString("ratingCount");
            int currentCount = 0;

            if (ratingCountStr != null && !ratingCountStr.isEmpty()) {
                try {
                    currentCount = Integer.parseInt(ratingCountStr);
                } catch (NumberFormatException e) {
                    Log.e(TAG, "Error parsing ratingCount", e);
                }
            }

            Log.d(TAG, "Current rating count (after parsing): " + currentCount);

            // === PHẦN LẤY RATING HIỆN TẠI, SỬA LỖI Ở ĐÂY ===
            String ratingStr = bookDoc.getString("rating");
            double currentAverage = 0.0;

            // Thêm log để kiểm tra giá trị ratingStr
            Log.d(TAG, "Raw rating string from Firestore: " + ratingStr);

            if (ratingStr != null && !ratingStr.isEmpty()) {
                try {
                    currentAverage = Double.parseDouble(ratingStr);
                    // Thêm log để theo dõi việc chuyển đổi
                    Log.d(TAG, "Successfully parsed current rating: " + currentAverage);
                } catch (NumberFormatException e) {
                    Log.e(TAG, "Error parsing rating", e);
                    currentAverage = 0.0;
                }
            } else {
                Log.d(TAG, "Rating string is null or empty, defaulting to 0.0");
            }

            // Kiểm tra người dùng đã đánh giá chưa
            Map<String, Object> userRatings = userDoc.exists() ?
                    (Map<String, Object>) userDoc.get("ratings") : null;
            boolean hasRatedBefore = userRatings != null && userRatings.containsKey(bookId);

            Log.d(TAG, "User has rated this book before: " + hasRatedBefore);

            // Tính toán giá trị mới
            int newCount;
            double newAverage;

            if (hasRatedBefore) {
                // Lấy đánh giá cũ
                Object oldRatingObj = userRatings.get(bookId);
                int oldRating = 0;
                if (oldRatingObj instanceof Long) {
                    oldRating = ((Long) oldRatingObj).intValue();
                } else if (oldRatingObj instanceof Integer) {
                    oldRating = (Integer) oldRatingObj;
                } else if (oldRatingObj instanceof Double) {
                    oldRating = ((Double) oldRatingObj).intValue();
                } else if (oldRatingObj instanceof String) {
                    try {
                        oldRating = Integer.parseInt((String) oldRatingObj);
                    } catch (NumberFormatException e) {
                        Log.e(TAG, "Error parsing old rating", e);
                    }
                }

                // QUAN TRỌNG: Xử lý trường hợp không nhất quán
                if (currentCount == 0) {
                    newCount = 1; // Phải có ít nhất 1 đánh giá
                    Log.d(TAG, "FIXING INCONSISTENCY: User has rated but count is 0, setting to 1");
                } else {
                    newCount = currentCount; // Giữ nguyên count
                }

                // === SỬA LỖI TÍNH TOÁN TRUNG BÌNH ===
                Log.d(TAG, "Rating calculation inputs: currentAverage=" + currentAverage +
                        ", currentCount=" + currentCount + ", oldRating=" + oldRating +
                        ", newRating=" + rating);

                if (newCount > 0) {
                    // Công thức tính trung bình mới khi thay đổi đánh giá
                    // ((tổng điểm hiện tại) - điểm cũ + điểm mới) / số lượng
                    double totalPoints = currentAverage * currentCount;
                    double newTotalPoints = totalPoints - oldRating + rating;
                    newAverage = newTotalPoints / newCount;

                    Log.d(TAG, "Total points: " + totalPoints);
                    Log.d(TAG, "New total points: " + newTotalPoints);
                    Log.d(TAG, "New average calculation: " + newTotalPoints + " / " + newCount + " = " + newAverage);
                } else {
                    newAverage = rating;
                    Log.d(TAG, "Setting new average directly to rating: " + rating);
                }

                Log.d(TAG, "Updating rating from " + oldRating + " to " + rating);
            } else {
                // Lần đánh giá đầu tiên
                newCount = currentCount + 1;

                // === SỬA LỖI TÍNH TOÁN TRUNG BÌNH KHI ĐÁNH GIÁ LẦN ĐẦU ===
                Log.d(TAG, "First rating calculation inputs: currentAverage=" + currentAverage +
                        ", currentCount=" + currentCount + ", rating=" + rating);

                if (currentCount > 0) {
                    // (tổng điểm hiện tại + điểm mới) / số lượng mới
                    double totalPoints = currentAverage * currentCount;
                    double newTotalPoints = totalPoints + rating;
                    newAverage = newTotalPoints / newCount;

                    Log.d(TAG, "Total points: " + totalPoints);
                    Log.d(TAG, "New total points: " + newTotalPoints);
                    Log.d(TAG, "New average calculation: " + newTotalPoints + " / " + newCount + " = " + newAverage);
                } else {
                    // Nếu đây là lần đánh giá đầu tiên, trung bình = điểm đánh giá
                    newAverage = rating;
                    Log.d(TAG, "First ever rating, setting average to: " + rating);
                }

                Log.d(TAG, "First time rating, count: " + currentCount + " -> " + newCount);
            }

            // === BẢO VỆ KHỎI LỖI GIÁ TRỊ TRUNG BÌNH KHÔNG HỢP LỆ ===
            if (Double.isNaN(newAverage) || Double.isInfinite(newAverage)) {
                Log.e(TAG, "Invalid average calculated: " + newAverage + ", falling back to rating: " + rating);
                newAverage = rating;
            }

            String newRatingStr = String.format(Locale.US, "%.1f", newAverage);
            String newRatingCountStr = String.valueOf(newCount);

            if (newAverage <= 0 && rating > 0) {
                Log.e(TAG, "WARNING: New average is " + newAverage + " but rating is " + rating);
                // Sửa lỗi trường hợp tính toán sai cho ra 0.0
                newRatingStr = String.format(Locale.US, "%.1f", (double)rating);
                Log.d(TAG, "Correcting invalid average, setting to rating value: " + newRatingStr);
            }

            Log.d(TAG, "New rating count will be: " + newRatingCountStr);
            Log.d(TAG, "New rating average will be: " + newRatingStr);

            // QUAN TRỌNG: Cập nhật tài liệu sách
            Map<String, Object> bookUpdates = new HashMap<>();
            bookUpdates.put("rating", newRatingStr);
            bookUpdates.put("ratingCount", newRatingCountStr);
            bookUpdates.put("ratingAverage", newRatingStr);

            // Sử dụng set với merge để đảm bảo mọi trường được cập nhật đúng
            transaction.set(bookRef, bookUpdates, SetOptions.merge());

            // Cập nhật đánh giá của người dùng
            if (userRatings == null) {
                userRatings = new HashMap<>();
            }
            userRatings.put(bookId, rating);
            transaction.update(userRef, "ratings", userRatings);

            return null;
        }).addOnSuccessListener(result -> {
            Log.d(TAG, "Transaction success!");

            // Xác nhận thay đổi
            bookRef.get().addOnSuccessListener(updatedDoc -> {
                // Log toàn bộ dữ liệu sách sau khi cập nhật để kiểm tra
                Log.d(TAG, "Book data after update: " + updatedDoc.getData());
                Object updatedRating = updatedDoc.get("rating");
                Object updatedCount = updatedDoc.get("ratingCount");
                Object updatedAverage = updatedDoc.get("ratingAverage");
                Log.d(TAG, "Confirmed values after update - rating: " + updatedRating +
                        ", ratingCount: " + updatedCount +
                        ", ratingAverage: " + updatedAverage);
            });

            clearCache();
            future.complete(true);
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Transaction failed", e);
            future.completeExceptionally(e);
        });

        return future;
    }

    // Phương thức để kiểm tra và cập nhật rating trong trường hợp người dùng đã đánh giá trước đó
    public CompletableFuture<Boolean> updateBookRating(String bookId, int oldRating, int newRating, String userId) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();

        // Sử dụng transaction
        db.runTransaction(transaction -> {
            DocumentReference bookRef = db.collection("books").document(bookId);
            DocumentSnapshot bookDoc = transaction.get(bookRef);

            if (!bookDoc.exists()) {
                throw new FirebaseFirestoreException("Book not found",
                        FirebaseFirestoreException.Code.NOT_FOUND);
            }

            // Log toàn bộ dữ liệu sách để kiểm tra
            Log.d(TAG, "Book data: " + bookDoc.getData());

            // Xử lý đặc biệt cho ratingCount (giờ là String)
            String ratingCountStr = bookDoc.getString("ratingCount");
            int currentCount = 0;

            if (ratingCountStr != null && !ratingCountStr.isEmpty()) {
                try {
                    currentCount = Integer.parseInt(ratingCountStr);
                } catch (NumberFormatException e) {
                    Log.e(TAG, "Error parsing ratingCount", e);
                }
            }

            Log.d(TAG, "Current rating count (from Firestore): " + currentCount);

            // Lấy rating hiện tại với log đầy đủ
            String ratingStr = bookDoc.getString("rating");
            Log.d(TAG, "Raw rating string from Firestore: " + ratingStr);

            double currentAverage = 0.0;
            if (ratingStr != null && !ratingStr.isEmpty()) {
                try {
                    currentAverage = Double.parseDouble(ratingStr);
                    Log.d(TAG, "Successfully parsed current rating: " + currentAverage);
                } catch (NumberFormatException e) {
                    Log.e(TAG, "Error parsing rating", e);
                    currentAverage = 0.0;
                }
            } else {
                Log.d(TAG, "Rating string is null or empty, defaulting to 0.0");
            }

            // Tính toán trung bình mới với log chi tiết
            Log.d(TAG, "Calculation inputs: currentAverage=" + currentAverage +
                    ", currentCount=" + currentCount + ", oldRating=" + oldRating +
                    ", newRating=" + newRating);

            double newAverage;
            if (currentCount > 0) {
                double totalPoints = currentAverage * currentCount;
                double newTotalPoints = totalPoints - oldRating + newRating;
                newAverage = newTotalPoints / currentCount;

                Log.d(TAG, "Total points: " + totalPoints);
                Log.d(TAG, "New total points: " + newTotalPoints);
                Log.d(TAG, "New average calculation: " + newTotalPoints + " / " + currentCount + " = " + newAverage);
            } else {
                newAverage = newRating;
                // Nếu không có đánh giá nhưng có người đã đánh giá, điều chỉnh
                currentCount = 1;
                Log.d(TAG, "Fixing inconsistent data: Setting count to 1 and average to rating: " + newRating);
            }

            // Kiểm tra giá trị không hợp lệ
            if (Double.isNaN(newAverage) || Double.isInfinite(newAverage) || newAverage <= 0) {
                if (newRating > 0) {
                    Log.e(TAG, "Invalid average calculated: " + newAverage + ", falling back to rating: " + newRating);
                    newAverage = newRating;
                }
            }

            String updatedRating = String.format(Locale.US, "%.1f", newAverage);
            String newAverageStr = String.format(Locale.US, "%.1f", newAverage);
            String newCountStr = String.valueOf(currentCount); // Convert to String

            Log.d(TAG, "Updating rating from " + oldRating + " to " + newRating);
            Log.d(TAG, "New rating average will be: " + updatedRating);
            Log.d(TAG, "Rating count will be: " + newCountStr);

            // Cập nhật trong Firestore
            Map<String, Object> updates = new HashMap<>();
            updates.put("rating", updatedRating);
            updates.put("ratingAverage", newAverageStr);
            updates.put("ratingCount", newCountStr); // Đảm bảo ratingCount là String

            // Sử dụng set với merge để đảm bảo tất cả trường được cập nhật
            transaction.set(bookRef, updates, SetOptions.merge());

            // Cập nhật đánh giá của người dùng
            DocumentReference userRef = db.collection("users").document(userId);
            DocumentSnapshot userDoc = transaction.get(userRef);

            Map<String, Object> userRatings = userDoc.exists() ?
                    (Map<String, Object>) userDoc.get("ratings") : new HashMap<>();

            if (userRatings == null) {
                userRatings = new HashMap<>();
            }
            userRatings.put(bookId, newRating);

            transaction.update(userRef, "ratings", userRatings);

            return null;
        }).addOnSuccessListener(result -> {
            Log.d(TAG, "Rating update transaction successful");

            // Kiểm tra lại giá trị sau khi cập nhật
            db.collection("books").document(bookId).get()
                    .addOnSuccessListener(updatedDoc -> {
                        Log.d(TAG, "Book data after update: " + updatedDoc.getData());
                        Object updatedRating = updatedDoc.get("rating");
                        Object updatedCount = updatedDoc.get("ratingCount");
                        Object updatedAverage = updatedDoc.get("ratingAverage");
                        Log.d(TAG, "Confirmed values after update - rating: " + updatedRating +
                                ", ratingCount: " + updatedCount +
                                ", ratingAverage: " + updatedAverage);
                    });

            clearCache();
            future.complete(true);
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Rating update transaction failed", e);
            future.completeExceptionally(e);
        });

        return future;
    }

    public void fixRatingCountInconsistencies() {
        // 1. Đầu tiên lấy tất cả đánh giá của người dùng
        Map<String, Set<String>> bookRaters = new HashMap<>();

        db.collection("users").get().addOnSuccessListener(userSnapshots -> {
            // Thu thập tất cả đánh giá từ người dùng
            for (DocumentSnapshot userDoc : userSnapshots.getDocuments()) {
                Map<String, Object> ratings = (Map<String, Object>) userDoc.get("ratings");
                if (ratings != null) {
                    for (String bookId : ratings.keySet()) {
                        bookRaters.computeIfAbsent(bookId, k -> new HashSet<>())
                                .add(userDoc.getId());
                    }
                }
            }

            // 2. Sau đó lấy tất cả sách và cập nhật ratingCount
            db.collection("books").get().addOnSuccessListener(bookSnapshots -> {
                final int[] updateCount = {0};
                final int totalBooks = bookSnapshots.size();

                WriteBatch batch = db.batch();

                for (DocumentSnapshot bookDoc : bookSnapshots.getDocuments()) {
                    String bookId = bookDoc.getId();
                    Set<String> ratersForBook = bookRaters.getOrDefault(bookId, new HashSet<>());
                    int actualRaterCount = ratersForBook.size();
                    String actualCountStr = String.valueOf(actualRaterCount); // Convert to String

                    // Lấy ratingCount hiện tại
                    String storedCountStr = bookDoc.getString("ratingCount");
                    int storedCount = 0;

                    if (storedCountStr != null && !storedCountStr.isEmpty()) {
                        try {
                            storedCount = Integer.parseInt(storedCountStr);
                        } catch (NumberFormatException e) {
                            Log.e(TAG, "Error parsing stored ratingCount", e);
                        }
                    }

                    // Nếu không khớp với số người đánh giá thực tế
                    if (storedCount != actualRaterCount) {
                        Log.d(TAG, "Fixing book " + bookId +
                                ": stored count = " + storedCount +
                                ", actual count = " + actualRaterCount);

                        // Cập nhật trong batch
                        batch.update(db.collection("books").document(bookId),
                                "ratingCount", actualCountStr); // String format

                        updateCount[0]++;

                        // Nếu batch quá lớn, commit và tạo batch mới
                        if (updateCount[0] % 500 == 0) {
                            batch.commit();
                            batch = db.batch();
                        }
                    }
                }

                // Commit batch cuối cùng nếu có cập nhật
                if (updateCount[0] % 500 != 0) {
                    batch.commit().addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "Fixed " + updateCount[0] + " out of " + totalBooks + " books");
                    });
                } else {
                    Log.d(TAG, "No books needed fixing");
                }
            });
        });
    }
}