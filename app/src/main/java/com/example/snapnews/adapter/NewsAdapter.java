package com.example.snapnews.adapter;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;
import com.example.snapnews.R;
import com.example.snapnews.databinding.ItemNewsBinding;
import com.example.snapnews.models.Article;
import com.example.snapnews.utils.DateUtils;
import java.util.List;

public class NewsAdapter extends RecyclerView.Adapter<NewsAdapter.NewsViewHolder> {
    private List<Article> articles;
    private OnItemClickListener onItemClickListener;
    private OnItemActionListener onItemActionListener;

    public interface OnItemClickListener {
        void onItemClick(Article article);
    }

    public interface OnItemActionListener {
        void onShareClick(Article article);
        void onBookmarkClick(Article article);
        void onFavoriteClick(Article article);
    }

    public NewsAdapter(List<Article> articles, OnItemClickListener listener) {
        this.articles = articles;
        this.onItemClickListener = listener;
    }

    public void setOnItemActionListener(OnItemActionListener listener) {
        this.onItemActionListener = listener;
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
            // Set title dengan animasi
            binding.textTitle.setText(article.getTitle());
            binding.textTitle.setSelected(true); // Enable marquee if needed

            // Set description
            if (article.getDescription() != null && !article.getDescription().isEmpty()) {
                binding.textDescription.setText(article.getDescription());
                binding.textDescription.setVisibility(View.VISIBLE);
            } else {
                binding.textDescription.setVisibility(View.GONE);
            }

            // Set source and date dengan icon
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

            // Set reading time estimation
            if (binding.textReadingTime != null) {
                String readingTime = estimateReadingTime(article.getContent(), article.getDescription());
                binding.textReadingTime.setText(readingTime);
            }

            // Load image dengan Glide dan rounded corners
            loadArticleImage(article);

            // Set favorite indicator
            if (article.isFavorite()) {
                binding.imageFavorite.setVisibility(View.VISIBLE);
                binding.imageFavorite.setImageResource(R.drawable.ic_favorite_filled);
            } else {
                binding.imageFavorite.setVisibility(View.GONE);
            }

            // Set category badge (jika ada)
            setCategoryBadge(article);

            // Setup click listeners
            setupClickListeners(article);

            // Add subtle animation
            animateItemEntry();
        }

        private void loadArticleImage(Article article) {
            if (article.getUrlToImage() != null && !article.getUrlToImage().isEmpty()) {
                Glide.with(binding.getRoot().getContext())
                        .load(article.getUrlToImage())
                        .apply(new RequestOptions()
                                .transform(new RoundedCorners(24))
                                .placeholder(R.drawable.placeholder_image)
                                .error(R.drawable.placeholder_image)
                                .centerCrop())
                        .into(binding.imageNews);
                binding.imageNews.setVisibility(View.VISIBLE);
            } else {
                binding.imageNews.setImageResource(R.drawable.placeholder_image);
                binding.imageNews.setVisibility(View.VISIBLE);
            }
        }

        private void setCategoryBadge(Article article) {
            // Set category badge berdasarkan source atau content
            if (binding.textCategory != null) {
                String category = determineCategoryFromSource(article);
                if (category != null) {
                    binding.textCategory.setText(category);
                    binding.textCategory.setVisibility(View.VISIBLE);
                } else {
                    binding.textCategory.setVisibility(View.GONE);
                }
            }
        }

        private String determineCategoryFromSource(Article article) {
            if (article.getSource() == null || article.getSource().getName() == null) {
                return null;
            }

            String sourceName = article.getSource().getName().toLowerCase();

            // Technology sources
            if (sourceName.contains("tech") || sourceName.contains("wired") ||
                    sourceName.contains("verge") || sourceName.contains("ars")) {
                return "Technology";
            }

            // Business sources
            if (sourceName.contains("bloomberg") || sourceName.contains("fortune") ||
                    sourceName.contains("business") || sourceName.contains("financial")) {
                return "Business";
            }

            // Sports sources
            if (sourceName.contains("espn") || sourceName.contains("sport") ||
                    sourceName.contains("athletic")) {
                return "Sports";
            }

            // Entertainment sources
            if (sourceName.contains("entertainment") || sourceName.contains("variety") ||
                    sourceName.contains("hollywood")) {
                return "Entertainment";
            }

            return null; // Default: no category badge
        }

        private void setupClickListeners(Article article) {
            // Main card click
            binding.cardView.setOnClickListener(v -> {
                if (onItemClickListener != null) {
                    onItemClickListener.onItemClick(article);
                }
            });

            // Share button
            if (binding.buttonShare != null) {
                binding.buttonShare.setOnClickListener(v -> {
                    if (onItemActionListener != null) {
                        onItemActionListener.onShareClick(article);
                    } else {
                        // Default share functionality
                        shareArticle(article);
                    }
                });
            }

            // Bookmark button
            if (binding.buttonBookmark != null) {
                binding.buttonBookmark.setOnClickListener(v -> {
                    if (onItemActionListener != null) {
                        onItemActionListener.onBookmarkClick(article);
                    } else {
                        // Default bookmark functionality
                        toggleBookmark(article);
                    }
                });
            }

            // Favorite icon click
            binding.imageFavorite.setOnClickListener(v -> {
                if (onItemActionListener != null) {
                    onItemActionListener.onFavoriteClick(article);
                }
            });
        }

        private void shareArticle(Article article) {
            try {
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("text/plain");

                String shareText = "";
                if (article.getTitle() != null) {
                    shareText += article.getTitle() + "\n\n";
                }
                if (article.getDescription() != null) {
                    shareText += article.getDescription() + "\n\n";
                }
                if (article.getUrl() != null) {
                    shareText += "Read more: " + article.getUrl();
                }

                shareIntent.putExtra(Intent.EXTRA_TEXT, shareText);
                shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                binding.getRoot().getContext().startActivity(
                        Intent.createChooser(shareIntent, "Share article"));

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        private void toggleBookmark(Article article) {
            // Toggle bookmark state
            boolean isBookmarked = !article.isFavorite();
            article.setFavorite(isBookmarked);

            // Update UI
            if (binding.buttonBookmark != null) {
                binding.buttonBookmark.setImageResource(
                        isBookmarked ? R.drawable.ic_bookmark_filled : R.drawable.ic_bookmark_border);
            }

            // Update favorite indicator
            if (isBookmarked) {
                binding.imageFavorite.setVisibility(View.VISIBLE);
            } else {
                binding.imageFavorite.setVisibility(View.GONE);
            }
        }

        private void animateItemEntry() {
            // Subtle fade-in animation
            binding.cardView.setAlpha(0f);
            binding.cardView.animate()
                    .alpha(1f)
                    .setDuration(300)
                    .setStartDelay(getAdapterPosition() * 50) // Staggered animation
                    .start();
        }

        private String estimateReadingTime(String content, String description) {
            String textToAnalyze = "";

            if (content != null && !content.isEmpty()) {
                textToAnalyze = content;
            } else if (description != null && !description.isEmpty()) {
                textToAnalyze = description;
            }

            if (textToAnalyze.isEmpty()) {
                return "2 min read";
            }

            // Rata-rata 200 kata per menit
            String[] words = textToAnalyze.split("\\s+");
            int wordCount = words.length;

            // Estimasi berdasarkan content yang ada + asumsi artikel penuh
            int estimatedFullWordCount = wordCount < 50 ? wordCount * 20 : wordCount * 5;
            int minutes = Math.max(1, estimatedFullWordCount / 200);

            return minutes + " min read";
        }
    }
}