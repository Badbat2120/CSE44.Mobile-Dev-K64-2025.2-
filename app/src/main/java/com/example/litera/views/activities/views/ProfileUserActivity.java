package com.example.litera.views.activities.views;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.litera.R;
import com.example.litera.views.activities.viewmodel.ProfileUserViewModel;

public class ProfileUserActivity extends AppCompatActivity {

    private ProfileUserViewModel profileUserViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_user);

        // Initialize UI elements
        TextView userName = findViewById(R.id.userName);
        TextView userEmail = findViewById(R.id.userEmail);

        // Initialize ViewModel
        profileUserViewModel = new ViewModelProvider(this).get(ProfileUserViewModel.class);

        // Observe LiveData
        profileUserViewModel.getUser().observe(this, user -> {
            if (user != null) {
                userName.setText(user.getName());
                userEmail.setText(user.getEmail());
            }
        });

        // Set mock user data
        profileUserViewModel.loadUser(); // Load user data
    }
}