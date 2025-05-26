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
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import com.example.snapnews.R;
import com.example.snapnews.database.ArticleDao;
import com.example.snapnews.database.NewsDatabase;
import com.example.snapnews.databinding.ActivityDetailBinding;
import com.example.snapnews.models.Article;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DetailActivity extends AppCompatActivity {
    private static final String TAG = "DetailActivity";
    private ActivityDetailBinding binding;
    private Article article;
    private ArticleDao articleDao;
    private ExecutorService executorService;
    private Handler mainHandler;
    private boolean isDestroyed = false;
    private boolean isFavorite = false;
    private ImageView btnFavorite;

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
            setContentView(binding.getRoot());

            // Initialize components
            mainHandler = new Handler(Looper.getMainLooper());
            initializeDatabase();
            getArticleFromIntent();
            setupUI();
            setupButtons();
            setupFavoriteButton();
            checkFavoriteStatus();

            Log.d(TAG, "DetailActivity created successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate: " + e.getMessage(), e);
            finish();
        }
    }

    private void initializeDatabase() {
        try {
            NewsDatabase database = NewsDatabase.getDatabase(this);
            articleDao = database.articleDao();
            executorService = Executors.newSingleThreadExecutor();
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

            Log.d(TAG, "Article data loaded: " + article.getTitle());
        } catch (Exception e) {
            Log.e(TAG, "Error getting article from intent: " + e.getMessage(), e);
        }
    }

    private void setupFavoriteButton() {
        try {
            btnFavorite = binding.toolbar.findViewById(R.id.btn_favorite);
            if (btnFavorite != null) {
                btnFavorite.setOnClickListener(v -> toggleFavorite());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error setting up favorite button: " + e.getMessage(), e);
        }
    }

    private void checkFavoriteStatus() {
        if (article == null || article.getUrl() == null) return;

        executorService.execute(() -> {
            try {
                Article existingArticle = articleDao.getArticleByUrl(article.getUrl());
                boolean currentFavoriteStatus = existingArticle != null && existingArticle.isFavorite();

                mainHandler.post(() -> {
                    isFavorite = currentFavoriteStatus;
                    updateFavoriteIcon();
                });
            } catch (Exception e) {
                Log.e(TAG, "Error checking favorite status: " + e.getMessage(), e);
            }
        });
    }

    private void toggleFavorite() {
        if (article == null || article.getUrl() == null) {
            Toast.makeText(this, "Cannot save this article", Toast.LENGTH_SHORT).show();
            return;
        }

        // Toggle status immediately for UI responsiveness
        isFavorite = !isFavorite;
        updateFavoriteIcon();

        // Update database in background
        executorService.execute(() -> {
            try {
                Article existingArticle = articleDao.getArticleByUrl(article.getUrl());

                if (existingArticle != null) {
                    // Update existing article
                    existingArticle.setFavorite(isFavorite);
                    articleDao.updateArticle(existingArticle);
                } else if (isFavorite) {
                    // Insert new article as favorite
                    article.setFavorite(true);
                    article.setTimestamp(System.currentTimeMillis());
                    articleDao.insertArticle(article);
                }

                mainHandler.post(() -> {
                    String message = isFavorite ? "Added to favorites" : "Removed from favorites";
                    Toast.makeText(DetailActivity.this, message, Toast.LENGTH_SHORT).show();
                });

                Log.d(TAG, "Favorite status updated: " + isFavorite);
            } catch (Exception e) {
                Log.e(TAG, "Error updating favorite: " + e.getMessage(), e);

                // Revert UI change if database update failed
                mainHandler.post(() -> {
                    isFavorite = !isFavorite;
                    updateFavoriteIcon();
                    Toast.makeText(DetailActivity.this, "Failed to update favorite", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void updateFavoriteIcon() {
        try {
            if (btnFavorite != null) {
                int iconRes = isFavorite ? R.drawable.ic_favorite_filled : R.drawable.ic_favorite_border;
                btnFavorite.setImageResource(iconRes);

                // Add animation
                btnFavorite.animate().scaleX(1.2f).scaleY(1.2f).setDuration(100)
                        .withEndAction(() -> btnFavorite.animate().scaleX(1f).scaleY(1f).setDuration(100));
            }
        } catch (Exception e) {
            Log.e(TAG, "Error updating favorite icon: " + e.getMessage(), e);
        }
    }

    private void setupUI() {
        if (article == null || isDestroyed) return;

        try {
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
            binding.webView.getSettings().setJavaScriptEnabled(false); // Disable JS for stability
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
            // Use toolbar instead of button_back
            if (binding.toolbar != null) {
                binding.toolbar.setNavigationOnClickListener(v -> safeFinish());
            }

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

    private void shareArticleSafely() {
        try {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            String shareText = (article.getTitle() != null ? article.getTitle() : "News Article") +
                    "\n\n" + (article.getUrl() != null ? article.getUrl() : "");
            shareIntent.putExtra(Intent.EXTRA_TEXT, shareText);

            // Add FLAG_ACTIVITY_NEW_TASK to prevent memory issues
            shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            startActivity(Intent.createChooser(shareIntent, "Share article"));

            Log.d(TAG, "Share intent started successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error sharing article: " + e.getMessage(), e);
            Toast.makeText(this, "Unable to share article", Toast.LENGTH_SHORT).show();
        }
    }

    // MAIN FIX: Improved browser opening with proper cleanup
    private void openInBrowserSafely() {
        try {
            if (article.getUrl() == null || article.getUrl().isEmpty()) {
                Toast.makeText(this, "No URL available", Toast.LENGTH_SHORT).show();
                return;
            }

            String url = article.getUrl();

            // Ensure URL starts with http:// or https://
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                url = "https://" + url;
            }

            Log.d(TAG, "Opening URL: " + url);

            // Method 1: Try simple browser intent first
            if (openBrowserSimple(url)) {
                return;
            }

            // Method 2: Try with explicit browser apps
            if (openBrowserExplicit(url)) {
                return;
            }

            // Method 3: Try with chooser
            if (openBrowserWithChooser(url)) {
                return;
            }

            // Method 4: Copy URL as fallback
            copyUrlToClipboard(url);

        } catch (Exception e) {
            Log.e(TAG, "Error opening browser: " + e.getMessage(), e);
            Toast.makeText(this, "Unable to open browser", Toast.LENGTH_SHORT).show();
        }
    }

    // Method 1: Simple browser intent
    private boolean openBrowserSimple(String url) {
        try {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));

            // Check if there's an app to handle this
            if (browserIntent.resolveActivity(getPackageManager()) != null) {
                startActivity(browserIntent);
                Log.d(TAG, "Opened with simple intent");
                return true;
            }
        } catch (Exception e) {
            Log.e(TAG, "Simple browser intent failed: " + e.getMessage());
        }
        return false;
    }

    // Method 2: Try explicit browser apps
    private boolean openBrowserExplicit(String url) {
        try {
            // List of common browser package names
            String[] browsers = {
                    "com.android.chrome",           // Chrome
                    "com.android.browser",          // Default Android Browser
                    "org.mozilla.firefox",          // Firefox
                    "com.opera.browser",            // Opera
                    "com.UCMobile.intl",           // UC Browser
                    "com.microsoft.emmx",           // Edge
                    "com.brave.browser"            // Brave
            };

            for (String browserPackage : browsers) {
                try {
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    browserIntent.setPackage(browserPackage);

                    if (browserIntent.resolveActivity(getPackageManager()) != null) {
                        startActivity(browserIntent);
                        Log.d(TAG, "Opened with browser: " + browserPackage);
                        return true;
                    }
                } catch (Exception e) {
                    // Try next browser
                    continue;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Explicit browser intent failed: " + e.getMessage());
        }
        return false;
    }

    // Method 3: Browser with chooser
    private boolean openBrowserWithChooser(String url) {
        try {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            Intent chooserIntent = Intent.createChooser(browserIntent, "Open with");

            if (chooserIntent.resolveActivity(getPackageManager()) != null) {
                startActivity(chooserIntent);
                Log.d(TAG, "Opened with chooser");
                return true;
            }
        } catch (Exception e) {
            Log.e(TAG, "Chooser intent failed: " + e.getMessage());
        }
        return false;
    }

    // Method 4: Copy URL as fallback
    private void copyUrlToClipboard(String url) {
        try {
            android.content.ClipboardManager clipboard =
                    (android.content.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            android.content.ClipData clip = android.content.ClipData.newPlainText("News URL", url);
            clipboard.setPrimaryClip(clip);

            Toast.makeText(this, "No browser found. URL copied to clipboard: " + url,
                    Toast.LENGTH_LONG).show();
            Log.d(TAG, "URL copied to clipboard as fallback");
        } catch (Exception e) {
            Log.e(TAG, "Failed to copy URL: " + e.getMessage());
            Toast.makeText(this, "Unable to open browser or copy URL", Toast.LENGTH_SHORT).show();
        }
    }

    // CRITICAL: Proper WebView cleanup
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