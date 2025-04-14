package com.example.litera.views.adapters;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStoreOwner;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.litera.R;
import com.example.litera.models.Book;
import com.example.litera.repositories.UserRepository;
import com.example.litera.utils.GoogleDriveUtils;
import com.example.litera.viewmodels.MainViewModel;
import com.example.litera.viewmodels.UserViewModel;
import com.example.litera.viewmodels.ViewModelFactory;
import com.example.litera.views.activities.AddToCartActivity;
import com.example.litera.views.activities.BookDetailActivity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
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
    private UserViewModel userViewModel;
    private int viewType = VIEW_TYPE_CONTINUE_READING;

    // Cache tên tác giả để tránh truy vấn nhiều lần
    private final Map<String, String> authorNameCache = new HashMap<>();

    // Thêm các interface để xử lý sự kiện click
    public interface OnItemClickListener {
        void onItemClick(Book book);
    }

    public interface OnItemLongClickListener {
        boolean onItemLongClick(Book book);
    }

    private OnItemClickListener onItemClickListener;
    private OnItemLongClickListener onItemLongClickListener;

    // Thêm các setter cho interface
    public void setOnItemClickListener(OnItemClickListener listener) {
        this.onItemClickListener = listener;
    }

    public void setOnItemLongClickListener(OnItemLongClickListener listener) {
        this.onItemLongClickListener = listener;
    }


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
        if (book == null) return;

        String authorId = book.getAuthorId();// Sử dụng getAuthorId() thay vì getAuthor()
        if (lifecycleOwner instanceof ViewModelStoreOwner) {
            userViewModel = new ViewModelProvider(
                    (ViewModelStoreOwner) lifecycleOwner,
                    new ViewModelFactory(new UserRepository())
            ).get(UserViewModel.class);
        }

        if (holder instanceof TrendingBookViewHolder) {
            // Binding cho trending book view holder
            TrendingBookViewHolder trendingHolder = (TrendingBookViewHolder) holder;
            if (trendingHolder.trendingBookTitle != null) {
                trendingHolder.trendingBookTitle.setText(book.getTitle());
            }
            if (trendingHolder.trendingBookAuthor != null) {
                loadAuthorName(authorId, trendingHolder.trendingBookAuthor);
            }
            if (trendingHolder.trendingBookImage != null) {
                loadBookImage(book.getImageUrl(), trendingHolder.trendingBookImage);
            }

            // Hiển thị rating nếu có
            if (trendingHolder.ratingBar != null) {
                try {
                    float ratingValue = book.getRatingAsFloat();
                    trendingHolder.ratingBar.setRating(ratingValue);

                    if (trendingHolder.ratingText != null) {
                        int ratingCount = book.getRatingCountAsInt();
                        trendingHolder.ratingText.setText(String.format(Locale.US, "%.1f (%d)",
                                ratingValue, ratingCount));
                    }
                } catch (Exception e) {
                    if (trendingHolder.ratingBar != null) {
                        trendingHolder.ratingBar.setRating(0f);
                    }
                    if (trendingHolder.ratingText != null) {
                        trendingHolder.ratingText.setText("No ratings");
                    }
                }
            }

        } else if (holder instanceof GridBookViewHolder) {
            // Binding cho grid book view holder
            GridBookViewHolder gridHolder = (GridBookViewHolder) holder;
            if (gridHolder.bookTitle != null) {
                gridHolder.bookTitle.setText(book.getTitle());
            }
            if (gridHolder.bookAuthor != null) {
                loadAuthorName(authorId, gridHolder.bookAuthor);
            }
            if (gridHolder.bookImage != null) {
                loadBookImage(book.getImageUrl(), gridHolder.bookImage);
            }

            // Hiển thị rating nếu có
            if (gridHolder.ratingBar != null) {
                try {
                    float ratingValue = book.getRatingAsFloat();
                    gridHolder.ratingBar.setRating(ratingValue);

                    if (gridHolder.ratingText != null) {
                        int ratingCount = book.getRatingCountAsInt();
                        gridHolder.ratingText.setText(String.format(Locale.US, "%.1f (%d)",
                                ratingValue, ratingCount));
                    }
                } catch (Exception e) {
                    if (gridHolder.ratingBar != null) {
                        gridHolder.ratingBar.setRating(0f);
                    }
                    if (gridHolder.ratingText != null) {
                        gridHolder.ratingText.setText("No ratings");
                    }
                }
            }

        } else if (holder instanceof ContinueReadingViewHolder) {
            // Binding cho continue reading view holder
            ContinueReadingViewHolder continueHolder = (ContinueReadingViewHolder) holder;
            if (continueHolder.bookTitle != null) {
                continueHolder.bookTitle.setText(book.getTitle());
            }
            if (continueHolder.bookAuthor != null) {
                loadAuthorName(authorId, continueHolder.bookAuthor);
            }
            if (continueHolder.bookImage != null) {
                loadBookImage(book.getImageUrl(), continueHolder.bookImage);
            }

            // Hiển thị rating nếu có
            if (continueHolder.ratingBar != null) {
                try {
                    float ratingValue = book.getRatingAsFloat();
                    continueHolder.ratingBar.setRating(ratingValue);

                    if (continueHolder.ratingText != null) {
                        int ratingCount = book.getRatingCountAsInt();
                        continueHolder.ratingText.setText(String.format(Locale.US, "%.1f (%d)",
                                ratingValue, ratingCount));
                    }
                } catch (Exception e) {
                    if (continueHolder.ratingBar != null) {
                        continueHolder.ratingBar.setRating(0f);
                    }
                    if (continueHolder.ratingText != null) {
                        continueHolder.ratingText.setText("No ratings");
                    }
                }
            }
        }

        // Set click listener cho tất cả loại layout
        holder.itemView.setOnClickListener(v -> {
            Log.d(TAG, "Checking if book " + book.getId() + " is purchased");
            userViewModel.checkBookPurchased(book.getId(), new UserRepository.OnBookPurchaseCheckListener() {
                @Override
                public void onResult(boolean hasPurchased) {
                    Log.d(TAG, "Result for book " + book.getId() + ": " + hasPurchased);
                    if (hasPurchased) {
                        Log.d(TAG, "Book is already purchased, opening AddToCartActivity");
                        Context context = holder.itemView.getContext();
                        Intent intent = new Intent(context, AddToCartActivity.class);
                        intent.putExtra("bookId", book.getId());
                        context.startActivity(intent);
                    } else {
                        Log.d(TAG, "Book is not purchased, opening BookDetailActivity");
                        Context context = holder.itemView.getContext();
                        Intent intent = new Intent(context, BookDetailActivity.class);
                        intent.putExtra("bookId", book.getId());
                        context.startActivity(intent);
                    }
                }
            });
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

    public Book getItem(int position) {
        if (position >= 0 && position < books.size()) {
            return books.get(position);
        }
        return null;
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
        RatingBar ratingBar;
        TextView ratingText;

        ContinueReadingViewHolder(@NonNull View itemView) {
            super(itemView);
            bookTitle = itemView.findViewById(R.id.bookTitle);
            bookAuthor = itemView.findViewById(R.id.bookAuthor);
            bookImage = itemView.findViewById(R.id.bookImage);

            // Thử tìm các view rating - có thể là null nếu layout không có
            try {
                ratingBar = itemView.findViewById(R.id.ratingBar);
                ratingText = itemView.findViewById(R.id.ratingText);
            } catch (Exception e) {
                Log.w(TAG, "Rating views not found in continue reading layout");
            }
        }
    }

    // ViewHolder for Trending Books
    public static class TrendingBookViewHolder extends BookViewHolder {
        TextView trendingBookTitle;
        TextView trendingBookAuthor;
        ImageView trendingBookImage;
        RatingBar ratingBar;
        TextView ratingText;

        TrendingBookViewHolder(@NonNull View itemView) {
            super(itemView);
            trendingBookTitle = itemView.findViewById(R.id.trendingBookTitle);
            trendingBookAuthor = itemView.findViewById(R.id.trendingBookAuthor);
            trendingBookImage = itemView.findViewById(R.id.trendingBookImage);

            // Thử tìm các view rating - có thể là null nếu layout không có
            try {
                ratingBar = itemView.findViewById(R.id.ratingBar);
                ratingText = itemView.findViewById(R.id.ratingText);
            } catch (Exception e) {
                Log.w(TAG, "Rating views not found in trending book layout");
            }
        }
    }

    // ViewHolder for Grid Books

    public static class GridBookViewHolder extends BookViewHolder {
        TextView bookTitle;
        TextView bookAuthor;
        ImageView bookImage;
        RatingBar ratingBar;
        TextView ratingText;


        GridBookViewHolder(@NonNull View itemView) {
            super(itemView);

            // Trong grid view layout, IDs có thể khác với các layout khác
            bookTitle = itemView.findViewById(R.id.bookTitle);
            if (bookTitle == null) {
                bookTitle = itemView.findViewById(R.id.gridBookTitle);
            }

            bookAuthor = itemView.findViewById(R.id.bookAuthor);
            if (bookAuthor == null) {
                bookAuthor = itemView.findViewById(R.id.gridBookAuthor);
            }

            bookImage = itemView.findViewById(R.id.bookImage);
            if (bookImage == null) {
                bookImage = itemView.findViewById(R.id.gridBookImage);
            }

            // Thử tìm các view rating - có thể là null nếu layout không có
            try {
                ratingBar = itemView.findViewById(R.id.ratingBar);
                ratingText = itemView.findViewById(R.id.ratingText);
            } catch (Exception e) {
                Log.w(TAG, "Rating views not found in grid book layout");
            }
        }
    }
}
