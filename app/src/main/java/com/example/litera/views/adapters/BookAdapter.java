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
import com.example.litera.models.Author;
import com.example.litera.models.Book;
import com.example.litera.utils.GoogleDriveUtils;
import com.example.litera.viewmodels.MainViewModel;
import com.example.litera.views.fragments.BookDetailActivity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BookAdapter extends RecyclerView.Adapter<BookAdapter.BookViewHolder> {
    private static final String TAG = "BookAdapter";
    private final List<Book> books = new ArrayList<>();
    private boolean isTrendingView;
    private MainViewModel viewModel;
    private LifecycleOwner lifecycleOwner;

    // Cache tên tác giả để tránh truy vấn nhiều lần
    private final Map<String, String> authorNameCache = new HashMap<>();

    // Constructor mặc định (không tham số)
    public BookAdapter() {
        this.isTrendingView = false; // Mặc định không phải là trending view
    }

    // Constructor mới nhận tham số boolean
    public BookAdapter(boolean isTrendingView) {
        this.isTrendingView = isTrendingView;
    }

    // Constructor nhận ViewModel và LifecycleOwner
    public BookAdapter(boolean isTrendingView, MainViewModel viewModel, LifecycleOwner lifecycleOwner) {
        this.isTrendingView = isTrendingView;
        this.viewModel = viewModel;
        this.lifecycleOwner = lifecycleOwner;
    }

    // Setter cho MainViewModel và LifecycleOwner nếu không được cung cấp trong constructor
    public void setViewModel(MainViewModel viewModel, LifecycleOwner lifecycleOwner) {
        this.viewModel = viewModel;
        this.lifecycleOwner = lifecycleOwner;
    }

    @NonNull
    @Override
    public BookViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;
        if (isTrendingView) {
            // Layout cho trending books
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_trending_book, parent, false);
        } else {
            // Layout cho continue reading
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_book, parent, false);
        }
        return new BookViewHolder(view, isTrendingView);
    }

    @Override
    public void onBindViewHolder(@NonNull BookViewHolder holder, int position) {
        Book book = books.get(position);
        String authorId = book.getAuthorId(); // Sửa: sử dụng getAuthorId() thay vì getAuthor()

        // Thiết lập dữ liệu chung cho cả hai loại layout
        if (isTrendingView) {
            // Thiết lập cho trending book
            holder.trendingBookTitle.setText(book.getTitle());

            // Tải tên tác giả nếu có ViewModel và LifecycleOwner
            loadAuthorName(authorId, holder.trendingBookAuthor);

            // Lấy URL Google Drive và chuyển đổi nó
            String driveUrl = book.getImageUrl();
            String directUrl = GoogleDriveUtils.convertToDirect(driveUrl);

            // Tải ảnh với Glide
            if (directUrl != null) {
                Glide.with(holder.itemView.getContext())
                        .load(directUrl)
                        .placeholder(R.drawable.z6456262903514_8961d85cbd925e7e3f1929bd368cd347)
                        .error(R.drawable.z6456262903514_8961d85cbd925e7e3f1929bd368cd347)
                        .into(holder.trendingBookImage);
            } else {
                holder.trendingBookImage.setImageResource(R.drawable.z6456262903514_8961d85cbd925e7e3f1929bd368cd347);
            }
        } else {
            // Thiết lập cho continue reading
            holder.bookTitle.setText(book.getTitle());

            // Tải tên tác giả nếu có ViewModel và LifecycleOwner
            loadAuthorName(authorId, holder.bookAuthor);

            // Giả sử: thiết lập tiến trình đọc (có thể thay đổi dựa trên dữ liệu thực tế)
            int progress = 75; // Phần trăm hoàn thành
            if (holder.readProgress != null) {
                holder.readProgress.setProgress(progress);
            }

            if (holder.readPercentage != null) {
                holder.readPercentage.setText(progress + "% completed");
            }

            // Lấy URL Google Drive và chuyển đổi nó
            String driveUrl = book.getImageUrl();
            String directUrl = GoogleDriveUtils.convertToDirect(driveUrl);

            // Tải ảnh với Glide
            if (directUrl != null) {
                Glide.with(holder.itemView.getContext())
                        .load(directUrl)
                        .placeholder(R.drawable.z6456262903514_8961d85cbd925e7e3f1929bd368cd347)
                        .error(R.drawable.z6456262903514_8961d85cbd925e7e3f1929bd368cd347)
                        .into(holder.bookImage);
            } else {
                holder.bookImage.setImageResource(R.drawable.z6456262903514_8961d85cbd925e7e3f1929bd368cd347);
            }
        }

        // Set click listener cho cả hai loại layout
        holder.itemView.setOnClickListener(v -> {
            Context context = holder.itemView.getContext();
            Intent intent = new Intent(context, BookDetailActivity.class);
            intent.putExtra("bookId", book.getId());
            context.startActivity(intent);
        });
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
        books.clear();
        if (newBooks != null) {
            books.addAll(newBooks);
        }
        notifyDataSetChanged();
    }

    static class BookViewHolder extends RecyclerView.ViewHolder {
        // Views cho Continue Reading
        TextView bookTitle;
        TextView bookAuthor;
        ImageView bookImage;
        ProgressBar readProgress;
        TextView readPercentage;

        // Views cho Trending
        TextView trendingBookTitle;
        TextView trendingBookAuthor;
        ImageView trendingBookImage;

        BookViewHolder(@NonNull View itemView, boolean isTrending) {
            super(itemView);

            if (isTrending) {
                // Ánh xạ views cho trending books
                trendingBookTitle = itemView.findViewById(R.id.trendingBookTitle);
                trendingBookAuthor = itemView.findViewById(R.id.trendingBookAuthor);
                trendingBookImage = itemView.findViewById(R.id.trendingBookImage);
            } else {
                // Ánh xạ views cho continue reading
                bookTitle = itemView.findViewById(R.id.bookTitle);
                bookAuthor = itemView.findViewById(R.id.bookAuthor);
                bookImage = itemView.findViewById(R.id.bookImage);
                readProgress = itemView.findViewById(R.id.readProgress);
                readPercentage = itemView.findViewById(R.id.readPercentage);
            }
        }
    }
}