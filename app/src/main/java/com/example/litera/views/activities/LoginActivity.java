package com.example.litera.views.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.litera.R;
import com.example.litera.repositories.UserRepository;
import com.example.litera.viewmodels.UserViewModel;
import com.example.litera.viewmodels.ViewModelFactory;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;

public class LoginActivity extends AppCompatActivity {
    private TextInputEditText inputEmail, inputPassword;
    private FirebaseAuth auth;
    private UserRepository userRepository;
    private ProgressBar progressBar;
    private Button btnLogin;
    private TextView tvRegister, tvForgotPassword;
    private UserViewModel userViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance();
        userRepository = new UserRepository();

        // Initialize views
        inputEmail = findViewById(R.id.etEmail);
        inputPassword = findViewById(R.id.etPassword);
        progressBar = findViewById(R.id.progressBar);
        btnLogin = findViewById(R.id.btnLogin);
        tvRegister = findViewById(R.id.tvRegister);

        userViewModel = new ViewModelProvider(this, new ViewModelFactory(userRepository)).get(UserViewModel.class);
        userViewModel.getIsLoading().observe(this, isLoading -> {
            if (isLoading) {
                progressBar.setVisibility(View.VISIBLE);
            } else {
                progressBar.setVisibility(View.GONE);
            }
        });
        userViewModel.getErrorMessage().observe(this, errorMessage -> {
            if (errorMessage != null) {
                Toast.makeText(LoginActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
        userViewModel.getUserLiveData().observe(this, user -> {
            if (user != null) {
                // Đăng nhập thành công, chuyển đến MainActivity
                startActivity(new Intent(LoginActivity.this, MainActivity.class));
                finish();
            }
        });

//        // Kiểm tra nếu người dùng đã đăng nhập
//        if (auth.getCurrentUser() != null) {
//            startActivity(new Intent(LoginActivity.this, MainActivity.class));
//            finish();
//        }

        // Handle login button click
        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = inputEmail.getText().toString().trim();
                String password = inputPassword.getText().toString().trim();

                if (TextUtils.isEmpty(email)) {
                    Toast.makeText(getApplicationContext(), "Enter email address!", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (TextUtils.isEmpty(password)) {
                    Toast.makeText(getApplicationContext(), "Enter password!", Toast.LENGTH_SHORT).show();
                    return;
                }

                userViewModel.login(email, password);

//                progressBar.setVisibility(View.VISIBLE);
//
//                // Authenticate user
//                auth.signInWithEmailAndPassword(email, password)
//                        .addOnCompleteListener(LoginActivity.this, new OnCompleteListener<AuthResult>() {
//                            @Override
//                            public void onComplete(@NonNull Task<AuthResult> task) {
//                                progressBar.setVisibility(View.GONE);
//                                if (task.isSuccessful()) {
//                                    // Đăng nhập thành công, kiểm tra xem người dùng có trong Firestore không
//                                    FirebaseUser firebaseUser = auth.getCurrentUser();
//                                    if (firebaseUser != null) {
//                                        userRepository.getUserByEmail(email, new UserRepository.OnUserFetchListener() {
//                                            @Override
//                                            public void onSuccess(com.example.litera.models.User user) {
//                                                // Người dùng đã có trong Firestore, chuyển đến MainActivity
//                                                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
//                                                startActivity(intent);
//                                                finish();
//                                            }
//
//                                            @Override
//                                            public void onFailure(String error) {
//                                                // Người dùng chỉ có trong Auth nhưng không có trong Firestore
//                                                // Tạo profile trong Firestore
//                                                String name = email.split("@")[0]; // Tạm thời lấy tên từ email
//                                                userRepository.createUser(name, email, new UserRepository.OnUserCreationListener() {
//                                                    @Override
//                                                    public void onSuccess() {
//                                                        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
//                                                        startActivity(intent);
//                                                        finish();
//                                                    }
//
//                                                    @Override
//                                                    public void onFailure(String error) {
//                                                        Toast.makeText(LoginActivity.this, "Error creating user profile: " + error, Toast.LENGTH_SHORT).show();
//                                                    }
//                                                });
//                                            }
//                                        });
//                                    }
//                                } else {
//                                    // Đăng nhập thất bại
//                                    Toast.makeText(LoginActivity.this, "Authentication failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
//                                }
//                            }
//                        });
            }
        });

        // Handle "Sign up" text click
        SpannableString ss = new SpannableString("Don't have an account? Sign up");
        ClickableSpan clickableSpan = new ClickableSpan() {
            @Override
            public void onClick(View textView) {
                startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
            }
        };
        ss.setSpan(clickableSpan, 23, 30, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        tvRegister.setText(ss);
        tvRegister.setMovementMethod(LinkMovementMethod.getInstance());

        // Handle "Forgot Password" text click if available
        if (tvForgotPassword != null) {
            tvForgotPassword.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String email = inputEmail.getText().toString().trim();
                    if (TextUtils.isEmpty(email)) {
                        Toast.makeText(getApplicationContext(), "Enter your email to reset password", Toast.LENGTH_SHORT).show();
                    } else {
                        resetPassword(email);
                    }
                }
            });
        }
    }

    private void resetPassword(String email) {
        progressBar.setVisibility(View.VISIBLE);
        auth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    progressBar.setVisibility(View.GONE);
                    if (task.isSuccessful()) {
                        Toast.makeText(LoginActivity.this, "Password reset email sent", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(LoginActivity.this, "Failed to send reset email: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
}