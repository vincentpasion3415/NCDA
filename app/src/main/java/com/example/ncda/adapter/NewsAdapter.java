// Path: app/src/main/java/com.example.ncda/adapter/NewsAdapter.java
package com.example.ncda.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.ncda.R; // R file links to your resources
import com.example.ncda.model.NewsArticle; // Your NewsArticle model

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class NewsAdapter extends RecyclerView.Adapter<NewsAdapter.NewsViewHolder> {

    private List<NewsArticle> newsList;
    private OnItemClickListener listener;

    // Interface for item click handling
    public interface OnItemClickListener {
        void onItemClick(NewsArticle newsArticle);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public NewsAdapter(List<NewsArticle> newsList) {
        this.newsList = newsList;
    }

    @NonNull
    @Override
    public NewsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_news_article, parent, false);
        return new NewsViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NewsViewHolder holder, int position) {
        NewsArticle currentNews = newsList.get(position);

        holder.title.setText(currentNews.getTitle());
        holder.description.setText(currentNews.getDescription());

        // Load image with Glide if URL exists
        if (currentNews.getImageUrl() != null && !currentNews.getImageUrl().isEmpty()) {
            holder.image.setVisibility(View.VISIBLE);
            Glide.with(holder.itemView.getContext())
                    .load(currentNews.getImageUrl())
                    .placeholder(R.drawable.placeholder_image) // Ensure this drawable exists
                    .error(R.drawable.error_image)     // Ensure this drawable exists
                    .into(holder.image);
        } else {
            holder.image.setVisibility(View.GONE);
            Glide.with(holder.itemView.getContext()).clear(holder.image); // Clear image if no URL
        }

        // Format and set published date
        if (currentNews.getPublishedOn() != null) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault());
            holder.date.setText("Published: " + dateFormat.format(currentNews.getPublishedOn().toDate()));
        } else {
            holder.date.setText("Published: N/A");
        }

        // Set up click listener for the item
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null) {
                    listener.onItemClick(currentNews);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return newsList.size();
    }

    // Method to update the data and notify the adapter
    public void updateNewsList(List<NewsArticle> newNewsList) {
        this.newsList.clear();
        this.newsList.addAll(newNewsList);
        notifyDataSetChanged();
    }

    // ViewHolder class
    public static class NewsViewHolder extends RecyclerView.ViewHolder {
        public TextView title;
        public TextView description;
        public ImageView image;
        public TextView date;

        public NewsViewHolder(View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.news_title);
            description = itemView.findViewById(R.id.news_description);
            image = itemView.findViewById(R.id.news_image);
            date = itemView.findViewById(R.id.news_date);
        }
    }
}