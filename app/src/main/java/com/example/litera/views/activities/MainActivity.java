package com.example.litera.views.activities;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.litera.R;
import com.example.litera.models.Author;
import com.example.litera.models.Book;
import com.example.litera.repositories.BookRepository;
import com.example.litera.repositories.UserRepository;
import com.example.litera.viewmodels.UserViewModel;
import com.example.litera.viewmodels.ViewModelFactory;
import com.example.litera.views.adapters.AuthorAdapter;
import com.example.litera.views.adapters.BookAdapter;
import com.example.litera.viewmodels.MainViewModel;

import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements AuthorAdapter.OnAuthorClickListener {
    private static final String TAG = "MainActivity";

    private ImageView imgProfile;
    private ImageView imgSlideShow;

    private int[] avatarImages = {
            R.drawable.slide_show_1,
            R.drawable.slide_show_4,
            R.drawable.slide_show_2,
            R.drawable.slide_show_3,
            R.drawable.slide_show_5,
    };
    private Handler avatarHandler = new Handler();
    private Runnable avatarRunnable;
    private TextView tvHello;
    private EditText etSearch;
    private RecyclerView rvContinueReading, rvTrendingBooks, rvPopularAuthors, rvAllBooks;
    private TabLayout tabGenres;
    private MainViewModel mainViewModel;
    private ProgressBar progressBar;

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    // Khai báo adapter ở mức class để có thể sử dụng trong các phương thức khác
    private BookAdapter trendingBooksAdapter;
    private BookAdapter continueReadingAdapter;
    private BookAdapter allBooksAdapter;
    private AuthorAdapter popularAuthorAdapter;
    private UserViewModel userViewModel;
    private UserRepository userRepository;

    // Các danh sách để lưu trữ dữ liệu sách sau khi tìm kiếm
    private List<Book> allBooks = new ArrayList<>();
    private List<Book> trendingBooks = new ArrayList<>();
    private List<Book> continueReadingBooks = new ArrayList<>();
    private List<Author> popularAuthors = new ArrayList<>();

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Khởi tạo Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Khởi tạo UI
        imgSlideShow = findViewById(R.id.imgSlideShow);
        tvHello = findViewById(R.id.tvHello);
        etSearch = findViewById(R.id.etSearch);
        rvContinueReading = findViewById(R.id.rvContinueReading);
        rvTrendingBooks = findViewById(R.id.rvTrendingBooks);
        rvPopularAuthors = findViewById(R.id.rvPopularAuthors);
        rvAllBooks = findViewById(R.id.rvAllBooks);
        tabGenres = findViewById(R.id.tabGenres);
        TextView tvViewAllAuthors = findViewById(R.id.tvViewAllAuthors);
        TextView tvViewAllContinueReading = findViewById(R.id.tvViewAllContinueReading);
        TextView tvViewAllTrendingBooks = findViewById(R.id.tvViewAllTrendingBooks);
        TextView tvViewAllBooks = findViewById(R.id.tvViewAllBooks);

        userRepository = new UserRepository();
        userViewModel = new ViewModelProvider(this, new ViewModelFactory(userRepository)).get(UserViewModel.class);

        userViewModel.getUserLiveData().observe(this, user -> {
            if (user != null) {
                tvHello.setText("Hello " + user.getName());
                Glide.with(MainActivity.this)
                        .load(user.getAvatar())
                        .placeholder(R.drawable.placeholder)
                        .into(imgProfile);
            } else {
                tvHello.setText("Hello Guest");
            }
        });

//        profileUserViewModel.fetchCurrentUser();

        // Hiển thị tên người dùng
//        loadUserNameAndDisplay();

        // Handle profile click
        imgProfile = findViewById(R.id.imgProfile);
        imgProfile.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ProfileUserActivity.class);
            startActivity(intent);
        });

        // Khởi tạo Runnable trước khi gọi post
        avatarRunnable = new Runnable() {
            @Override
            public void run() {
                // Chọn ảnh ngẫu nhiên từ mảng
                int randomIndex = (int) (Math.random() * avatarImages.length);
                imgSlideShow.setImageResource(avatarImages[randomIndex]);

                // Gọi lại sau 1.5 giây (1500ms)
                avatarHandler.postDelayed(this, 1500);
            }
        };
        // Bắt đầu slideshow
        avatarHandler.post(avatarRunnable);

        // Khởi tạo ViewModel
        mainViewModel = new ViewModelProvider(this).get(MainViewModel.class);

        // Setup adapters
        setupAdapters();

        // Handle navigation for view all buttons
        setupNavigationListeners(tvViewAllAuthors, tvViewAllContinueReading, tvViewAllTrendingBooks, tvViewAllBooks);

        // Observe ViewModel data
        observeViewModel();

        // Lắng nghe sự kiện thay đổi trong EditText tìm kiếm
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence charSequence, int start, int before, int count) {
                filterData(charSequence.toString());
            }

            @Override
            public void afterTextChanged(Editable editable) {}
        });

        // Khi quay lại màn hình, tải lại dữ liệu sách để có thông tin rating mới nhất
        String bookId = getIntent().getStringExtra("bookId");
        if (bookId != null) {
            // Xóa cache trước khi tải lại
            BookRepository.getInstance().clearCache();

            // Khắc phục vấn đề không nhất quán
            BookRepository.getInstance().fixRatingCountInconsistencies();
            BookRepository.getInstance().clearCache();

            // Get bookId from intent
            bookId = getIntent().getStringExtra("bookId");

            // Debug rating system
            if (bookId != null && !bookId.isEmpty()) {
                BookRepository.getInstance().debugRatingSystem(bookId);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        userViewModel.fetchCurrentUser();

        // Tải lại tên người dùng mỗi khi quay lại MainActivity
//        loadUserNameAndDisplay();

        // Làm mới dữ liệu sách
        if (mainViewModel != null) {
            BookRepository.getInstance().fixRatingCountInconsistencies();
            BookRepository.getInstance().clearCache();
            // Xóa cache để đảm bảo lấy dữ liệu mới nhất từ Firestore
            mainViewModel.clearCache();

            // Tải lại tất cả dữ liệu
            mainViewModel.refreshData();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        avatarHandler.removeCallbacks(avatarRunnable); // Ngưng gọi khi activity bị huỷ
    }


    @SuppressLint("SetTextI18n")
    private void loadUserNameAndDisplay() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String email = currentUser.getEmail();

            // Tìm document chứa email này trong collection users
            db.collection("users")
                    .whereEqualTo("email", email)
                    .limit(1)
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful() && !task.getResult().isEmpty()) {
                            // Lấy document đầu tiên
                            String name = task.getResult().getDocuments().get(0).getString("name");
                            if (name != null && !name.isEmpty()) {
                                tvHello.setText("Hello " + name);
                            } else {
                                // Sử dụng phần đầu của email làm tên mặc định
                                String defaultName = email.split("@")[0];
                                tvHello.setText("Hello " + defaultName);
                            }
                        } else {
                            Log.w(TAG, "Error getting user document", task.getException());
                            // Hiển thị thông báo chào mặc định
                            tvHello.setText("Hello User");
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error querying users collection", e);
                        tvHello.setText("Hello User");
                    });
        } else {
            // Không có người dùng đăng nhập
            tvHello.setText("Hello User");
        }
    }

    private void setupAdapters() {
        // Setup RecyclerView for Trending Books
        trendingBooksAdapter = new BookAdapter(BookAdapter.VIEW_TYPE_TRENDING, mainViewModel, this);
        rvTrendingBooks.setAdapter(trendingBooksAdapter);
        rvTrendingBooks.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));

        // Setup RecyclerView for Continue Reading
        continueReadingAdapter = new BookAdapter(BookAdapter.VIEW_TYPE_CONTINUE_READING, mainViewModel, this);
        rvContinueReading.setAdapter(continueReadingAdapter);
        rvContinueReading.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));

        // Setup RecyclerView for All Books
        allBooksAdapter = new BookAdapter(BookAdapter.VIEW_TYPE_GRID, mainViewModel, this);
        GridLayoutManager gridLayoutManager = new GridLayoutManager(this, 3);
        rvAllBooks.setLayoutManager(gridLayoutManager);
        rvAllBooks.setAdapter(allBooksAdapter);

        // Setup RecyclerView for Popular Authors
        popularAuthorAdapter = new AuthorAdapter(this);
        rvPopularAuthors.setAdapter(popularAuthorAdapter);
        rvPopularAuthors.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
    }

    private void setupNavigationListeners(TextView viewAllAuthors, TextView viewAllContinueReading,
                                          TextView viewAllTrendingBooks, TextView viewAllBooks) {
        // Handle navigation
        viewAllAuthors.setOnClickListener(v -> {
            // Navigate to AuthorListActivity
            Intent intent = new Intent(MainActivity.this, AuthorListActivity.class);
            startActivity(intent);
        });

        viewAllContinueReading.setOnClickListener(v -> {
            // Navigate to BookListActivity with continue reading mode
            Intent intent = new Intent(MainActivity.this, BookListActivity.class);
            intent.putExtra(BookListActivity.EXTRA_DISPLAY_MODE, BookListActivity.DISPLAY_CONTINUE_READING);
            startActivity(intent);
        });

        viewAllTrendingBooks.setOnClickListener(v -> {
            // Navigate to BookListActivity with trending books mode
            Intent intent = new Intent(MainActivity.this, BookListActivity.class);
            intent.putExtra(BookListActivity.EXTRA_DISPLAY_MODE, BookListActivity.DISPLAY_TRENDING_BOOKS);
            startActivity(intent);
        });

        viewAllBooks.setOnClickListener(v -> {
            // Navigate to BookListActivity with all books mode
            Intent intent = new Intent(MainActivity.this, BookListActivity.class);
            intent.putExtra(BookListActivity.EXTRA_DISPLAY_MODE, BookListActivity.DISPLAY_ALL_BOOKS);
            startActivity(intent);
        });
    }

    private void observeViewModel() {
        // Lấy dữ liệu từ ViewModel cho Continue Reading và Trending Books
        mainViewModel.getContinueReadingBooks().observe(this, books -> {
            if (books != null && !books.isEmpty()) {
                continueReadingAdapter.submitList(books);
                continueReadingBooks.clear();
                continueReadingBooks.addAll(books);
            }
        });

        mainViewModel.getTrendingBooks().observe(this, books -> {
            if (books != null && !books.isEmpty()) {
                trendingBooksAdapter.submitList(books);
                trendingBooks.clear();
                trendingBooks.addAll(books);
            }
        });

        // Observe ALL books
        mainViewModel.getAllBooks().observe(this, books -> {
            if (books != null && !books.isEmpty()) {
                // Lưu tất cả sách vào danh sách
                allBooks.clear();
                allBooks.addAll(books);
                filterData(etSearch.getText().toString());  // Lọc ngay nếu có tìm kiếm
            }
        });

        // Observe popular authors
        mainViewModel.getPopularAuthors().observe(this, authors -> {
            if (authors != null) {
                popularAuthorAdapter.submitList(authors);
                popularAuthors.clear();
                popularAuthors.addAll(authors);
            }
        });

        // Observe loading state
        mainViewModel.getIsLoading().observe(this, isLoading -> {
            if (progressBar != null) {
                progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            }
        });

        // Observe error messages
        mainViewModel.getErrorMessage().observe(this, error -> {
            if (error != null && !error.isEmpty()) {
                Toast.makeText(MainActivity.this, error, Toast.LENGTH_LONG).show();
            }
        });
    }

    // Lọc dữ liệu theo từ khóa tìm kiếm
    private void filterData(String query) {
        // Lọc dữ liệu cho từng danh sách sách
        List<Book> filteredTrendingBooks = new ArrayList<>();
        List<Book> filteredContinueReadingBooks = new ArrayList<>();
        List<Book> filteredAllBooks = new ArrayList<>();
        List<Author> filteredPopularAuthors = new ArrayList<>(); // Lọc tác giả

        // Lọc sách Trending
        for (Book book : trendingBooks) {
            if (book.getTitle().toLowerCase().contains(query.toLowerCase()) ||
                    book.getAuthor().getName().toLowerCase().contains(query.toLowerCase())) {
                filteredTrendingBooks.add(book);
            }
        }

        // Lọc sách Continue Reading
        for (Book book : continueReadingBooks) {
            if (book.getTitle().toLowerCase().contains(query.toLowerCase()) ||
                    book.getAuthor().getName().toLowerCase().contains(query.toLowerCase())) {
                filteredContinueReadingBooks.add(book);
            }
        }

        // Lọc sách All Books
        for (Book book : allBooks) {
            if (book.getTitle().toLowerCase().contains(query.toLowerCase()) ||
                    book.getAuthor().getName().toLowerCase().contains(query.toLowerCase())) {
                filteredAllBooks.add(book);
            }
        }

        // Lọc tác giả
        for (Author author : popularAuthors) {
            if (author.getName().toLowerCase().contains(query.toLowerCase())) {
                filteredPopularAuthors.add(author);
            }
        }

        // Cập nhật adapter cho từng RecyclerView với dữ liệu đã lọc
        trendingBooksAdapter.submitList(filteredTrendingBooks);
        continueReadingAdapter.submitList(filteredContinueReadingBooks);
        allBooksAdapter.submitList(filteredAllBooks);
        popularAuthorAdapter.submitList(filteredPopularAuthors);  // Cập nhật danh sách tác giả đã lọc
    }

    @Override
    public void onAuthorClick(Author author) {
        // Xử lý khi người dùng click vào tác giả
        if (author != null) {
            // Điều hướng đến AuthorDetailActivity
            Intent intent = new Intent(this, AuthorDetailActivity.class);
            intent.putExtra("authorId", author.getId()); // Giả sử Author có phương thức getId()
            intent.putExtra("authorName", author.getName());
            startActivity(intent);
        } else {
            Toast.makeText(this, "Author data is null", Toast.LENGTH_SHORT).show();
        }
    }
}
