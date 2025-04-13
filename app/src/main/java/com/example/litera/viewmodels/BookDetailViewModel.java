package com.example.litera.viewmodels;

import static android.content.ContentValues.TAG;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.litera.models.Author;
import com.example.litera.models.Book;
import com.example.litera.repositories.AuthorRepository;
import com.example.litera.repositories.BookRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.concurrent.CompletableFuture;

public class BookDetailViewModel extends ViewModel {

    private final BookRepository bookRepository;
    private final AuthorRepository authorRepository;
    private final MutableLiveData<Book> selectedBook = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isFavorite = new MutableLiveData<>(false);

    public BookDetailViewModel() {
        this.bookRepository = BookRepository.getInstance();
        this.authorRepository = new AuthorRepository();
    }

    public LiveData<Book> getSelectedBook() {
        return selectedBook;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public void selectBook(String bookId) {
        isLoading.setValue(true);
        errorMessage.setValue(null);

        // Ensure we're getting fresh data from Firestore
        bookRepository.clearCache();

        bookRepository.getBookById(bookId)
                .thenAccept(book -> {
                    if (book != null) {
                        // Lấy thông tin author
                        String authorId = book.getAuthorId();
                        if (authorId != null && !authorId.isEmpty()) {
                            authorRepository.getAuthorById(authorId)
                                    .thenAccept(author -> {
                                        book.setAuthor(author);
                                        selectedBook.postValue(book);
                                        isLoading.postValue(false);
                                    })
                                    .exceptionally(e -> {
                                        // Nếu không lấy được author, vẫn hiển thị book
                                        selectedBook.postValue(book);
                                        isLoading.postValue(false);
                                        errorMessage.postValue("Could not load author details: " + e.getMessage());
                                        return null;
                                    });
                        } else {
                            // Không có authorId
                            selectedBook.postValue(book);
                            isLoading.postValue(false);
                        }
                    } else {
                        // Không tìm thấy book
                        isLoading.postValue(false);
                        errorMessage.postValue("Book not found");
                    }
                })
                .exceptionally(e -> {
                    isLoading.postValue(false);
                    errorMessage.postValue("Error loading book: " + e.getMessage());
                    return null;
                });
    }
    public LiveData<Boolean> getIsFavorite() {
        return isFavorite;
    }

    public void checkFavoriteStatus(String bookId) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            isFavorite.setValue(false);
            return;
        }

        bookRepository.isBookFavorite(bookId, currentUser.getUid())
                .thenAccept(favorite -> {
                    Log.d(TAG, "Book is favorite: " + favorite);
                    isFavorite.postValue(favorite);
                })
                .exceptionally(e -> {
                    Log.e(TAG, "Error checking favorite status", e);
                    isFavorite.postValue(false);
                    return null;
                });
    }

    public void toggleFavorite(String bookId) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            errorMessage.setValue("Log in to add to favorites");
            return;
        }

        Boolean currentStatus = isFavorite.getValue();
        isLoading.setValue(true);

        if (currentStatus != null && currentStatus) {
            // Đã là yêu thích, xóa khỏi danh sách
            bookRepository.removeFromFavorites(bookId, currentUser.getUid())
                    .thenAccept(success -> {
                        if (success) {
                            isFavorite.postValue(false);
                        }
                        isLoading.postValue(false);
                    })
                    .exceptionally(e -> {
                        Log.e(TAG, "Error removing from favorites", e);
                        errorMessage.postValue("Error removing from favorites: " + e.getMessage());
                        isLoading.postValue(false);
                        return null;
                    });
        } else {
            // Chưa là yêu thích, thêm vào danh sách
            bookRepository.addToFavorites(bookId, currentUser.getUid())
                    .thenAccept(success -> {
                        if (success) {
                            isFavorite.postValue(true);
                        }
                        isLoading.postValue(false);
                    })
                    .exceptionally(e -> {
                        Log.e(TAG, "Error adding to favorites", e);
                        errorMessage.postValue("Error adding to favorites: " + e.getMessage());
                        isLoading.postValue(false);
                        return null;
                    });
        }
    }

}