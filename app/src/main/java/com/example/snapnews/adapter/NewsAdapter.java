package com.example.snapnews.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.snapnews.R;
import com.example.snapnews.databinding.ItemNewsBinding;
import com.example.snapnews.models.Article;
import com.example.snapnews.utils.DateUtils;
import java.util.List;

public class NewsAdapter extends RecyclerView.Adapter<NewsAdapter.NewsViewHolder> {
    private List<Article> articles;
    private OnItemClickListener onItemClickListener;

    public interface OnItemClickListener {
        void onItemClick(Article article);
    }

    public NewsAdapter(List<Article> articles, OnItemClickListener listener) {
        this.articles = articles;
        this.onItemClickListener = listener;
    }

    @NonNull
    @Override
    public NewsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemNewsBinding binding = ItemNewsBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new NewsViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull NewsViewHolder holder, int position) {
        Article article = articles.get(position);
        holder.bind(article);
    }

    @Override
    public int getItemCount() {
        return articles.size();
    }

    public void updateArticles(List<Article> newArticles) {
        this.articles.clear();
        this.articles.addAll(newArticles);
        notifyDataSetChanged();
    }

    class NewsViewHolder extends RecyclerView.ViewHolder {
        private ItemNewsBinding binding;

        public NewsViewHolder(@NonNull ItemNewsBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(Article article) {
            // Set title
            binding.textTitle.setText(article.getTitle());

            // Set description
            if (article.getDescription() != null && !article.getDescription().isEmpty()) {
                binding.textDescription.setText(article.getDescription());
                binding.textDescription.setVisibility(View.VISIBLE);
            } else {
                binding.textDescription.setVisibility(View.GONE);
            }

            // Set source and date
            String sourceAndDate = "";
            if (article.getSource() != null && article.getSource().getName() != null) {
                sourceAndDate = article.getSource().getName();
            }

            if (article.getPublishedAt() != null) {
                String formattedDate = DateUtils.getTimeAgo(article.getPublishedAt());
                if (!sourceAndDate.isEmpty()) {
                    sourceAndDate += " • " + formattedDate;
                } else {
                    sourceAndDate = formattedDate;
                }
            }

            binding.textSourceDate.setText(sourceAndDate);

            // Load image
            if (article.getUrlToImage() != null && !article.getUrlToImage().isEmpty()) {
                Glide.with(binding.getRoot().getContext())
                        .load(article.getUrlToImage())
                        .placeholder(R.drawable.placeholder_image)
                        .error(R.drawable.placeholder_image)
                        .into(binding.imageNews);
                binding.imageNews.setVisibility(View.VISIBLE);
            } else {
                binding.imageNews.setVisibility(View.GONE);
            }

            // Set favorite indicator
            if (article.isFavorite()) {
                binding.imageFavorite.setVisibility(View.VISIBLE);
            } else {
                binding.imageFavorite.setVisibility(View.GONE);
            }

            // Set click listener
            binding.getRoot().setOnClickListener(v -> {
                if (onItemClickListener != null) {
                    onItemClickListener.onItemClick(article);
                }
            });

            // Add ripple effect
            binding.cardView.setOnClickListener(v -> {
                if (onItemClickListener != null) {
                    onItemClickListener.onItemClick(article);
                }
            });
        }
    }
}