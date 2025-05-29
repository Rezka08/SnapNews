package com.example.snapnews.activity;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import com.example.snapnews.R;
import com.example.snapnews.database.ArticleDao;
import com.example.snapnews.database.NewsDatabaseHelper;
import com.example.snapnews.databinding.ActivityDetailBinding;
import com.example.snapnews.models.Article;
import com.example.snapnews.models.Source;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DetailActivity extends AppCompatActivity {
    private static final String TAG = "DetailActivity";
    private ActivityDetailBinding binding;
    private Article article;

    // PERUBAHAN: Tambah database components untuk favorite functionality
    private ArticleDao articleDao;
    private NewsDatabaseHelper dbHelper;
    private ExecutorService executorService;
    private Handler mainHandler;
    private boolean isDestroyed = false;

    // Intent extras constants
    public static final String EXTRA_ARTICLE_TITLE = "extra_article_title";
    public static final String EXTRA_ARTICLE_DESCRIPTION = "extra_article_description";
    public static final String EXTRA_ARTICLE_URL = "extra_article_url";
    public static final String EXTRA_ARTICLE_IMAGE_URL = "extra_article_image_url";
    public static final String EXTRA_ARTICLE_PUBLISHED_AT = "extra_article_published_at";
    public static final String EXTRA_ARTICLE_CONTENT = "extra_article_content";
    public static final String EXTRA_ARTICLE_AUTHOR = "extra_article_author";
    public static final String EXTRA_ARTICLE_SOURCE = "extra_article_source";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            binding = ActivityDetailBinding.inflate(getLayoutInflater());
            super.setContentView(binding.getRoot());

            // Initialize components
            mainHandler = new Handler(Looper.getMainLooper());
            initializeDatabase();
            getArticleFromIntent();
            setupUI();
            setupButtons();

            // Load favorite status from database
            loadFavoriteStatus();

            Log.d(TAG, "DetailActivity created successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate: " + e.getMessage(), e);
            finish();
        }
    }

    private void initializeDatabase() {
        try {
            // PERUBAHAN: Initialize SQLite database
            dbHelper = NewsDatabaseHelper.getInstance(this);
            articleDao = new ArticleDao(dbHelper);
            executorService = Executors.newSingleThreadExecutor();
            Log.d(TAG, "Database initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error initializing database: " + e.getMessage(), e);
        }
    }

    private void getArticleFromIntent() {
        try {
            Intent intent = getIntent();
            article = new Article();
            article.setTitle(intent.getStringExtra(EXTRA_ARTICLE_TITLE));
            article.setDescription(intent.getStringExtra(EXTRA_ARTICLE_DESCRIPTION));
            article.setUrl(intent.getStringExtra(EXTRA_ARTICLE_URL));
            article.setUrlToImage(intent.getStringExtra(EXTRA_ARTICLE_IMAGE_URL));
            article.setPublishedAt(intent.getStringExtra(EXTRA_ARTICLE_PUBLISHED_AT));
            article.setContent(intent.getStringExtra(EXTRA_ARTICLE_CONTENT));
            article.setAuthor(intent.getStringExtra(EXTRA_ARTICLE_AUTHOR));

            // Handle source
            String sourceName = intent.getStringExtra(EXTRA_ARTICLE_SOURCE);
            if (sourceName != null) {
                article.setSource(new Source(null, sourceName));
            }

            // Set timestamp if not set
            if (article.getTimestamp() == 0) {
                article.setTimestamp(System.currentTimeMillis());
            }

            Log.d(TAG, "Article data loaded: " + article.getTitle());
        } catch (Exception e) {
            Log.e(TAG, "Error getting article from intent: " + e.getMessage(), e);
        }
    }

    private void loadFavoriteStatus() {
        if (article == null || article.getUrl() == null) return;

        executorService.execute(() -> {
            try {
                // Check if article exists in database and get its favorite status
                Article existingArticle = articleDao.getArticleByUrl(article.getUrl());

                mainHandler.post(() -> {
                    if (existingArticle != null) {
                        article.setFavorite(existingArticle.isFavorite());
                        article.setId(existingArticle.getId()); // Set ID for updates
                        Log.d(TAG, "Loaded favorite status: " + article.isFavorite());
                    } else {
                        article.setFavorite(false);
                        Log.d(TAG, "Article not in database, default favorite: false");
                    }
                    // Update UI after loading favorite status
                    invalidateOptionsMenu();
                });
            } catch (Exception e) {
                Log.e(TAG, "Error loading favorite status", e);
            }
        });
    }

    private void setupUI() {
        if (article == null || isDestroyed) return;

        try {
            // Setup toolbar
            setSupportActionBar(binding.toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setTitle("News Detail");
            }

            // Set title
            if (article.getTitle() != null) {
                binding.textTitle.setText(article.getTitle());
            }

            // Set description
            if (article.getDescription() != null && !article.getDescription().isEmpty()) {
                binding.textDescription.setText(article.getDescription());
                binding.textDescription.setVisibility(View.VISIBLE);
            } else {
                binding.textDescription.setVisibility(View.GONE);
            }

            // Set author and date
            setupAuthorDate();

            // Load image safely
            loadArticleImage();

            // Setup WebView with proper cleanup
            setupWebViewSafely();

        } catch (Exception e) {
            Log.e(TAG, "Error in setupUI: " + e.getMessage(), e);
        }
    }

    private void setupAuthorDate() {
        try {
            String authorDate = "";
            if (article.getAuthor() != null && !article.getAuthor().isEmpty()) {
                authorDate = "By " + article.getAuthor();
            }
            if (article.getPublishedAt() != null) {
                String formattedDate = formatDate(article.getPublishedAt());
                if (!authorDate.isEmpty()) {
                    authorDate += " • " + formattedDate;
                } else {
                    authorDate = formattedDate;
                }
            }
            if (!authorDate.isEmpty()) {
                binding.textAuthorDate.setText(authorDate);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error setting author date: " + e.getMessage(), e);
        }
    }

    private void loadArticleImage() {
        try {
            if (article.getUrlToImage() != null && !article.getUrlToImage().isEmpty() && !isDestroyed) {
                Glide.with(this)
                        .load(article.getUrlToImage())
                        .placeholder(R.drawable.placeholder_image)
                        .error(R.drawable.placeholder_image)
                        .into(binding.imageArticle);
            } else {
                binding.imageArticle.setImageResource(R.drawable.placeholder_image);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading image: " + e.getMessage(), e);
            binding.imageArticle.setImageResource(R.drawable.placeholder_image);
        }
    }

    private void setupWebViewSafely() {
        if (isDestroyed || binding.webView == null) return;

        try {
            // Configure WebView settings
            binding.webView.getSettings().setJavaScriptEnabled(false);
            binding.webView.getSettings().setDomStorageEnabled(false);
            binding.webView.getSettings().setCacheMode(android.webkit.WebSettings.LOAD_NO_CACHE);

            // Set WebView client with proper cleanup
            binding.webView.setWebViewClient(new WebViewClient() {
                @Override
                public void onPageStarted(WebView view, String url, Bitmap favicon) {
                    super.onPageStarted(view, url, favicon);
                    if (!isDestroyed && binding.progressBar != null) {
                        binding.progressBar.setVisibility(View.VISIBLE);
                    }
                }

                @Override
                public void onPageFinished(WebView view, String url) {
                    super.onPageFinished(view, url);
                    if (!isDestroyed && binding.progressBar != null) {
                        binding.progressBar.setVisibility(View.GONE);
                    }
                }

                @Override
                public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                    super.onReceivedError(view, errorCode, description, failingUrl);
                    Log.e(TAG, "WebView error: " + description);
                    if (!isDestroyed && binding.progressBar != null) {
                        binding.progressBar.setVisibility(View.GONE);
                    }
                }
            });

            // Load content
            loadWebViewContent();

        } catch (Exception e) {
            Log.e(TAG, "Error setting up WebView: " + e.getMessage(), e);
            if (binding.progressBar != null) {
                binding.progressBar.setVisibility(View.GONE);
            }
        }
    }

    private void loadWebViewContent() {
        try {
            if (article.getContent() != null && !article.getContent().isEmpty()) {
                String htmlContent = "<html><head>" +
                        "<meta name='viewport' content='width=device-width, initial-scale=1.0'>" +
                        "<style>" +
                        "body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; padding: 16px; }" +
                        "img { max-width: 100%; height: auto; }" +
                        "</style>" +
                        "</head><body>"
                        + article.getContent().replace("\n", "<br>") +
                        "</body></html>";
                binding.webView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null);
            } else {
                binding.webView.loadData("<html><body><p>Content not available</p></body></html>", "text/html", "UTF-8");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading WebView content: " + e.getMessage(), e);
        }
    }

    private void setupButtons() {
        try {
            // Setup toolbar navigation
            binding.toolbar.setNavigationOnClickListener(v -> safeFinish());

            // Share button
            if (binding.fabShare != null) {
                binding.fabShare.setOnClickListener(v -> shareArticleSafely());
            }

            // Open browser button
            if (binding.fabOpenBrowser != null) {
                binding.fabOpenBrowser.setOnClickListener(v -> openInBrowserSafely());
            }

        } catch (Exception e) {
            Log.e(TAG, "Error setting up buttons: " + e.getMessage(), e);
        }
    }

    // PERUBAHAN: Implement options menu untuk favorite functionality
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.detail_menu, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem favoriteItem = menu.findItem(R.id.action_favorite);
        if (favoriteItem != null && article != null) {
            // Update icon based on favorite status
            if (article.isFavorite()) {
                favoriteItem.setIcon(R.drawable.ic_favorite_filled);
                favoriteItem.setTitle("Remove from favorites");
            } else {
                favoriteItem.setIcon(R.drawable.ic_favorite_border);
                favoriteItem.setTitle("Add to favorites");
            }
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_favorite) {
            toggleFavorite();
            return true;
        } else if (item.getItemId() == android.R.id.home) {
            safeFinish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // PERUBAHAN: Implement toggle favorite functionality
    private void toggleFavorite() {
        if (article == null) {
            Log.w(TAG, "Cannot toggle favorite - article is null");
            return;
        }

        Log.d(TAG, "Toggling favorite for article: " + article.getTitle());
        Log.d(TAG, "Current favorite status: " + article.isFavorite());

        executorService.execute(() -> {
            try {
                // Toggle favorite status
                boolean newFavoriteStatus = !article.isFavorite();
                article.setFavorite(newFavoriteStatus);

                // Check if article exists in database
                Article existingArticle = articleDao.getArticleByUrl(article.getUrl());

                if (existingArticle != null) {
                    // Update existing article
                    existingArticle.setFavorite(newFavoriteStatus);
                    articleDao.updateArticle(existingArticle);
                    Log.d(TAG, "Updated existing article favorite status: " + newFavoriteStatus);
                } else {
                    // Insert new article with favorite status
                    articleDao.insertArticle(article);
                    Log.d(TAG, "Inserted new article with favorite status: " + newFavoriteStatus);
                }

                // Update UI on main thread
                mainHandler.post(() -> {
                    invalidateOptionsMenu(); // Refresh menu icon

                    String message = newFavoriteStatus ?
                            "Added to favorites" : "Removed from favorites";
                    Toast.makeText(DetailActivity.this, message, Toast.LENGTH_SHORT).show();

                    Log.d(TAG, "Favorite toggle completed: " + message);
                });

            } catch (Exception e) {
                Log.e(TAG, "Error toggling favorite", e);
                mainHandler.post(() -> {
                    Toast.makeText(DetailActivity.this, "Error updating favorite", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void shareArticleSafely() {
        try {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            String shareText = (article.getTitle() != null ? article.getTitle() : "News Article") +
                    "\n\n" + (article.getUrl() != null ? article.getUrl() : "");
            shareIntent.putExtra(Intent.EXTRA_TEXT, shareText);
            shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            startActivity(Intent.createChooser(shareIntent, "Share article"));
            Log.d(TAG, "Share intent started successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error sharing article: " + e.getMessage(), e);
            Toast.makeText(this, "Unable to share article", Toast.LENGTH_SHORT).show();
        }
    }

    private void openInBrowserSafely() {
        try {
            if (article.getUrl() == null || article.getUrl().isEmpty()) {
                Toast.makeText(this, "No URL available", Toast.LENGTH_SHORT).show();
                return;
            }

            String url = article.getUrl();
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                url = "https://" + url;
            }

            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            if (browserIntent.resolveActivity(getPackageManager()) != null) {
                startActivity(browserIntent);
            } else {
                Toast.makeText(this, "No browser app found", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error opening browser: " + e.getMessage(), e);
            Toast.makeText(this, "Unable to open browser", Toast.LENGTH_SHORT).show();
        }
    }

    private void cleanupWebView() {
        try {
            if (binding.webView != null) {
                binding.webView.stopLoading();
                binding.webView.clearCache(true);
                binding.webView.clearHistory();
                binding.webView.loadUrl("about:blank");
                Log.d(TAG, "WebView cleaned up");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error cleaning WebView: " + e.getMessage(), e);
        }
    }

    private void safeFinish() {
        try {
            cleanupWebView();
            finish();
        } catch (Exception e) {
            Log.e(TAG, "Error in safeFinish: " + e.getMessage(), e);
            finish();
        }
    }

    private String formatDate(String dateString) {
        try {
            if (dateString != null && dateString.length() >= 10) {
                return dateString.substring(0, 10);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error formatting date: " + e.getMessage(), e);
        }
        return dateString;
    }

    @Override
    protected void onPause() {
        super.onPause();
        try {
            if (binding.webView != null) {
                binding.webView.onPause();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in onPause: " + e.getMessage(), e);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        try {
            if (binding.webView != null && !isDestroyed) {
                binding.webView.onResume();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in onResume: " + e.getMessage(), e);
        }
    }

    @Override
    protected void onDestroy() {
        isDestroyed = true;

        try {
            // Cleanup WebView
            if (binding.webView != null) {
                binding.webView.stopLoading();
                binding.webView.clearCache(true);
                binding.webView.clearHistory();
                binding.webView.destroy();
            }

            // Cleanup executor service
            if (executorService != null && !executorService.isShutdown()) {
                executorService.shutdown();
            }

            // Clear handlers
            if (mainHandler != null) {
                mainHandler.removeCallbacksAndMessages(null);
            }

            // Clear binding
            binding = null;

            Log.d(TAG, "DetailActivity destroyed and cleaned up");

        } catch (Exception e) {
            Log.e(TAG, "Error in onDestroy: " + e.getMessage(), e);
        }

        super.onDestroy();
    }
}