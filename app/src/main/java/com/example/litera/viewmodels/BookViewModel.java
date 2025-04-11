package com.example.litera.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.litera.models.Book;
import com.example.litera.repositories.BookRepository;

import java.util.ArrayList;
import java.util.List;

public class BookViewModel extends ViewModel {

    private final MutableLiveData<List<Book>> books = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final BookRepository bookRepository;

    public BookViewModel() {
        // Initialize repository using Singleton pattern
        bookRepository = BookRepository.getInstance();
        loadBooks();
    }

    private void loadBooks() {
        isLoading.setValue(true);
        bookRepository.getBooks()
                .thenAccept(loadedBooks -> {
                    books.postValue(loadedBooks);
                    isLoading.postValue(false);
                })
                .exceptionally(e -> {
                    errorMessage.postValue("Failed to load books: " + e.getMessage());
                    isLoading.postValue(false);
                    return null;
                });
    }

    public LiveData<List<Book>> getBooks() {
        return books;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public void refreshBooks() {
        loadBooks();
    }
}