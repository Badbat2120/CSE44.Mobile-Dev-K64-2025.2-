package com.example.litera.views.activities.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.litera.views.activities.model.Author;
import com.example.litera.views.activities.model.Book;

import java.util.ArrayList;
import java.util.List;

public class BookRepository {

    private static BookRepository instance;

    private BookRepository() {
    }

    public static synchronized BookRepository getInstance() {
        if (instance == null) {
            instance = new BookRepository();
        }
        return instance;
    }

    // Mock data for books
    public List<Book> getBooks() {
        List<Book> books = new ArrayList<>();
        books.add(new Book("1", "Book 1", "Author 1", "Description 1", ""));
        books.add(new Book("2", "Book 2", "Author 2", "Description 2", ""));
        books.add(new Book("3", "Book 3", "Author 3", "Description 3", ""));
        return books;
    }

    // Mock data for trending books
    public List<Book> getTrendingBooks() {
        List<Book> trendingBooks = new ArrayList<>();
        trendingBooks.add(new Book("1", "Trending Book 1", "Author 1", "Description 1", ""));
        trendingBooks.add(new Book("2", "Trending Book 2", "Author 2", "Description 2", ""));
        trendingBooks.add(new Book("3", "Trending Book 3", "Author 3", "Description 3", ""));
        return trendingBooks;
    }

    public List<Book> getContinueReadingBooks() {
        List<Book> books = new ArrayList<>();
        books.add(new Book("4", "Continue Reading 1", "Author 4", "Description 4", ""));
        books.add(new Book("5", "Continue Reading 2", "Author 5", "Description 5", ""));
        books.add(new Book("6", "Continue Reading 3", "Author 6", "Description 6", ""));
        return books;
    }

    // Mock data for popular authors
    public List<Author> getPopularAuthors() {
        List<Author> authors = new ArrayList<>();
        authors.add(new Author("Author 1", "https://example.com/author1.jpg"));
        authors.add(new Author("Author 2", "https://example.com/author2.jpg"));
        authors.add(new Author("Author 3", "https://example.com/author3.jpg"));
        return authors;
    }
}