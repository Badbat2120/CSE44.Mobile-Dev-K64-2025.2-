package com.example.litera.views.activities.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.litera.views.activities.model.Book;
import com.example.litera.views.activities.repository.BookRepository;

public class BookDetailViewModel extends ViewModel {

    private final MutableLiveData<Book> selectedBook = new MutableLiveData<>();
    private final BookRepository bookRepository;

    public BookDetailViewModel() {
        // Initialize repository
        bookRepository = BookRepository.getInstance();
    }

    public LiveData<Book> getSelectedBook() {
        return selectedBook;
    }

    public void selectBook(String bookId) {
        // Fetch book by ID from the repository
        Book book = bookRepository.getBooks().stream()
                .filter(b -> b.getId().equals(bookId))
                .findFirst()
                .orElse(null);
        selectedBook.setValue(book);
    }
}
