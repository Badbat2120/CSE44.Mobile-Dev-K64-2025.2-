package com.example.litera.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.litera.models.Author;
import com.example.litera.models.Book;
import com.example.litera.repositories.AuthorRepository;
import com.example.litera.repositories.BookRepository;

import java.util.concurrent.CompletableFuture;

public class BookDetailViewModel extends ViewModel {

    private final BookRepository bookRepository;
    private final AuthorRepository authorRepository;
    private final MutableLiveData<Book> selectedBook = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

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

        bookRepository.getBookById(bookId)
                .thenAccept(book -> {
                    if (book != null) {
                        // Lấy thông tin author
                        String authorId = book.getAuthorId(); // Chú ý: dùng getAuthorId() thay vì getAuthor()
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
}