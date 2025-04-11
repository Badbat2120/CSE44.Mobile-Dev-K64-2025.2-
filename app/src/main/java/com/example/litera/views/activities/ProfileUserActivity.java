package com.example.litera.views.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.example.litera.R;

public class ProfileUserActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_user);

        ImageButton btnBack = findViewById(R.id.btnBack);
        Button btnFavourites = findViewById(R.id.btnFavourites);
        Button btnChangePass = findViewById(R.id.btnChangePass);

        // Navigate back to MainActivity
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ProfileUserActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
            }
        });

        // Navigate to FavBookActivity
        btnFavourites.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ProfileUserActivity.this, FavBookActivity.class);
                startActivity(intent);
            }
        });

        // Navigate to ChangePassActivity
        btnChangePass.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ProfileUserActivity.this, ChangePassActivity.class);
                startActivity(intent);
            }
        });
    }
}