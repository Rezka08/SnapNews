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
                if (article.isFavorite()) {
                    // Artikel sudah di-favorite
                    binding.favoriteContainer.setVisibility(View.VISIBLE);
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
                } else {
                    // Artikel belum di-favorite - hide icon
                    binding.favoriteContainer.setVisibility(View.GONE);
                }
            }
        }

        private void setupClickListeners(Article article) {
            binding.cardView.setOnClickListener(v -> {
                if (onItemClickListener != null) {
                    onItemClickListener.onItemClick(article);
                }
            });

            if (binding.favoriteContainer != null) {
                binding.favoriteContainer.setOnClickListener(v -> {
                    // Toggle favorite status
                    boolean newFavoriteStatus = !article.isFavorite();
                    article.setFavorite(newFavoriteStatus);

                    // Update UI immediately for responsive feel
                    updateFavoriteIcon(article);

                    // FIXED: Save to database asynchronously
                    saveFavoriteToDatabase(article, newFavoriteStatus);

                    // Show feedback
                    String message = newFavoriteStatus ?
                            "Added to favorites" : "Removed from favorites";
                    Toast.makeText(binding.getRoot().getContext(), message, Toast.LENGTH_SHORT).show();

                    // Notify listener for any additional handling
                    if (onItemActionListener != null) {
                        onItemActionListener.onFavoriteChanged(article);
                    }
                });
            }

            // Share button
            if (binding.buttonShare != null) {
                binding.buttonShare.setOnClickListener(v -> {
                    if (onItemActionListener != null) {
                        onItemActionListener.onShareClick(article);
                    } else {
                        shareArticle(article);
                    }
                });
            }

            // Bookmark button (same as favorite for consistency)
            if (binding.buttonBookmark != null) {
                binding.buttonBookmark.setOnClickListener(v -> {
                    // Same logic as favorite icon
                    boolean newFavoriteStatus = !article.isFavorite();
                    article.setFavorite(newFavoriteStatus);
                    updateFavoriteIcon(article);
                    updateBookmarkButton(article);
                    saveFavoriteToDatabase(article, newFavoriteStatus);

                    String message = newFavoriteStatus ?
                            "Added to favorites" : "Removed from favorites";
                    Toast.makeText(binding.getRoot().getContext(), message, Toast.LENGTH_SHORT).show();

                    if (onItemActionListener != null) {
                        onItemActionListener.onFavoriteChanged(article);
                    }
                });
            }
        }

        private void saveFavoriteToDatabase(Article article, boolean isFavorite) {
            if (articleDao == null || executorService == null) {
                return; // Database not initialized
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
                    } else {
                        // Insert new article with favorite status
                        article.setFavorite(isFavorite);
                        articleDao.insertArticle(article);
                    }

                    // Log success
                    android.util.Log.d("NewsAdapter", "Favorite status updated in database: " +
                            article.getTitle() + " -> " + isFavorite);

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

        private void updateBookmarkButton(Article article) {
            if (binding.buttonBookmark != null) {
                if (article.isFavorite()) {
                    binding.buttonBookmark.setImageResource(R.drawable.ic_favorite_filled);
                    binding.buttonBookmark.setColorFilter(
                            ContextCompat.getColor(binding.getRoot().getContext(), R.color.error_color));
                } else {
                    binding.buttonBookmark.setImageResource(R.drawable.ic_favorite_border);
                    binding.buttonBookmark.setColorFilter(
                            ContextCompat.getColor(binding.getRoot().getContext(), R.color.on_surface_variant));
                }
            }
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
            if (binding.textCategory != null) {
                String category = determineCategoryFromSource(article);
                if (category != null) {
                    binding.textCategory.setText(category);
                    binding.categoryContainer.setVisibility(View.VISIBLE);
                } else {
                    binding.categoryContainer.setVisibility(View.GONE);
                }
            }
        }

        private String determineCategoryFromSource(Article article) {
            if (article.getSource() == null || article.getSource().getName() == null) {
                return null;
            }

            String sourceName = article.getSource().getName().toLowerCase();

            if (sourceName.contains("tech") || sourceName.contains("wired") ||
                    sourceName.contains("verge") || sourceName.contains("ars")) {
                return "Technology";
            }
            if (sourceName.contains("bloomberg") || sourceName.contains("fortune") ||
                    sourceName.contains("business") || sourceName.contains("financial")) {
                return "Business";
            }
            if (sourceName.contains("espn") || sourceName.contains("sport") ||
                    sourceName.contains("athletic")) {
                return "Sports";
            }
            if (sourceName.contains("entertainment") || sourceName.contains("variety") ||
                    sourceName.contains("hollywood")) {
                return "Entertainment";
            }

            return null;
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

    // ADDED: Cleanup method
    public void cleanup() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
}