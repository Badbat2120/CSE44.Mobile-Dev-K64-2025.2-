package com.example.litera.views.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import androidx.appcompat.app.AppCompatActivity;

import com.example.litera.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class SplashActivity extends AppCompatActivity {
    private static final int SPLASH_TIME_OUT = 2000; // 2 seconds

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                startActivity(new Intent(SplashActivity.this, MainActivity.class));
//                // Check if user is signed in
//                FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
//                if (currentUser != null) {
//                    // User is signed in, go to MainActivity
//                    startActivity(new Intent(SplashActivity.this, MainActivity.class));
//                } else {
//                    // No user is signed in, go to LoginActivity
//                    startActivity(new Intent(SplashActivity.this, LoginActivity.class));
//                }
                finish();
            }
        }, SPLASH_TIME_OUT);
    }
}