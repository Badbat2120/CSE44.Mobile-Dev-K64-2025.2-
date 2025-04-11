package com.example.litera.views.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.litera.R;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

public class TopUpWalletActivity extends AppCompatActivity {
    private EditText etAmount;
    private Button btnTopUp;
    private ImageButton btnBack;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_top_up_wallet);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // Initialize UI components
        etAmount = findViewById(R.id.etAmount);
        btnTopUp = findViewById(R.id.btnTopUp);
        btnBack = findViewById(R.id.btnBack);

        // Setup click listeners
        btnBack.setOnClickListener(v -> finish());
        btnTopUp.setOnClickListener(v -> topUpWallet());
    }

    private void topUpWallet() {
        String amountStr = etAmount.getText().toString().trim();
        if (amountStr.isEmpty()) {
            Toast.makeText(this, "Please enter amount", Toast.LENGTH_SHORT).show();
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(amountStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Please enter a valid amount", Toast.LENGTH_SHORT).show();
            return;
        }

        // Get current user
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String email = currentUser.getEmail();

            // Find user document
            db.collection("users")
                    .whereEqualTo("email", email)
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        if (!queryDocumentSnapshots.isEmpty()) {
                            DocumentSnapshot documentSnapshot = queryDocumentSnapshots.getDocuments().get(0);
                            DocumentReference userRef = documentSnapshot.getReference();

                            // Get current value
                            String currentValueStr = documentSnapshot.getString("value");
                            double currentValue = 0;
                            if (currentValueStr != null && !currentValueStr.isEmpty()) {
                                // Remove $ sign if present
                                if (currentValueStr.startsWith("$")) {
                                    currentValueStr = currentValueStr.substring(1);
                                }
                                try {
                                    currentValue = Double.parseDouble(currentValueStr);
                                } catch (NumberFormatException e) {
                                    // If parsing fails, keep currentValue as 0
                                }
                            }

                            // Add new amount
                            double newValue = currentValue + amount;
                            String newValueStr = String.format("%.2f", newValue);

                            // Update value in Firestore
                            userRef.update("value", newValueStr)
                                    .addOnSuccessListener(aVoid -> {
                                        Toast.makeText(TopUpWalletActivity.this,
                                                "Successfully topped up $" + String.format("%.2f", amount),
                                                Toast.LENGTH_SHORT).show();
                                        finish();
                                    })
                                    .addOnFailureListener(e -> {
                                        Toast.makeText(TopUpWalletActivity.this,
                                                "Failed to top up: " + e.getMessage(),
                                                Toast.LENGTH_SHORT).show();
                                    });
                        } else {
                            Toast.makeText(TopUpWalletActivity.this,
                                    "User not found", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(TopUpWalletActivity.this,
                                "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        }
    }
}