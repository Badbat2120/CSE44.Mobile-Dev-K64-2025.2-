package com.example.litera.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.litera.models.Book;
import com.example.litera.repositories.BookRepository;

import java.util.List;

public class BookViewModel extends ViewModel {

    private final MutableLiveData<List<Book>> books = new MutableLiveData<>();
    private final BookRepository bookRepository;

    public BookViewModel() {
        // Initialize repository using Singleton pattern
        bookRepository = BookRepository.getInstance();
        books.setValue(bookRepository.getBooks());
    }

    public LiveData<List<Book>> getBooks() {
        return books;
    }
}