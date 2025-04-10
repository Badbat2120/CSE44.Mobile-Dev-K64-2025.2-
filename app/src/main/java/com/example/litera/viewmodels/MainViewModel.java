package com.example.litera.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.litera.models.Author;
import com.example.litera.models.Book;
import com.example.litera.repositories.BookRepository;

import java.util.List;

public class MainViewModel extends ViewModel {

    private final MutableLiveData<List<Book>> trendingBooks = new MutableLiveData<>();
    private final MutableLiveData<List<Book>> continueReadingBooks = new MutableLiveData<>();
    private final MutableLiveData<List<Author>> popularAuthors = new MutableLiveData<>();
    private final BookRepository bookRepository;

    public MainViewModel() {
        bookRepository = BookRepository.getInstance();

        // Load trending books
        trendingBooks.setValue(bookRepository.getTrendingBooks());

        // Load continue reading books
        continueReadingBooks.setValue(bookRepository.getContinueReadingBooks());

        // Load popular authors
        popularAuthors.setValue(bookRepository.getPopularAuthors());
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
}