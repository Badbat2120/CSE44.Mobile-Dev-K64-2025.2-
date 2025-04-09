package com.example.litera.views.activities.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.litera.R;
import com.example.litera.views.activities.model.Author;

import java.util.ArrayList;
import java.util.List;

public class AuthorAdapter extends RecyclerView.Adapter<AuthorAdapter.AuthorViewHolder> {

    private final List<Author> authors = new ArrayList<>();

    @NonNull
    @Override
    public AuthorViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_tg, parent, false);
        return new AuthorViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AuthorViewHolder holder, int position) {
        Author author = authors.get(position);
        holder.authorName.setText(author.getName());

        // Load author image using Glide
        Glide.with(holder.itemView.getContext())
                .load(author.getImageUrl())
                .placeholder(R.drawable.z6456262903514_8961d85cbd925e7e3f1929bd368cd347) // Placeholder image
                .into(holder.authorImage);
    }

    @Override
    public int getItemCount() {
        return authors.size();
    }

    public void submitList(List<Author> newAuthors) {
        authors.clear();
        authors.addAll(newAuthors);
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