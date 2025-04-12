package com.example.litera.views.adapters;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.lifecycle.LifecycleOwner;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.litera.R;
import com.example.litera.models.Book;
import com.example.litera.utils.GoogleDriveUtils;
import com.example.litera.viewmodels.MainViewModel;
import com.example.litera.views.activities.BookDetailActivity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BookAdapter extends RecyclerView.Adapter<BookAdapter.BookViewHolder> {
    private static final String TAG = "BookAdapter";

    // Constants for view types
    public static final int VIEW_TYPE_CONTINUE_READING = 0;
    public static final int VIEW_TYPE_TRENDING = 1;
    public static final int VIEW_TYPE_GRID = 2;

    private final List<Book> books = new ArrayList<>();
    private boolean isTrendingView;
    private MainViewModel viewModel;
    private LifecycleOwner lifecycleOwner;
    private int viewType = VIEW_TYPE_CONTINUE_READING;

    // Cache tên tác giả để tránh truy vấn nhiều lần
    private final Map<String, String> authorNameCache = new HashMap<>();

    // Constructor mặc định (không tham số)
    public BookAdapter() {
        this.isTrendingView = false; // Mặc định không phải là trending view
        this.viewType = VIEW_TYPE_CONTINUE_READING;
    }

    // Constructor mới nhận tham số boolean
    public BookAdapter(boolean isTrendingView) {
        this.isTrendingView = isTrendingView;
        this.viewType = isTrendingView ? VIEW_TYPE_TRENDING : VIEW_TYPE_CONTINUE_READING;
    }

    // Constructor nhận ViewModel và LifecycleOwner
    public BookAdapter(boolean isTrendingView, MainViewModel viewModel, LifecycleOwner lifecycleOwner) {
        this.isTrendingView = isTrendingView;
        this.viewType = isTrendingView ? VIEW_TYPE_TRENDING : VIEW_TYPE_CONTINUE_READING;
        this.viewModel = viewModel;
        this.lifecycleOwner = lifecycleOwner;
    }

    // Constructor for grid view
    public BookAdapter(int viewType, MainViewModel viewModel, LifecycleOwner lifecycleOwner) {
        this.viewType = viewType;
        this.isTrendingView = (viewType == VIEW_TYPE_TRENDING);
        this.viewModel = viewModel;
        this.lifecycleOwner = lifecycleOwner;
    }

    // Setter cho MainViewModel và LifecycleOwner nếu không được cung cấp trong constructor
    public void setViewModel(MainViewModel viewModel, LifecycleOwner lifecycleOwner) {
        this.viewModel = viewModel;
        this.lifecycleOwner = lifecycleOwner;
    }

    @Override
    public int getItemViewType(int position) {
        return viewType;
    }

    @NonNull
    @Override
    public BookViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;
        switch (viewType) {
            case VIEW_TYPE_TRENDING:
                view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_trending_book, parent, false);
                return new TrendingBookViewHolder(view);
            case VIEW_TYPE_GRID:
                view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_grid_book, parent, false);
                return new GridBookViewHolder(view);
            case VIEW_TYPE_CONTINUE_READING:
            default:
                view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_book, parent, false);
                return new ContinueReadingViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull BookViewHolder holder, int position) {
        Book book = books.get(position);
        String authorId = book.getAuthorId(); // Sử dụng getAuthorId() thay vì getAuthor()

        if (holder instanceof TrendingBookViewHolder) {
            // Binding cho trending book view holder
            TrendingBookViewHolder trendingHolder = (TrendingBookViewHolder) holder;
            trendingHolder.trendingBookTitle.setText(book.getTitle());
            loadAuthorName(authorId, trendingHolder.trendingBookAuthor);
            loadBookImage(book.getImageUrl(), trendingHolder.trendingBookImage);

        } else if (holder instanceof GridBookViewHolder) {
            // Binding cho grid book view holder
            GridBookViewHolder gridHolder = (GridBookViewHolder) holder;
            gridHolder.bookTitle.setText(book.getTitle());
            loadAuthorName(authorId, gridHolder.bookAuthor);
            loadBookImage(book.getImageUrl(), gridHolder.bookImage);

        } else if (holder instanceof ContinueReadingViewHolder) {
            // Binding cho continue reading view holder
            ContinueReadingViewHolder continueHolder = (ContinueReadingViewHolder) holder;
            continueHolder.bookTitle.setText(book.getTitle());
            loadAuthorName(authorId, continueHolder.bookAuthor);
            loadBookImage(book.getImageUrl(), continueHolder.bookImage);

            // Thiết lập tiến trình đọc (demo value)
            int progress = 75;
            if (continueHolder.readProgress != null) {
                continueHolder.readProgress.setProgress(progress);
            }
            if (continueHolder.readPercentage != null) {
                continueHolder.readPercentage.setText(progress + "% completed");
            }
        }

        // Set click listener cho tất cả loại layout
        holder.itemView.setOnClickListener(v -> {
            Context context = holder.itemView.getContext();
            Intent intent = new Intent(context, BookDetailActivity.class);
            intent.putExtra("bookId", book.getId());
            context.startActivity(intent);
        });
    }

    // Helper method to load book images
    private void loadBookImage(String imageUrl, ImageView imageView) {
        String directUrl = GoogleDriveUtils.convertToDirect(imageUrl);
        if (directUrl != null) {
            Glide.with(imageView.getContext())
                    .load(directUrl)
                    .placeholder(R.drawable.z6456262903514_8961d85cbd925e7e3f1929bd368cd347)
                    .error(R.drawable.z6456262903514_8961d85cbd925e7e3f1929bd368cd347)
                    .into(imageView);
        } else {
            imageView.setImageResource(R.drawable.z6456262903514_8961d85cbd925e7e3f1929bd368cd347);
        }
    }

    // Phương thức để tải và hiển thị tên tác giả
    private void loadAuthorName(String authorId, TextView authorTextView) {
        if (authorId == null || authorId.isEmpty()) {
            authorTextView.setText("Không rõ tác giả");
            return;
        }

        // Kiểm tra cache trước
        if (authorNameCache.containsKey(authorId)) {
            authorTextView.setText(authorNameCache.get(authorId));
            return;
        }

        // Hiển thị "Đang tải..." trong khi đợi
        authorTextView.setText("Đang tải...");

        // Nếu có MainViewModel và LifecycleOwner, tải tên tác giả
        if (viewModel != null && lifecycleOwner != null) {
            viewModel.getAuthorById(authorId).observe(lifecycleOwner, author -> {
                if (author != null) {
                    String authorName = author.getName();
                    authorTextView.setText(authorName);
                    // Lưu vào cache
                    authorNameCache.put(authorId, authorName);
                } else {
                    authorTextView.setText("Không rõ tác giả");
                }
            });
        } else {
            // Nếu không có ViewModel, chỉ hiển thị ID của tác giả
            authorTextView.setText(authorId);
        }
    }

    @Override
    public int getItemCount() {
        return books.size();
    }

    public void submitList(List<Book> newBooks) {
        Log.d(TAG, "Submitting " + (newBooks != null ? newBooks.size() : 0) + " books to adapter");
        books.clear();
        if (newBooks != null) {
            books.addAll(newBooks);
        }
        notifyDataSetChanged();
    }

    // Abstract base ViewHolder
    public abstract static class BookViewHolder extends RecyclerView.ViewHolder {
        public BookViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }

    // ViewHolder for Continue Reading
    public static class ContinueReadingViewHolder extends BookViewHolder {
        TextView bookTitle;
        TextView bookAuthor;
        ImageView bookImage;
        ProgressBar readProgress;
        TextView readPercentage;

        ContinueReadingViewHolder(@NonNull View itemView) {
            super(itemView);
            bookTitle = itemView.findViewById(R.id.bookTitle);
            bookAuthor = itemView.findViewById(R.id.bookAuthor);
            bookImage = itemView.findViewById(R.id.bookImage);
            readProgress = itemView.findViewById(R.id.readProgress);
            readPercentage = itemView.findViewById(R.id.readPercentage);
        }
    }

    // ViewHolder for Trending Books
    public static class TrendingBookViewHolder extends BookViewHolder {
        TextView trendingBookTitle;
        TextView trendingBookAuthor;
        ImageView trendingBookImage;

        TrendingBookViewHolder(@NonNull View itemView) {
            super(itemView);
            trendingBookTitle = itemView.findViewById(R.id.trendingBookTitle);
            trendingBookAuthor = itemView.findViewById(R.id.trendingBookAuthor);
            trendingBookImage = itemView.findViewById(R.id.trendingBookImage);
        }
    }

    // ViewHolder for Grid Books
    public static class GridBookViewHolder extends BookViewHolder {
        TextView bookTitle;
        TextView bookAuthor;
        ImageView bookImage;

        GridBookViewHolder(@NonNull View itemView) {
            super(itemView);
            bookTitle = itemView.findViewById(R.id.bookTitle);
            bookAuthor = itemView.findViewById(R.id.bookAuthor);
            bookImage = itemView.findViewById(R.id.bookImage);
        }
    }
}