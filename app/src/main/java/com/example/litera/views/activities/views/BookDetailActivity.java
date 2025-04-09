package com.example.litera.views.activities.views;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.litera.R;
import com.example.litera.views.activities.viewmodel.BookDetailViewModel;

public class BookDetailActivity extends AppCompatActivity {

    private BookDetailViewModel bookDetailViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_book_detail);

        // Initialize UI elements
        ImageView bookCover = findViewById(R.id.bookCover);
        TextView bookTitle = findViewById(R.id.bookTitle);
        TextView bookAuthor = findViewById(R.id.bookAuthor);
        TextView bookDescription = findViewById(R.id.description);

        // Initialize ViewModel
        bookDetailViewModel = new ViewModelProvider(this).get(BookDetailViewModel.class);

        // Observe LiveData
        bookDetailViewModel.getSelectedBook().observe(this, book -> {
            if (book != null) {
                bookTitle.setText(book.getTitle());
                bookAuthor.setText(book.getAuthor());
                bookDescription.setText(book.getDescription());
                // Load book cover image using Glide or Picasso
                // Glide.with(this).load(book.getImageUrl()).into(bookCover);
            }
        });

        // Set mock data (you can replace this with navigation arguments or intent extras)
        bookDetailViewModel.selectBook("1"); // Pass the book ID
    }
}