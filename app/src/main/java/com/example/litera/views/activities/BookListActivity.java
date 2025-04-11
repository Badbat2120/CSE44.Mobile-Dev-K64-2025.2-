package com.example.litera.views.activities;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.example.litera.R;

public class BookListActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.item_book);
    }
}