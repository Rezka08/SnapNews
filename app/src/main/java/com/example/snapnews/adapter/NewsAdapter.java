package com.example.snapnews.adapter;

import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;
import com.example.snapnews.R;
import com.example.snapnews.database.ArticleDao;
import com.example.snapnews.database.NewsDatabaseHelper;
import com.example.snapnews.databinding.ItemNewsBinding;
import com.example.snapnews.models.Article;
import com.example.snapnews.models.FilterChip;
import com.example.snapnews.utils.DateUtils;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class NewsAdapter extends RecyclerView.Adapter<NewsAdapter.NewsViewHolder> {
    private List<Article> articles;
    private OnItemClickListener onItemClickListener;
    private OnItemActionListener onItemActionListener;

    private ArticleDao articleDao;
    private NewsDatabaseHelper dbHelper;
    private ExecutorService executorService;
    private Handler mainHandler;

    private FilterChip currentCategory;

    public interface OnItemClickListener {
        void onItemClick(Article article);
    }

    public interface OnItemActionListener {
        void onShareClick(Article article);
        void onBookmarkClick(Article article);
        void onFavoriteClick(Article article);
        void onFavoriteChanged(Article article);
    }

    public NewsAdapter(List<Article> articles, OnItemClickListener listener) {
        this.articles = articles;
        this.onItemClickListener = listener;

        this.executorService = Executors.newSingleThreadExecutor();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    public void initializeDatabase(NewsDatabaseHelper dbHelper) {
        this.dbHelper = dbHelper;
        this.articleDao = new ArticleDao(dbHelper);
    }

    public void setOnItemActionListener(OnItemActionListener listener) {
        this.onItemActionListener = listener;
    }

    public void setCurrentCategory(FilterChip currentCategory) {
        this.currentCategory = currentCategory;
        notifyDataSetChanged(); // Refresh to update category badges
        android.util.Log.d("NewsAdapter", "Current category updated: " +
                (currentCategory != null ? currentCategory.getName() : "null"));
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

    public void updateArticleFavoriteStatus(String articleUrl, boolean isFavorite) {
        for (int i = 0; i < articles.size(); i++) {
            Article article = articles.get(i);
            if (article.getUrl() != null && article.getUrl().equals(articleUrl)) {
                article.setFavorite(isFavorite);
                notifyItemChanged(i);
                break;
            }
        }
    }

    public void refreshFavoriteStatuses() {
        if (articleDao == null || executorService == null || executorService.isShutdown()) {
            return;
        }

        executorService.execute(() -> {
            try {
                // Load favorite status untuk semua articles dari database
                for (Article article : articles) {
                    if (article.getUrl() != null) {
                        Article existingArticle = articleDao.getArticleByUrl(article.getUrl());
                        if (existingArticle != null) {
                            article.setFavorite(existingArticle.isFavorite());
                            article.setId(existingArticle.getId());
                        }
                    }
                }

                // Update UI di main thread
                if (mainHandler != null) {
                    mainHandler.post(() -> {
                        notifyDataSetChanged();
                        android.util.Log.d("NewsAdapter", "Favorite statuses refreshed for all articles");
                    });
                }

            } catch (Exception e) {
                android.util.Log.e("NewsAdapter", "Error refreshing favorite statuses", e);
            }
        });
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
            binding.textTitle.setSelected(true);

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

            loadArticleImage(article);

            updateFavoriteIcon(article);

            setCategoryBadge(article);

            setupClickListeners(article);

            animateItemEntry();
        }

        private void updateFavoriteIcon(Article article) {
            if (binding.favoriteContainer != null && binding.imageFavorite != null) {
                // ALWAYS show the favorite container
                binding.favoriteContainer.setVisibility(View.VISIBLE);

                if (article.isFavorite()) {
                    // Artikel sudah di-favorite - red filled heart
                    binding.imageFavorite.setImageResource(R.drawable.ic_favorite_filled);
                    binding.imageFavorite.setColorFilter(
                            ContextCompat.getColor(binding.getRoot().getContext(), R.color.error_color));

                    // Add subtle animation for favorite
                    binding.favoriteContainer.setScaleX(1.1f);
                    binding.favoriteContainer.setScaleY(1.1f);
                    binding.favoriteContainer.animate()
                            .scaleX(1.0f)
                            .scaleY(1.0f)
                            .setDuration(200)
                            .start();

                    android.util.Log.d("NewsAdapter", "Article " + article.getTitle() + " - FAVORITE: true");
                } else {
                    // Artikel belum di-favorite - outline heart
                    binding.imageFavorite.setImageResource(R.drawable.ic_favorite_border);
                    binding.imageFavorite.setColorFilter(
                            ContextCompat.getColor(binding.getRoot().getContext(), R.color.on_surface_variant));

                    android.util.Log.d("NewsAdapter", "Article " + article.getTitle() + " - FAVORITE: false");
                }
            }
        }

        private void setupClickListeners(Article article) {
            binding.cardView.setOnClickListener(v -> {
                if (onItemClickListener != null) {
                    onItemClickListener.onItemClick(article);
                }
            });

            // ONLY favorite container click listener (top-right favorite icon)
            if (binding.favoriteContainer != null) {
                binding.favoriteContainer.setOnClickListener(v -> {
                    // Toggle favorite status
                    boolean newFavoriteStatus = !article.isFavorite();
                    article.setFavorite(newFavoriteStatus);

                    // Update UI immediately for responsive feel
                    updateFavoriteIcon(article);

                    // Save to database asynchronously
                    saveFavoriteToDatabase(article, newFavoriteStatus);

                    // Show feedback
                    String message = newFavoriteStatus ?
                            "Added to favorites" : "Removed from favorites";
                    Toast.makeText(binding.getRoot().getContext(), message, Toast.LENGTH_SHORT).show();

                    // Notify listener for any additional handling
                    if (onItemActionListener != null) {
                        onItemActionListener.onFavoriteChanged(article);
                    }

                    android.util.Log.d("NewsAdapter", "Favorite toggled: " + article.getTitle() + " -> " + newFavoriteStatus);
                });
            }

            // Share button ONLY
            if (binding.buttonShare != null) {
                binding.buttonShare.setOnClickListener(v -> {
                    if (onItemActionListener != null) {
                        onItemActionListener.onShareClick(article);
                    } else {
                        shareArticle(article);
                    }
                });
            }
        }

        private void saveFavoriteToDatabase(Article article, boolean isFavorite) {
            if (articleDao == null || executorService == null) {
                android.util.Log.w("NewsAdapter", "Database not initialized, cannot save favorite");
                return;
            }

            executorService.execute(() -> {
                try {
                    // Set timestamp if not set
                    if (article.getTimestamp() == 0) {
                        article.setTimestamp(System.currentTimeMillis());
                    }

                    // Check if article exists in database
                    Article existingArticle = articleDao.getArticleByUrl(article.getUrl());

                    if (existingArticle != null) {
                        // Update existing article
                        existingArticle.setFavorite(isFavorite);
                        articleDao.updateArticle(existingArticle);

                        // Update local article ID
                        article.setId(existingArticle.getId());

                        android.util.Log.d("NewsAdapter", "Updated existing article favorite status: " +
                                article.getTitle() + " -> " + isFavorite);
                    } else {
                        // Insert new article with favorite status
                        article.setFavorite(isFavorite);
                        articleDao.insertArticle(article);

                        android.util.Log.d("NewsAdapter", "Inserted new article with favorite status: " +
                                article.getTitle() + " -> " + isFavorite);
                    }

                } catch (Exception e) {
                    android.util.Log.e("NewsAdapter", "Error updating favorite in database", e);

                    // Revert UI change on error
                    if (mainHandler != null) {
                        mainHandler.post(() -> {
                            article.setFavorite(!isFavorite); // Revert
                            updateFavoriteIcon(article);
                            Toast.makeText(binding.getRoot().getContext(),
                                    "Error updating favorite", Toast.LENGTH_SHORT).show();
                        });
                    }
                }
            });
        }

        private void loadArticleImage(Article article) {
            try {
                if (article.getUrlToImage() != null && !article.getUrlToImage().isEmpty()) {
                    Glide.with(binding.getRoot().getContext())
                            .load(article.getUrlToImage())
                            .apply(new RequestOptions()
                                    .transform(new RoundedCorners(24))
                                    .placeholder(R.drawable.placeholder_image)
                                    .error(R.drawable.placeholder_image)
                                    .centerCrop())
                            .into(binding.imageNews);
                } else {
                    binding.imageNews.setImageResource(R.drawable.placeholder_image);
                }
            } catch (Exception e) {
                android.util.Log.e("NewsAdapter", "Error loading image: " + e.getMessage(), e);
                binding.imageNews.setImageResource(R.drawable.placeholder_image);
            }
        }

        private void setCategoryBadge(Article article) {
            if (binding.textCategory != null && binding.categoryContainer != null) {
                String categoryName = getCurrentCategoryName();

                if (categoryName != null && !categoryName.isEmpty()) {
                    // Show category badge based on current filter
                    binding.textCategory.setText(categoryName);
                    binding.categoryContainer.setVisibility(View.VISIBLE);

                    android.util.Log.d("NewsAdapter", "Category badge shown: " + categoryName +
                            " for article: " + article.getTitle());
                } else {
                    // No current category - try to determine from source (fallback)
                    String sourceCategory = determineCategoryFromSource(article);

                    if (sourceCategory != null) {
                        binding.textCategory.setText(sourceCategory);
                        binding.categoryContainer.setVisibility(View.VISIBLE);

                        android.util.Log.d("NewsAdapter", "Source-based category badge shown: " + sourceCategory +
                                " for article: " + article.getTitle());
                    } else {
                        // No category to show
                        binding.categoryContainer.setVisibility(View.GONE);

                        android.util.Log.d("NewsAdapter", "No category badge for article: " + article.getTitle());
                    }
                }
            }
        }

        private String getCurrentCategoryName() {
            if (currentCategory == null) {
                return null;
            }

            String categoryName = currentCategory.getName();

            // Don't show badge for "All" or "Latest" categories
            if ("All".equals(categoryName) || "Latest".equals(categoryName)) {
                return null;
            }

            return categoryName;
        }

        private String determineCategoryFromSource(Article article) {
            if (article.getSource() == null || article.getSource().getName() == null) {
                return null;
            }

            String sourceName = article.getSource().getName().toLowerCase();
            String title = article.getTitle() != null ? article.getTitle().toLowerCase() : "";
            String description = article.getDescription() != null ? article.getDescription().toLowerCase() : "";

            // Enhanced category detection
            if (containsKeywords(sourceName, title, description, "tech", "technology", "gadget", "software", "hardware", "ai", "startup")) {
                return "Technology";
            }
            if (containsKeywords(sourceName, title, description, "business", "finance", "economy", "market", "stock", "economic", "financial")) {
                return "Business";
            }
            if (containsKeywords(sourceName, title, description, "sport", "football", "basketball", "soccer", "baseball", "tennis", "olympics")) {
                return "Sports";
            }
            if (containsKeywords(sourceName, title, description, "health", "medical", "medicine", "doctor", "disease", "hospital", "wellness")) {
                return "Health";
            }
            if (containsKeywords(sourceName, title, description, "entertainment", "movie", "music", "celebrity", "film", "actor", "actress", "hollywood")) {
                return "Entertainment";
            }
            if (containsKeywords(sourceName, title, description, "science", "research", "study", "scientist", "discovery", "experiment", "laboratory")) {
                return "Science";
            }

            return null;
        }

        private boolean containsKeywords(String sourceName, String title, String description, String... keywords) {
            for (String keyword : keywords) {
                if (sourceName.contains(keyword) || title.contains(keyword) || description.contains(keyword)) {
                    return true;
                }
            }
            return false;
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

        private void animateItemEntry() {
            binding.cardView.setAlpha(0f);
            binding.cardView.animate()
                    .alpha(1f)
                    .setDuration(300)
                    .setStartDelay(getAdapterPosition() * 50)
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

            String[] words = textToAnalyze.split("\\s+");
            int wordCount = words.length;
            int estimatedFullWordCount = wordCount < 50 ? wordCount * 20 : wordCount * 5;
            int minutes = Math.max(1, estimatedFullWordCount / 200);

            return minutes + " min read";
        }
    }

    public void cleanup() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
}