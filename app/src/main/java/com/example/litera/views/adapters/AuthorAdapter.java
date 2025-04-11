package com.example.litera.views.adapters;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.example.litera.R;
import com.example.litera.models.Author;
import com.example.litera.utils.GoogleDriveUtils;

import java.util.ArrayList;
import java.util.List;

public class AuthorAdapter extends RecyclerView.Adapter<AuthorAdapter.AuthorViewHolder> {

    private final List<Author> authors = new ArrayList<>();
    private static final String TAG = "AuthorAdapter";

    @NonNull
    @Override
    public AuthorViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_author, parent, false);
        return new AuthorViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AuthorViewHolder holder, int position) {
        Author author = authors.get(position);
        holder.authorName.setText(author.getName());

        // Lấy URL từ đối tượng Author
        String driveUrl = author.getImageUrl();

        // Kiểm tra null và chuyển đổi URL
        String directUrl = null;
        if (driveUrl != null && !driveUrl.isEmpty()) {
            directUrl = GoogleDriveUtils.convertToDirect(driveUrl);
        }

        Log.d(TAG, "Author: " + author.getName());
        Log.d(TAG, "Original Drive URL: " + (driveUrl != null ? driveUrl : "null"));
        Log.d(TAG, "Direct URL for loading: " + (directUrl != null ? directUrl : "null"));

        // Tải ảnh với Glide
        if (directUrl != null) {
            // Sử dụng RequestOptions để làm tròn ảnh của tác giả
            RequestOptions requestOptions = new RequestOptions()
                    .placeholder(R.drawable.z6456262903514_8961d85cbd925e7e3f1929bd368cd347)
                    .error(R.drawable.z6456262903514_8961d85cbd925e7e3f1929bd368cd347)
                    .circleCrop();

            Glide.with(holder.itemView.getContext())
                    .load(directUrl)
                    .apply(requestOptions)
                    .into(holder.authorImage);
        } else {
            holder.authorImage.setImageResource(R.drawable.z6456262903514_8961d85cbd925e7e3f1929bd368cd347);
        }
    }

    @Override
    public int getItemCount() {
        return authors.size();
    }

    public void submitList(List<Author> newAuthors) {
        authors.clear();
        if (newAuthors != null) {
            authors.addAll(newAuthors);
        }
        notifyDataSetChanged();
    }

    static class AuthorViewHolder extends RecyclerView.ViewHolder {
        TextView authorName;
        ImageView authorImage;

        AuthorViewHolder(@NonNull View itemView) {
            super(itemView);
            authorName = itemView.findViewById(R.id.txt_author_name);
            authorImage = itemView.findViewById(R.id.img_author);
        }
    }
}