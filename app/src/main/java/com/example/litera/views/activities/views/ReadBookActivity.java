package com.example.litera.views.activities.views;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.litera.R;
import com.example.litera.views.activities.viewmodel.ReadBookViewModel;

public class ReadBookActivity extends AppCompatActivity {

    private ReadBookViewModel readBookViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rbook);

        // Initialize UI elements
        TextView bookContent = findViewById(R.id.bookContent);

        // Initialize ViewModel
        readBookViewModel = new ViewModelProvider(this).get(ReadBookViewModel.class);

        // Observe LiveData
        readBookViewModel.getBookContent().observe(this, content -> {
            bookContent.setText(content);
        });

        // Set mock book content
        readBookViewModel.loadBookContent("Book content goes here...");
    }
}