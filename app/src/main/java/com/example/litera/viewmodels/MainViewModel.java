package com.example.litera.viewmodels;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.litera.models.Author;
import com.example.litera.models.Book;
import com.example.litera.models.User;
import com.example.litera.repositories.BookRepository;
import com.example.litera.repositories.AuthorRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class MainViewModel extends ViewModel {
    private static final String TAG = "MainViewModel";

    private final MutableLiveData<List<Book>> trendingBooks = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<Book>> continueReadingBooks = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<Author>> popularAuthors = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    private final BookRepository bookRepository;
    private final AuthorRepository authorRepository;

    // Cache cho books và authors
    private final Map<String, MutableLiveData<Book>> bookCache = new HashMap<>();
    private final Map<String, MutableLiveData<Author>> authorCache = new HashMap<>();

    public MainViewModel() {
        bookRepository = BookRepository.getInstance();
        authorRepository = AuthorRepository.getInstance();

        // Đánh dấu là đang tải
        isLoading.setValue(true);

        // Load data from Firebase
        loadTrendingBooks();
        loadContinueReadingBooks();
        loadPopularAuthors();

        // Đánh dấu là đã tải xong
        isLoading.setValue(false);
    }

    // Phương thức lấy sách theo ID
    public LiveData<Book> getBookById(String bookId) {
        // Kiểm tra cache trước
        if (bookCache.containsKey(bookId)) {
            return bookCache.get(bookId);
        }

        // Nếu không có trong cache, tạo LiveData mới
        MutableLiveData<Book> bookLiveData = new MutableLiveData<>();
        bookCache.put(bookId, bookLiveData);

        // Tải dữ liệu sách
        loadBookById(bookId, bookLiveData);

        return bookLiveData;
    }

    // Phương thức mới: lấy tác giả theo ID
    public LiveData<Author> getAuthorById(String authorId) {
        // Kiểm tra cache trước
        if (authorCache.containsKey(authorId)) {
            return authorCache.get(authorId);
        }

        // Nếu không có trong cache, tạo LiveData mới
        MutableLiveData<Author> authorLiveData = new MutableLiveData<>();
        authorCache.put(authorId, authorLiveData);

        // Tải dữ liệu tác giả
        loadAuthorById(authorId, authorLiveData);

        return authorLiveData;
    }

    // Phương thức helper để tải sách theo ID
    private void loadBookById(String bookId, MutableLiveData<Book> liveData) {
        bookRepository.getBookById(bookId)
                .thenAccept(book -> {
                    if (book != null) {
                        // Đảm bảo rằng các trường mới được xử lý đúng
                        validateBookFields(book);

                        // Nếu có authorId, tải thông tin tác giả
                        if (book.getAuthorId() != null && !book.getAuthorId().isEmpty()) {
                            // Sử dụng authorRepository thay vì bookRepository
                            authorRepository.getAuthorById(book.getAuthorId())
                                    .thenAccept(author -> {
                                        if (author != null) {
                                            book.setAuthor(author);
                                        }
                                        liveData.postValue(book);
                                    })
                                    .exceptionally(e -> {
                                        // Vẫn trả về book ngay cả khi không lấy được tác giả
                                        liveData.postValue(book);
                                        Log.e(TAG, "Error loading author for book: " + bookId, e);
                                        return null;
                                    });
                        } else {
                            liveData.postValue(book);
                        }
                    } else {
                        errorMessage.postValue("Không tìm thấy sách với ID: " + bookId);
                    }
                })
                .exceptionally(ex -> {
                    errorMessage.postValue("Lỗi khi tải sách: " + ex.getMessage());
                    Log.e(TAG, "Error loading book by ID: " + bookId, ex);
                    return null;
                });
    }

    /**
     * Phương thức mới để kiểm tra và đảm bảo các trường mới (pricePhysic) được xử lý đúng
     */
    private void validateBookFields(Book book) {
        // Kiểm tra trường price
        if (book.getPrice() == null || book.getPrice().isEmpty()) {
            book.setPrice("0.00"); // Giá mặc định
        }

        // Kiểm tra trường pricePhysic - Trường mới trong Firebase
        if (book.getPricePhysic() == null || book.getPricePhysic().isEmpty()) {
            if (book.getPrice() != null && !book.getPrice().isEmpty()) {
                // Nếu không có giá sách vật lý, sử dụng giá sách điện tử cộng thêm 10$ làm mặc định
                try {
                    float priceValue = Float.parseFloat(book.getPrice());
                    float physicalPrice = priceValue + 10.0f;
                    book.setPricePhysic(String.format("%.2f", physicalPrice));
                } catch (NumberFormatException e) {
                    book.setPricePhysic("19.99"); // Giá mặc định nếu không thể chuyển đổi
                    Log.e(TAG, "Error parsing price: " + book.getPrice(), e);
                }
            } else {
                book.setPricePhysic("19.99"); // Giá mặc định
            }
        }
    }

    // Phương thức helper để tải tác giả theo ID
    private void loadAuthorById(String authorId, MutableLiveData<Author> liveData) {
        authorRepository.getAuthorById(authorId)
                .thenAccept(author -> {
                    if (author != null) {
                        liveData.postValue(author);
                    } else {
                        errorMessage.postValue("Không tìm thấy tác giả với ID: " + authorId);
                    }
                })
                .exceptionally(ex -> {
                    errorMessage.postValue("Lỗi khi tải thông tin tác giả: " + ex.getMessage());
                    Log.e(TAG, "Error loading author by ID: " + authorId, ex);
                    return null;
                });
    }

    private void loadTrendingBooks() {
        // Đánh dấu là đang tải
        isLoading.setValue(true);

        // Sử dụng phương thức getTrendingBooks() từ repository thay vì getBooks()
        bookRepository.getTrendingBooks()
                .thenAccept(trendingBooks -> {
                    // Tải thông tin tác giả cho mỗi cuốn sách
                    for (Book book : trendingBooks) {
                        // Đảm bảo các trường mới được xử lý đúng
                        validateBookFields(book);

                        if (book.getAuthorId() != null && !book.getAuthorId().isEmpty()) {
                            // Sử dụng authorRepository để lấy thông tin tác giả
                            authorRepository.getAuthorById(book.getAuthorId())
                                    .thenAccept(book::setAuthor)
                                    .exceptionally(e -> {
                                        Log.e(TAG, "Error loading author for book: " + book.getId(), e);
                                        return null;
                                    });
                        }
                    }

                    // Cập nhật dữ liệu và thông báo đã tải xong
                    this.trendingBooks.postValue(trendingBooks);
                    isLoading.postValue(false);

                    // Log để debug
                    Log.d(TAG, "Loaded " + trendingBooks.size() + " trending books");
                })
                .exceptionally(e -> {
                    Log.e(TAG, "Error loading trending books", e);
                    errorMessage.postValue("Lỗi khi tải sách nổi bật: " + e.getMessage());
                    isLoading.postValue(false);
                    return null;
                });
    }

    private void loadContinueReadingBooks() {
        // Đánh dấu là đang tải
        isLoading.setValue(true);

        // Sử dụng phương thức getContinueReadingBooks từ repository
        bookRepository.getContinueReadingBooks()
                .thenAccept(books -> {
                    // Tải thông tin tác giả cho mỗi cuốn sách
                    List<Book> validBooks = new ArrayList<>();

                    for (Book book : books) {
                        // Đảm bảo các trường mới được xử lý đúng
                        validateBookFields(book);

                        // Nếu có authorId, tải thông tin tác giả
                        if (book.getAuthorId() != null && !book.getAuthorId().isEmpty()) {
                            // Sử dụng authorRepository để lấy thông tin tác giả
                            authorRepository.getAuthorById(book.getAuthorId())
                                    .thenAccept(author -> {
                                        book.setAuthor(author);
                                    })
                                    .exceptionally(e -> {
                                        Log.e(TAG, "Error loading author for book: " + book.getId(), e);
                                        return null;
                                    });
                        }

                        // Thêm sách vào danh sách hợp lệ
                        validBooks.add(book);
                    }

                    // Cập nhật LiveData với danh sách sách hợp lệ
                    continueReadingBooks.postValue(validBooks);
                    isLoading.postValue(false);

                    // Log để debug
                    Log.d(TAG, "Loaded " + validBooks.size() + " continue reading books");
                })
                .exceptionally(e -> {
                    Log.e(TAG, "Error loading continue reading books", e);
                    errorMessage.postValue("Lỗi khi tải sách đang đọc: " + e.getMessage());
                    isLoading.postValue(false);
                    return null;
                });
    }

    // Hàm mới để tải thông tin chi tiết của sách đang đọc
    private void fetchContinueReadingBooks(List<String> bookIds) {
        List<Book> books = new ArrayList<>();
        AtomicInteger counter = new AtomicInteger(0);

        if (bookIds.isEmpty()) {
            continueReadingBooks.postValue(books);
            return;
        }

        for (String bookId : bookIds) {
            bookRepository.getBookById(bookId)
                    .thenAccept(book -> {
                        if (book != null) {
                            books.add(book);
                        }

                        // Kiểm tra đã tải xong tất cả sách chưa
                        if (counter.incrementAndGet() == bookIds.size()) {
                            continueReadingBooks.postValue(books);
                        }
                    })
                    .exceptionally(e -> {
                        Log.e(TAG, "Error loading book: " + bookId, e);

                        // Vẫn đếm số lượng sách đã xử lý xong
                        if (counter.incrementAndGet() == bookIds.size()) {
                            continueReadingBooks.postValue(books);
                        }
                        return null;
                    });
        }
    }

    private void loadPopularAuthors() {
        authorRepository.getPopularAuthors()
                .thenAccept(authors -> {
                    popularAuthors.postValue(authors);
                })
                .exceptionally(e -> {
                    Log.e(TAG, "Error loading popular authors", e);
                    errorMessage.postValue("Lỗi khi tải tác giả nổi tiếng: " + e.getMessage());
                    return null;
                });
    }

    public LiveData<List<Book>> getTrendingBooks() {
        if (trendingBooks.getValue() == null || trendingBooks.getValue().isEmpty()) {
            loadTrendingBooks();
        }
        return trendingBooks;
    }

    public LiveData<List<Book>> getContinueReadingBooks() {
        if (continueReadingBooks.getValue() == null || continueReadingBooks.getValue().isEmpty()) {
            loadContinueReadingBooks();
        }
        return continueReadingBooks;
    }

    public LiveData<List<Author>> getPopularAuthors() {
        if (popularAuthors.getValue() == null || popularAuthors.getValue().isEmpty()) {
            loadPopularAuthors();
        }
        return popularAuthors;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    private final MutableLiveData<List<Book>> allBooks = new MutableLiveData<>(new ArrayList<>());

    public LiveData<List<Book>> getAllBooks() {
        if (allBooks.getValue() == null || allBooks.getValue().isEmpty()) {
            loadAllBooks();
        }
        return allBooks;
    }

    private void loadAllBooks() {
        isLoading.setValue(true);

        bookRepository.getBooks()
                .thenAccept(books -> {
                    // Tải thông tin tác giả cho mỗi cuốn sách
                    for (Book book : books) {
                        // Đảm bảo các trường mới được xử lý đúng
                        validateBookFields(book);

                        if (book.getAuthorId() != null && !book.getAuthorId().isEmpty()) {
                            // Sử dụng authorRepository để lấy thông tin tác giả
                            authorRepository.getAuthorById(book.getAuthorId())
                                    .thenAccept(book::setAuthor)
                                    .exceptionally(e -> {
                                        Log.e(TAG, "Error loading author for book: " + book.getId(), e);
                                        return null;
                                    });
                        }
                    }

                    // Lọc hoặc sắp xếp sách đã đọc ở đây nếu cần
                    // Ví dụ: chỉ lấy vài cuốn sách đầu tiên
                    List<Book> allBooksList = books.size() > 6 ?
                            books.subList(0, 6) : new ArrayList<>(books);
                    allBooks.postValue(allBooksList);
                    isLoading.postValue(false);

                })
                .exceptionally(e -> {
                    Log.e(TAG, "Error loading all books", e);
                    errorMessage.postValue("Lỗi khi tải danh sách sách: " + e.getMessage());
                    isLoading.postValue(false);
                    return null;
                });
    }

    // Method to refresh data
    public void refreshData() {
        // Đánh dấu là đang tải
        isLoading.setValue(true);

        // Xóa cache trước khi làm mới dữ liệu
        clearCache();

        // Tải lại dữ liệu
        loadTrendingBooks();
        loadContinueReadingBooks();
        loadPopularAuthors();
        loadAllBooks();

        // Đánh dấu là đã tải xong
        isLoading.setValue(false);
    }

    // Phương thức để xóa cache khi cần
    public void clearCache() {
        bookCache.clear();
        authorCache.clear();
        bookRepository.clearCache();
        authorRepository.clearCache();
    }

    // Phương thức để refresh data một cách có chọn lọc
    public void refreshBookData(String bookId) {
        // Chỉ làm mới dữ liệu của sách cụ thể nếu nó đang ở trong cache
        if (bookCache.containsKey(bookId)) {
            // Xóa cache trước khi tải lại
            bookRepository.clearCache();

            // Tải lại dữ liệu sách
            loadBookById(bookId, bookCache.get(bookId));
        }

        // Refresh tất cả các danh sách sách
        refreshBookLists();
    }

    // Phương thức để làm mới các danh sách sách
    public void refreshBookLists() {
        // Đánh dấu là đang tải
        isLoading.setValue(true);

        // Xóa cache ở repository
        bookRepository.clearCache();

        // Tải lại tất cả dữ liệu sách
        loadTrendingBooks();
        loadContinueReadingBooks();
        loadAllBooks();

        isLoading.postValue(false);
    }
}
