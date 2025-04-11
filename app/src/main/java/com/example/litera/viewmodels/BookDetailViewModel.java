package com.example.litera.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.litera.models.Book;
import com.example.litera.repositories.BookRepository;

public class BookDetailViewModel extends ViewModel {

    private final MutableLiveData<Book> selectedBook = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final BookRepository bookRepository;

    public BookDetailViewModel() {
        // Initialize repository
        bookRepository = BookRepository.getInstance();
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
        // Show loading indicator
        isLoading.setValue(true);

        // Fetch book by ID from Firebase via repository
        bookRepository.getBookById(bookId)
                .thenAccept(book -> {
                    selectedBook.postValue(book);
                    isLoading.postValue(false);
                })
                .exceptionally(e -> {
                    errorMessage.postValue("Failed to load book details: " + e.getMessage());
                    isLoading.postValue(false);
                    return null;
                });
    }
}