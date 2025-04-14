package com.example.litera.views.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.View;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.litera.R;
import com.example.litera.repositories.UserRepository;
import com.example.litera.viewmodels.ProfileUserViewModel;
import com.example.litera.viewmodels.ViewModelFactory;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;

public class RegisterActivity extends AppCompatActivity {

    private TextInputEditText inputName, inputEmail, inputPassword;
    private FirebaseAuth auth;
    private UserRepository userRepository;
    private ProgressBar progressBar;
    private Button btnSignUp;
    private TextView tvLogin;
    private ProfileUserViewModel profileUserViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // Khởi tạo Firebase Auth và UserRepository
        auth = FirebaseAuth.getInstance();
        userRepository = new UserRepository();

        // Liên kết các thành phần giao diện người dùng
        inputName = findViewById(R.id.etName); // Thêm TextInputEditText cho tên
        inputEmail = findViewById(R.id.etEmail);
        inputPassword = findViewById(R.id.etPassword);
        progressBar = findViewById(R.id.progressBar);
        btnSignUp = findViewById(R.id.btnRegister);
        tvLogin = findViewById(R.id.tvLogin);

        profileUserViewModel = new ViewModelProvider(this, new ViewModelFactory(userRepository)).get(ProfileUserViewModel.class);

        profileUserViewModel.getIsLoading().observe(this, isLoading -> {
            if (isLoading) {
                progressBar.setVisibility(View.VISIBLE);
            } else {
                progressBar.setVisibility(View.GONE);
            }
        });
        profileUserViewModel.getUserLiveData().observe(this, user -> {
            if (user != null) {
                // Đăng nhập thành công, chuyển đến MainActivity
                startActivity(new Intent(RegisterActivity.this, MainActivity.class));
                finish();
            }
        });
        profileUserViewModel.getErrorMessage().observe(this, errorMessage -> {
            if (errorMessage != null) {
                // Hiển thị thông báo lỗi
                Toast.makeText(RegisterActivity.this, errorMessage, Toast.LENGTH_LONG).show();
            }
        });

        btnSignUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String name = inputName.getText().toString().trim();
                String email = inputEmail.getText().toString().trim();
                String password = inputPassword.getText().toString().trim();

                // Kiểm tra đầu vào
                if (TextUtils.isEmpty(name)) {
                    Toast.makeText(getApplicationContext(), "Enter your name!", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (TextUtils.isEmpty(email)) {
                    Toast.makeText(getApplicationContext(), "Enter email address!", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (TextUtils.isEmpty(password)) {
                    Toast.makeText(getApplicationContext(), "Enter password!", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (password.length() < 6) {
                    Toast.makeText(getApplicationContext(), "Password too short, enter minimum 6 characters!", Toast.LENGTH_SHORT).show();
                    return;
                }
                // Đăng ký người dùng mới
                profileUserViewModel.register(email, password, name);

//                progressBar.setVisibility(View.VISIBLE);
//
//                // Tạo người dùng với Firebase Authentication
//                auth.createUserWithEmailAndPassword(email, password)
//                        .addOnCompleteListener(RegisterActivity.this, new OnCompleteListener<AuthResult>() {
//                            @Override
//                            public void onComplete(@NonNull Task<AuthResult> task) {
//                                if (task.isSuccessful()) {
//                                    // Tạo tài liệu người dùng trong Firestore
//                                    userRepository.createUser(name, email, new UserRepository.OnUserCreationListener() {
//                                        @Override
//                                        public void onSuccess() {
//                                            progressBar.setVisibility(View.GONE);
//                                            Toast.makeText(RegisterActivity.this, "Registration successful!", Toast.LENGTH_SHORT).show();
//                                            startActivity(new Intent(RegisterActivity.this, MainActivity.class));
//                                            finish();
//                                        }
//
//                                        @Override
//                                        public void onFailure(String error) {
//                                            progressBar.setVisibility(View.GONE);
//                                            Toast.makeText(RegisterActivity.this, "Failed to create user profile: " + error, Toast.LENGTH_LONG).show();
//                                        }
//                                    });
//                                } else {
//                                    progressBar.setVisibility(View.GONE);
//                                    Toast.makeText(RegisterActivity.this, "Registration failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
//                                }
//                            }
//                        });
            }
        });

        // Thiết lập chữ "Login" có thể nhấp được
        SpannableString ss = new SpannableString("Already have an account? Login");

        ClickableSpan clickableSpan = new ClickableSpan() {
            @Override
            public void onClick(View widget) {
                startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
                finish();
            }
        };

        ss.setSpan(clickableSpan, 25, 30, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE); // chỉ chữ "Login"
        tvLogin.setText(ss);
        tvLogin.setMovementMethod(LinkMovementMethod.getInstance());
    }
}