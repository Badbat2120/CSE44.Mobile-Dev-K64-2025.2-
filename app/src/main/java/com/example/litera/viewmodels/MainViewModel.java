package com.example.litera.viewmodels;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.litera.models.Author;
import com.example.litera.models.Book;
import com.example.litera.repositories.BookRepository;
import com.example.litera.repositories.AuthorRepository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        bookRepository.getBooks()
                .thenAccept(allBooks -> {
                    // Tải thông tin tác giả cho mỗi cuốn sách
                    for (Book book : allBooks) {
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
                    // Lọc hoặc sắp xếp sách trending ở đây nếu cần
                    trendingBooks.postValue(allBooks);
                })
                .exceptionally(e -> {
                    Log.e(TAG, "Error loading trending books", e);
                    errorMessage.postValue("Lỗi khi tải sách nổi bật: " + e.getMessage());
                    return null;
                });
    }

    private void loadContinueReadingBooks() {
        //Chúng ta có thể sử dụng getBooks() và lọc/sắp xếp
        bookRepository.getBooks()
                .thenAccept(allBooks -> {
                    // Tải thông tin tác giả cho mỗi cuốn sách
                    for (Book book : allBooks) {
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
                    List<Book> reading = allBooks.size() > 5 ?
                            allBooks.subList(0, 5) : new ArrayList<>(allBooks);
                    continueReadingBooks.postValue(reading);
                })
                .exceptionally(e -> {
                    Log.e(TAG, "Error loading continue reading books", e);
                    errorMessage.postValue("Lỗi khi tải sách đang đọc: " + e.getMessage());
                    return null;
                });
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

    // Method to refresh data
    public void refreshData() {
        isLoading.setValue(true);
        loadTrendingBooks();
        loadContinueReadingBooks();
        loadPopularAuthors();
        isLoading.setValue(false);
    }

    // Phương thức để xóa cache khi cần
    public void clearCache() {
        bookCache.clear();
        authorCache.clear();
        bookRepository.clearCache();
        authorRepository.clearCache();
    }
}