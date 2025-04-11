package com.example.litera.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.litera.models.Author;
import com.example.litera.models.Book;
import com.example.litera.repositories.BookRepository;

import java.util.ArrayList;
import java.util.List;

public class MainViewModel extends ViewModel {

    private final MutableLiveData<List<Book>> trendingBooks = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<Book>> continueReadingBooks = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<Author>> popularAuthors = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    private final BookRepository bookRepository;

    public MainViewModel() {
        bookRepository = BookRepository.getInstance();

        // Load data from Firebase
        loadTrendingBooks();
        loadContinueReadingBooks();
        loadPopularAuthors();
    }

    private void loadTrendingBooks() {
        isLoading.setValue(true);
        bookRepository.getTrendingBooks()
                .thenAccept(books -> {
                    trendingBooks.postValue(books);
                    isLoading.postValue(false);
                })
                .exceptionally(e -> {
                    errorMessage.postValue("Failed to load trending books: " + e.getMessage());
                    isLoading.postValue(false);
                    return null;
                });
    }

    private void loadContinueReadingBooks() {
        bookRepository.getContinueReadingBooks()
                .thenAccept(books -> {
                    continueReadingBooks.postValue(books);
                })
                .exceptionally(e -> {
                    errorMessage.postValue("Failed to load continue reading books: " + e.getMessage());
                    return null;
                });
    }

    private void loadPopularAuthors() {
        bookRepository.getPopularAuthors()
                .thenAccept(authors -> {
                    popularAuthors.postValue(authors);
                })
                .exceptionally(e -> {
                    errorMessage.postValue("Failed to load popular authors: " + e.getMessage());
                    return null;
                });
    }

    public LiveData<List<Book>> getTrendingBooks() {
        return trendingBooks;
    }

    public LiveData<List<Book>> getContinueReadingBooks() {
        return continueReadingBooks;
    }

    public LiveData<List<Author>> getPopularAuthors() {
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
        loadTrendingBooks();
        loadContinueReadingBooks();
        loadPopularAuthors();
    }
}