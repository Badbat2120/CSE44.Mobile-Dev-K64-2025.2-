package com.example.litera.repositories;

import android.util.Log;

import com.example.litera.models.Author;
import com.example.litera.models.Book;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

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

        // Kiểm tra người dùng đã đăng nhập chưa
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            // Nếu chưa đăng nhập, trả về danh sách sách đang đọc mặc định (5 cuốn đầu)
            getBooks().thenAccept(allBooks -> {
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

        // Nếu đã đăng nhập, tải danh sách đang đọc từ Firestore
        FirebaseFirestore.getInstance()
                .collection("users")
                .document(currentUser.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // Lấy danh sách ID sách đang đọc từ document
                        List<String> bookIds = (List<String>) documentSnapshot.get("continue");

                        if (bookIds != null && !bookIds.isEmpty()) {
                            // Tải thông tin chi tiết của từng cuốn sách
                            List<Book> books = new ArrayList<>();
                            AtomicInteger counter = new AtomicInteger(0);

                            for (String bookId : bookIds) {
                                getBookById(bookId)
                                        .thenAccept(book -> {
                                            if (book != null) {
                                                books.add(book);
                                            }

                                            // Nếu đã tải tất cả sách, trả về danh sách
                                            if (counter.incrementAndGet() == bookIds.size()) {
                                                future.complete(books);
                                            }
                                        })
                                        .exceptionally(e -> {
                                            Log.e(TAG, "Error loading book: " + bookId, e);

                                            // Vẫn đếm để biết khi nào đã hoàn tất
                                            if (counter.incrementAndGet() == bookIds.size()) {
                                                future.complete(books);
                                            }
                                            return null;
                                        });
                            }
                        } else {
                            // Nếu không có danh sách sách đang đọc, sử dụng danh sách mặc định
                            getDefaultContinueReadingBooks(future);
                        }
                    } else {
                        // Nếu không tìm thấy document người dùng, sử dụng danh sách mặc định
                        getDefaultContinueReadingBooks(future);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading user document", e);
                    // Trong trường hợp lỗi, sử dụng danh sách mặc định
                    getDefaultContinueReadingBooks(future);
                });

        return future;
    }

    // Phương thức helper để lấy danh sách sách đang đọc mặc định
    private void getDefaultContinueReadingBooks(CompletableFuture<List<Book>> future) {
        getBooks().thenAccept(allBooks -> {
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

    // Thay thế phương thức rateBook hiện tại bằng phương thức này
    public CompletableFuture<Boolean> rateBook(String bookId, int rating, String userId) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();

        // Tham chiếu đến document
        DocumentReference bookRef = db.collection("books").document(bookId);
        DocumentReference userRef = db.collection("users").document(userId);

        // Đầu tiên lấy dữ liệu user để kiểm tra đánh giá trước đó
        userRef.get().addOnSuccessListener(userDoc -> {
            // Kiểm tra đánh giá cũ
            Map<String, Object> userRatings = userDoc.exists() ?
                    (Map<String, Object>) userDoc.get("ratings") : new HashMap<>();
            boolean hasRatedBefore = userRatings != null && userRatings.containsKey(bookId);

            // Lấy đánh giá cũ nếu có
            final int oldRating; // Đánh dấu là final
            if (hasRatedBefore) {
                Object oldRatingObj = userRatings.get(bookId);
                int tempOldRating = 0;
                if (oldRatingObj instanceof Long) {
                    tempOldRating = ((Long) oldRatingObj).intValue();
                } else if (oldRatingObj instanceof Integer) {
                    tempOldRating = (Integer) oldRatingObj;
                } else if (oldRatingObj instanceof Double) {
                    tempOldRating = ((Double) oldRatingObj).intValue();
                } else if (oldRatingObj instanceof String) {
                    try {
                        tempOldRating = Integer.parseInt((String) oldRatingObj);
                    } catch (NumberFormatException ignored) {}
                }
                oldRating = tempOldRating; // Gán giá trị cho oldRating final
            } else {
                oldRating = 0; // Nếu chưa đánh giá, gán giá trị mặc định là 0
            }

            // 1. Cập nhật rating trong document người dùng
            if (userRatings == null) {
                userRatings = new HashMap<>();
            }
            userRatings.put(bookId, rating);

            final int finalRating = rating; // Tạo biến final cho rating
            Map<String, Object> finalRatings = userRatings;
            userRef.update("ratings", finalRatings)
                    .addOnSuccessListener(v -> {
                        // 2. Lấy dữ liệu sách hiện tại
                        bookRef.get().addOnSuccessListener(bookDoc -> {
                            if (!bookDoc.exists()) {
                                future.completeExceptionally(new Exception("Book not found"));
                                return;
                            }

                            // Lấy rating và count hiện tại
                            String ratingStr = bookDoc.getString("rating");
                            String ratingCountStr = bookDoc.getString("ratingCount");

                            double currentAverage = 0;
                            int currentCount = 0;

                            // Parse rating
                            if (ratingStr != null && !ratingStr.isEmpty()) {
                                try {
                                    currentAverage = Double.parseDouble(ratingStr);
                                } catch (NumberFormatException e) {
                                    Log.e(TAG, "Error parsing rating", e);
                                }
                            }

                            // Parse count
                            if (ratingCountStr != null && !ratingCountStr.isEmpty()) {
                                try {
                                    currentCount = Integer.parseInt(ratingCountStr);
                                } catch (NumberFormatException e) {
                                    Log.e(TAG, "Error parsing count", e);
                                }
                            }

                            // Đảm bảo count ít nhất là 0
                            currentCount = Math.max(0, currentCount);

                            // Tính toán giá trị mới
                            double totalPoints = currentAverage * currentCount;
                            int newCount;
                            double newTotalPoints;

                            if (hasRatedBefore) {
                                // Người dùng đã đánh giá trước đó - chỉ cập nhật giá trị
                                newCount = currentCount;
                                newTotalPoints = totalPoints - oldRating + finalRating; // Sử dụng finalRating
                                Log.d(TAG, "Updating existing rating from " + oldRating + " to " + finalRating); // Sử dụng finalRating
                            } else {
                                // Đánh giá mới - tăng count
                                newCount = currentCount + 1;
                                newTotalPoints = totalPoints + finalRating; // Sử dụng finalRating
                                Log.d(TAG, "Adding new rating: " + finalRating + ", new count: " + newCount); // Sử dụng finalRating
                            }

                            // Tính trung bình mới
                            double newAverage = newCount > 0 ? newTotalPoints / newCount : finalRating; // Sử dụng finalRating

                            // Log chi tiết quá trình tính toán
                            Log.d(TAG, String.format("Rating calculation: total=%.1f, count=%d, avg=%.2f",
                                    newTotalPoints, newCount, newAverage));

                            // Kiểm tra giá trị không hợp lệ
                            if (Double.isNaN(newAverage) || Double.isInfinite(newAverage) || newAverage <= 0) {
                                if (finalRating > 0) { // Sử dụng finalRating
                                    newAverage = finalRating; // Sử dụng finalRating
                                    Log.d(TAG, "Invalid average detected, falling back to: " + finalRating); // Sử dụng finalRating
                                }
                            }

                            // Format để lưu vào Firestore
                            String newRatingStr = String.format(Locale.US, "%.1f", newAverage);
                            String newCountStr = String.valueOf(newCount);

                            // 3. Cập nhật document book
                            Map<String, Object> updates = new HashMap<>();
                            updates.put("rating", newRatingStr);
                            updates.put("ratingCount", newCountStr);
                            updates.put("ratingAverage", newRatingStr);

                            // Sử dụng update thay vì set với merge
                            bookRef.update(updates)
                                    .addOnSuccessListener(aVoid -> {
                                        Log.d(TAG, "Book rating updated successfully");
                                        clearCache();
                                        future.complete(true);
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e(TAG, "Failed to update book rating", e);
                                        future.completeExceptionally(e);
                                    });
                        }).addOnFailureListener(e -> future.completeExceptionally(e));
                    })
                    .addOnFailureListener(e -> future.completeExceptionally(e));
        }).addOnFailureListener(e -> future.completeExceptionally(e));

        return future;
    }

    // Thêm phương thức này để debug và kiểm tra vấn đề
    public void debugRatingSystem(String bookId) {
        Log.d(TAG, "Starting rating system debug for book: " + bookId);

        // 1. Kiểm tra dữ liệu sách
        db.collection("books").document(bookId)
                .get()
                .addOnSuccessListener(bookDoc -> {
                    if (!bookDoc.exists()) {
                        Log.e(TAG, "Book not found!");
                        return;
                    }

                    Log.d(TAG, "Book data: " + bookDoc.getData());
                    String ratingStr = bookDoc.getString("rating");
                    String countStr = bookDoc.getString("ratingCount");
                    String avgStr = bookDoc.getString("ratingAverage");

                    Log.d(TAG, String.format(
                            "Book rating info: rating=%s, count=%s, average=%s",
                            ratingStr, countStr, avgStr));

                    // 2. Kiểm tra các đánh giá của người dùng
                    db.collection("users")
                            .get()
                            .addOnSuccessListener(usersSnapshot -> {
                                int actualRaters = 0;
                                double totalRating = 0;

                                for (DocumentSnapshot userDoc : usersSnapshot) {
                                    Map<String, Object> ratings = (Map<String, Object>) userDoc.get("ratings");
                                    if (ratings != null && ratings.containsKey(bookId)) {
                                        actualRaters++;

                                        Object ratingObj = ratings.get(bookId);
                                        int userRating = 0;

                                        if (ratingObj instanceof Long) {
                                            userRating = ((Long) ratingObj).intValue();
                                        } else if (ratingObj instanceof Integer) {
                                            userRating = (Integer) ratingObj;
                                        } else if (ratingObj instanceof Double) {
                                            userRating = ((Double) ratingObj).intValue();
                                        } else if (ratingObj instanceof String) {
                                            try {
                                                userRating = Integer.parseInt(ratingObj.toString());
                                            } catch (NumberFormatException ignored) {}
                                        }

                                        totalRating += userRating;
                                        Log.d(TAG, String.format(
                                                "User %s rated this book: %d stars",
                                                userDoc.getId(), userRating));
                                    }
                                }

                                double actualAverage = actualRaters > 0 ? totalRating / actualRaters : 0;

                                Log.d(TAG, String.format(
                                        "Actual rating stats: count=%d, total=%.1f, average=%.2f",
                                        actualRaters, totalRating, actualAverage));

                                // 3. So sánh với dữ liệu đang lưu
                                int storedCount = 0;
                                double storedAverage = 0;

                                try {
                                    if (countStr != null && !countStr.isEmpty()) {
                                        storedCount = Integer.parseInt(countStr);
                                    }

                                    if (ratingStr != null && !ratingStr.isEmpty()) {
                                        storedAverage = Double.parseDouble(ratingStr);
                                    }
                                } catch (NumberFormatException e) {
                                    Log.e(TAG, "Error parsing stored values", e);
                                }

                                // 4. Hiển thị kết quả và đề xuất sửa
                                Log.d(TAG, String.format(
                                        "Comparison: stored count=%d vs actual=%d, stored average=%.2f vs actual=%.2f",
                                        storedCount, actualRaters, storedAverage, actualAverage));

                                if (storedCount != actualRaters || Math.abs(storedAverage - actualAverage) > 0.1) {
                                    Log.e(TAG, "INCONSISTENCY DETECTED in rating data!");

                                    // 5. Tự động sửa nếu cần
                                    if (storedCount != actualRaters) {
                                        Log.d(TAG, "Auto-fixing rating count inconsistency...");
                                        db.collection("books").document(bookId)
                                                .update("ratingCount", String.valueOf(actualRaters))
                                                .addOnSuccessListener(v -> Log.d(TAG, "Fixed rating count!"))
                                                .addOnFailureListener(e -> Log.e(TAG, "Failed to fix rating count", e));
                                    }

                                    if (Math.abs(storedAverage - actualAverage) > 0.1 && actualRaters > 0) {
                                        String fixedAverage = String.format(Locale.US, "%.1f", actualAverage);
                                        Log.d(TAG, "Auto-fixing rating average inconsistency...");
                                        Map<String, Object> updates = new HashMap<>();
                                        updates.put("rating", fixedAverage);
                                        updates.put("ratingAverage", fixedAverage);

                                        db.collection("books").document(bookId)
                                                .update(updates)
                                                .addOnSuccessListener(v -> Log.d(TAG, "Fixed rating average!"))
                                                .addOnFailureListener(e -> Log.e(TAG, "Failed to fix rating average", e));
                                    }
                                }
                            })
                            .addOnFailureListener(e -> Log.e(TAG, "Error checking user ratings", e));
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error getting book data", e));
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

            // Đảm bảo currentCount ít nhất là 1 nếu có đánh giá
            if (currentCount == 0 && (currentAverage > 0 || oldRating > 0)) {
                // Nếu có điểm trung bình nhưng count là 0, thiết lập count = 1
                currentCount = 1;
                Log.d(TAG, "Fixing inconsistent data: Setting count to 1 since ratings exist");
            }

            // Tính toán trung bình mới
            Log.d(TAG, "Calculation inputs: currentAverage=" + currentAverage +
                    ", currentCount=" + currentCount + ", oldRating=" + oldRating +
                    ", newRating=" + newRating);

            double totalPoints = currentAverage * currentCount;
            double newTotalPoints = totalPoints - oldRating + newRating;
            double newAverage = currentCount > 0 ? newTotalPoints / currentCount : newRating;

            Log.d(TAG, "Total points: " + totalPoints);
            Log.d(TAG, "New total points: " + newTotalPoints);
            Log.d(TAG, "New average calculation: " + newTotalPoints + " / " + currentCount + " = " + newAverage);

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

    /**
     * Kiểm tra xem một cuốn sách có nằm trong danh sách yêu thích của người dùng không
     */
    public CompletableFuture<Boolean> isBookFavorite(String bookId, String userId) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();

        if (userId == null || userId.isEmpty() || bookId == null || bookId.isEmpty()) {
            future.complete(false);
            return future;
        }

        db.collection("users").document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        List<String> favoriteIds = (List<String>) documentSnapshot.get("favourite");
                        boolean isFavorite = favoriteIds != null && favoriteIds.contains(bookId);
                        future.complete(isFavorite);
                    } else {
                        future.complete(false);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error checking if book is favorite", e);
                    future.completeExceptionally(e);
                });

        return future;
    }

    /**
     * Thêm một cuốn sách vào danh sách yêu thích của người dùng
     */
    public CompletableFuture<Boolean> addToFavorites(String bookId, String userId) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();

        if (userId == null || userId.isEmpty() || bookId == null || bookId.isEmpty()) {
            future.complete(false);
            return future;
        }

        DocumentReference userRef = db.collection("users").document(userId);

        userRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                // Lấy danh sách hiện tại
                List<String> favoriteIds = (List<String>) documentSnapshot.get("favourite");

                // Nếu danh sách null thì tạo mới
                if (favoriteIds == null) {
                    favoriteIds = new ArrayList<>();
                }

                // Nếu chưa có trong danh sách thì thêm vào
                if (!favoriteIds.contains(bookId)) {
                    favoriteIds.add(bookId);
                    userRef.update("favourite", favoriteIds)
                            .addOnSuccessListener(v -> future.complete(true))
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Error updating favorites", e);
                                future.completeExceptionally(e);
                            });
                } else {
                    // Đã có trong danh sách
                    future.complete(true);
                }
            } else {
                future.completeExceptionally(new Exception("User document not found"));
            }
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Error checking user document", e);
            future.completeExceptionally(e);
        });

        return future;
    }

    /**
     * Xóa một cuốn sách khỏi danh sách yêu thích của người dùng
     */
    public CompletableFuture<Boolean> removeFromFavorites(String bookId, String userId) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();

        if (userId == null || userId.isEmpty() || bookId == null || bookId.isEmpty()) {
            future.complete(false);
            return future;
        }

        DocumentReference userRef = db.collection("users").document(userId);

        userRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                // Lấy danh sách hiện tại
                List<String> favoriteIds = (List<String>) documentSnapshot.get("favourite");

                // Nếu danh sách rỗng thì không cần xóa
                if (favoriteIds == null || favoriteIds.isEmpty()) {
                    future.complete(false);
                    return;
                }

                // Nếu có trong danh sách thì xóa
                if (favoriteIds.contains(bookId)) {
                    favoriteIds.remove(bookId);
                    userRef.update("favourite", favoriteIds)
                            .addOnSuccessListener(v -> future.complete(true))
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Error updating favorites", e);
                                future.completeExceptionally(e);
                            });
                } else {
                    // Không có trong danh sách
                    future.complete(false);
                }
            } else {
                future.completeExceptionally(new Exception("User document not found"));
            }
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Error checking user document", e);
            future.completeExceptionally(e);
        });

        return future;
    }

    /**
     * Lấy danh sách sách yêu thích dựa trên userId
     */
    public CompletableFuture<List<Book>> getFavoriteBooks(String userId) {
        CompletableFuture<List<Book>> future = new CompletableFuture<>();

        if (userId == null || userId.isEmpty()) {
            future.complete(new ArrayList<>());
            return future;
        }

        // Lấy document người dùng để xem danh sách ID sách yêu thích
        db.collection("users").document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // Lấy danh sách ID sách yêu thích
                        List<String> favoriteIds = (List<String>) documentSnapshot.get("favourite");

                        if (favoriteIds == null || favoriteIds.isEmpty()) {
                            // Không có sách yêu thích
                            future.complete(new ArrayList<>());
                            return;
                        }

                        // Tải thông tin chi tiết của từng cuốn sách
                        List<Book> favoriteBooks = new ArrayList<>();
                        final int[] completedCount = {0};

                        for (String bookId : favoriteIds) {
                            getBookById(bookId)
                                    .thenAccept(book -> {
                                        if (book != null) {
                                            favoriteBooks.add(book);
                                        }

                                        // Kiểm tra đã hoàn thành hết chưa
                                        completedCount[0]++;
                                        if (completedCount[0] >= favoriteIds.size()) {
                                            future.complete(favoriteBooks);
                                        }
                                    })
                                    .exceptionally(e -> {
                                        Log.e(TAG, "Error loading favorite book: " + bookId, e);
                                        completedCount[0]++;

                                        // Hoàn thành future dù có lỗi
                                        if (completedCount[0] >= favoriteIds.size()) {
                                            future.complete(favoriteBooks);
                                        }
                                        return null;
                                    });
                        }
                    } else {
                        // Document người dùng không tồn tại
                        future.complete(new ArrayList<>());
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting user favorites", e);
                    future.completeExceptionally(e);
                });

        return future;
    }
}