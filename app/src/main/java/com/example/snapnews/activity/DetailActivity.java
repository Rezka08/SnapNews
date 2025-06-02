package com.example.snapnews.activity;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;
import com.example.snapnews.R;
import com.example.snapnews.database.ArticleDao;
import com.example.snapnews.database.NewsDatabaseHelper;
import com.example.snapnews.databinding.ActivityDetailBinding;
import com.example.snapnews.models.Article;
import com.example.snapnews.models.Source;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DetailActivity extends AppCompatActivity {
    private static final String TAG = "DetailActivity";
    private ActivityDetailBinding binding;
    private Article article;

    // Database components
    private ArticleDao articleDao;
    private NewsDatabaseHelper dbHelper;
    private ExecutorService executorService;
    private Handler mainHandler;
    private boolean isDestroyed = false;

    // Reading mode components
    private boolean isLoadingFullArticle = false;
    private String currentMode = "summary"; // "summary" atau "full"

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
            setContentView(binding.getRoot());

            // Check if binding is successful
            if (binding == null) {
                Log.e(TAG, "Failed to inflate layout - binding is null");
                finish();
                return;
            }

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
            Toast.makeText(this, "Error loading article details", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void initializeDatabase() {
        try {
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
                Article existingArticle = articleDao.getArticleByUrl(article.getUrl());

                mainHandler.post(() -> {
                    if (existingArticle != null) {
                        article.setFavorite(existingArticle.isFavorite());
                        article.setId(existingArticle.getId());
                        Log.d(TAG, "Loaded favorite status: " + article.isFavorite());
                    } else {
                        article.setFavorite(false);
                        Log.d(TAG, "Article not in database, default favorite: false");
                    }
                    invalidateOptionsMenu();
                });
            } catch (Exception e) {
                Log.e(TAG, "Error loading favorite status", e);
            }
        });
    }

    private void setupUI() {
        if (article == null || isDestroyed || binding == null) {
            Log.e(TAG, "Cannot setup UI - missing components");
            return;
        }

        try {
            // Setup toolbar
            setSupportActionBar(binding.toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setTitle("News Detail");
            }

            // Set title
            if (article.getTitle() != null && binding.textTitle != null) {
                binding.textTitle.setText(article.getTitle());
            }

            // Set description
            if (article.getDescription() != null && !article.getDescription().isEmpty() && binding.textDescription != null) {
                binding.textDescription.setText(article.getDescription());
                binding.textDescription.setVisibility(View.VISIBLE);
            } else if (binding.textDescription != null) {
                binding.textDescription.setVisibility(View.GONE);
            }

            // Set author and date
            setupAuthorDate();

            // Set reading time
            setupReadingTime();

            // Load image safely
            loadArticleImage();

            // Setup WebView with proper cleanup
            setupWebViewSafely();

            // Setup reading mode toggle
            setupReadingModeToggle();

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

    private void setupReadingTime() {
        try {
            if (binding.textReadingTime != null) {
                String readingTime = estimateReadingTime(article.getContent(), article.getDescription());
                binding.textReadingTime.setText(readingTime);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error setting reading time: " + e.getMessage(), e);
        }
    }

    private void loadArticleImage() {
        try {
            if (article.getUrlToImage() != null && !article.getUrlToImage().isEmpty() && !isDestroyed) {
                Glide.with(this)
                        .load(article.getUrlToImage())
                        .apply(new RequestOptions()
                                .transform(new RoundedCorners(24))
                                .placeholder(R.drawable.placeholder_image)
                                .error(R.drawable.placeholder_image)
                                .centerCrop())
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
        if (isDestroyed || binding == null || binding.webView == null) {
            Log.w(TAG, "Cannot setup WebView - components not available");
            return;
        }

        try {
            // Configure WebView settings for summary mode
            WebSettings webSettings = binding.webView.getSettings();
            webSettings.setJavaScriptEnabled(false);
            webSettings.setDomStorageEnabled(false);
            webSettings.setCacheMode(WebSettings.LOAD_NO_CACHE);
            webSettings.setLoadWithOverviewMode(true);
            webSettings.setUseWideViewPort(true);

            binding.webView.setWebViewClient(new WebViewClient() {
                @Override
                public void onPageStarted(WebView view, String url, Bitmap favicon) {
                    super.onPageStarted(view, url, favicon);
                    if (!isDestroyed && binding != null && binding.progressBar != null) {
                        binding.progressBar.setVisibility(View.VISIBLE);
                    }
                }

                @Override
                public void onPageFinished(WebView view, String url) {
                    super.onPageFinished(view, url);
                    if (!isDestroyed && binding != null && binding.progressBar != null) {
                        binding.progressBar.setVisibility(View.GONE);

                        if (currentMode.equals("full")) {
                            injectReadingModeCSS();
                        }
                    }
                }

                @Override
                public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                    super.onReceivedError(view, errorCode, description, failingUrl);
                    Log.e(TAG, "WebView error: " + description);
                    if (!isDestroyed && binding != null && binding.progressBar != null) {
                        binding.progressBar.setVisibility(View.GONE);
                    }
                }

                @Override
                public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                    if (currentMode.equals("full")) {
                        String url = request.getUrl().toString();
                        if (url.startsWith("http://") || url.startsWith("https://")) {
                            openUrlInExternalBrowser(url);
                            return true;
                        }
                    }
                    return false;
                }
            });

            // Load initial content (summary mode)
            loadWebViewContent();

        } catch (Exception e) {
            Log.e(TAG, "Error setting up WebView: " + e.getMessage(), e);
            if (binding != null && binding.progressBar != null) {
                binding.progressBar.setVisibility(View.GONE);
            }
        }
    }

    private void setupReadingModeToggle() {
        if (binding == null) {
            Log.w(TAG, "Cannot setup reading mode toggle - binding is null");
            return;
        }

        try {
            if (binding.buttonToggleMode != null) {
                binding.buttonToggleMode.setOnClickListener(v -> toggleReadingMode());
                updateReadingModeUI();
            }

            if (binding.textModeDescription != null) {
                binding.textModeDescription.setText("Currently showing article summary");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error setting up reading mode toggle: " + e.getMessage(), e);
        }
    }

    private void loadWebViewContent() {
        try {
            if (article.getContent() != null && !article.getContent().isEmpty()) {
                String htmlContent = createSummaryHTML(article.getContent());
                binding.webView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null);
            } else {
                String htmlContent = createSummaryHTML(article.getDescription());
                binding.webView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading WebView content: " + e.getMessage(), e);
        }
    }

    private String createSummaryHTML(String content) {
        if (content == null) content = "Content not available";

        return "<html><head>" +
                "<meta name='viewport' content='width=device-width, initial-scale=1.0'>" +
                "<style>" +
                "body { " +
                "  font-family: 'Segoe UI', Arial, sans-serif; " +
                "  line-height: 1.8; " +
                "  color: #333; " +
                "  padding: 20px; " +
                "  margin: 0; " +
                "  background-color: #ffffff; " +
                "} " +
                "p { " +
                "  margin-bottom: 16px; " +
                "  text-align: justify; " +
                "  font-size: 16px; " +
                "} " +
                ".summary-note { " +
                "  background-color: #E3F2FD; " +
                "  border-left: 4px solid #1976D2; " +
                "  padding: 12px 16px; " +
                "  margin: 16px 0; " +
                "  border-radius: 4px; " +
                "  font-style: italic; " +
                "  color: #1565C0; " +
                "} " +
                "</style>" +
                "</head><body>" +
                "<div class='summary-note'>📄 Article Summary</div>" +
                "<p>" + content.replace("\n", "</p><p>") + "</p>" +
                "<div class='summary-note'>💡 Tap 'Full Article' to read the complete story</div>" +
                "</body></html>";
    }

    // READING MODE TOGGLE FUNCTIONALITY
    private void toggleReadingMode() {
        if (currentMode.equals("summary")) {
            loadFullArticle();
        } else {
            loadSummaryMode();
        }
    }

    private void loadFullArticle() {
        if (article.getUrl() == null || article.getUrl().isEmpty()) {
            Toast.makeText(this, "Article URL not available", Toast.LENGTH_SHORT).show();
            return;
        }

        currentMode = "full";
        isLoadingFullArticle = true;
        showLoading();

        Log.d(TAG, "Loading full article from: " + article.getUrl());

        // Setup WebView for full article
        setupFullArticleWebView();

        // Load URL artikel
        binding.webView.loadUrl(article.getUrl());

        // Update UI
        updateReadingModeUI();
    }

    private void loadSummaryMode() {
        currentMode = "summary";
        isLoadingFullArticle = false;

        // Reload summary content
        loadWebViewContent();
        updateReadingModeUI();

        Toast.makeText(this, "Switched to summary mode", Toast.LENGTH_SHORT).show();
    }

    private void setupFullArticleWebView() {
        if (binding.webView == null) return;

        // Enhanced WebView settings for full article
        WebSettings webSettings = binding.webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setUseWideViewPort(true);
        webSettings.setBuiltInZoomControls(true);
        webSettings.setDisplayZoomControls(false);
        webSettings.setSupportZoom(true);
        webSettings.setDefaultTextEncodingName("utf-8");

        // Enable mixed content untuk HTTPS/HTTP
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
        }
    }

    private void injectReadingModeCSS() {
        String css =
                "javascript:(function() {" +
                        "var style = document.createElement('style');" +
                        "style.innerHTML = '" +
                        "body { " +
                        "  font-family: Georgia, serif !important; " +
                        "  line-height: 1.7 !important; " +
                        "  font-size: 18px !important; " +
                        "  margin: 20px !important; " +
                        "  background-color: #ffffff !important; " +
                        "  color: #333333 !important; " +
                        "  max-width: 800px !important; " +
                        "  margin: 0 auto !important; " +
                        "  padding: 20px !important; " +
                        "} " +
                        "p { " +
                        "  margin-bottom: 18px !important; " +
                        "  text-align: justify !important; " +
                        "  text-indent: 1em !important; " +
                        "} " +
                        "h1, h2, h3, h4, h5, h6 { " +
                        "  color: #1976D2 !important; " +
                        "  margin-top: 28px !important; " +
                        "  margin-bottom: 14px !important; " +
                        "  font-weight: bold !important; " +
                        "} " +
                        "img { " +
                        "  max-width: 100% !important; " +
                        "  height: auto !important; " +
                        "  margin: 20px auto !important; " +
                        "  display: block !important; " +
                        "  border-radius: 8px !important; " +
                        "} " +
                        "a { " +
                        "  color: #1976D2 !important; " +
                        "  text-decoration: underline !important; " +
                        "} " +
                        "blockquote { " +
                        "  border-left: 4px solid #1976D2 !important; " +
                        "  padding-left: 20px !important; " +
                        "  margin: 20px 0 !important; " +
                        "  background-color: #f8f9fa !important; " +
                        "  padding: 15px 20px !important; " +
                        "  border-radius: 4px !important; " +
                        "  font-style: italic !important; " +
                        "} " +
                        "nav, header, footer, .advertisement, .ads, .sidebar, " +
                        ".social-share, .related-articles, .comments, " +
                        ".newsletter-signup, .popup, .modal { " +
                        "  display: none !important; " +
                        "} " +
                        "';" +
                        "document.head.appendChild(style);" +
                        "})()";

        binding.webView.evaluateJavascript(css, null);
    }

    private void updateReadingModeUI() {
        try {
            if (binding.buttonToggleMode != null) {
                if (currentMode.equals("full")) {
                    binding.buttonToggleMode.setText("Summary View");
                    binding.buttonToggleMode.setBackgroundColor(
                            ContextCompat.getColor(this, R.color.secondary_color));
                } else {
                    binding.buttonToggleMode.setText("Full Article");
                    binding.buttonToggleMode.setBackgroundColor(
                            ContextCompat.getColor(this, R.color.primary_color));
                }
            }

            if (binding.textModeDescription != null) {
                if (currentMode.equals("full")) {
                    binding.textModeDescription.setText("Reading full article from web");
                } else {
                    binding.textModeDescription.setText("Currently showing article summary");
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error updating reading mode UI: " + e.getMessage(), e);
        }
    }

    private void setupButtons() {
        if (binding == null) {
            Log.w(TAG, "Cannot setup buttons - binding is null");
            return;
        }

        try {
            // Setup toolbar navigation
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

    // ENHANCED BROWSER OPENING FUNCTIONALITY
    private void openInBrowserSafely() {
        String url = null; // Deklarasi di luar try-catch

        try {
            if (article.getUrl() == null || article.getUrl().isEmpty()) {
                Toast.makeText(this, "No URL available", Toast.LENGTH_SHORT).show();
                return;
            }

            url = article.getUrl(); // Inisialisasi url
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                url = "https://" + url;
            }

            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            browserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            // Cek apakah ada aplikasi yang bisa menangani intent ini
            PackageManager packageManager = getPackageManager();
            List<ResolveInfo> activities = packageManager.queryIntentActivities(
                    browserIntent, PackageManager.MATCH_DEFAULT_ONLY);

            if (activities.size() > 0) {
                // Buat chooser untuk memberikan pilihan aplikasi
                Intent chooser = Intent.createChooser(browserIntent, "Open with");
                startActivity(chooser);
                Log.d(TAG, "Browser intent started successfully");
            } else {
                // Fallback: coba buka dengan aplikasi browser tertentu
                tryOpenWithSpecificBrowser(url);
            }

        } catch (Exception e) {
            Log.e(TAG, "Error opening browser: " + e.getMessage(), e);
            // Fallback terakhir - load di WebView internal
            loadUrlInInternalWebView(url);
        }
    }

    private void tryOpenWithSpecificBrowser(String url) {
        // Daftar aplikasi browser yang umum
        String[] browserPackages = {
                "com.android.chrome",           // Chrome
                "com.android.browser",          // Default Browser
                "org.mozilla.firefox",          // Firefox
                "com.opera.browser",            // Opera
                "com.UCMobile.intl",           // UC Browser
                "com.microsoft.emmx"            // Edge
        };

        for (String packageName : browserPackages) {
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                intent.setPackage(packageName);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                if (intent.resolveActivity(getPackageManager()) != null) {
                    startActivity(intent);
                    Log.d(TAG, "Opened with " + packageName);
                    return;
                }
            } catch (Exception e) {
                Log.d(TAG, "Failed to open with " + packageName);
            }
        }

        // Jika semua gagal, gunakan WebView internal
        Toast.makeText(this, "Opening article in app", Toast.LENGTH_SHORT).show();
        loadUrlInInternalWebView(url);
    }

    private void loadUrlInInternalWebView(String url) {
        if (binding.webView != null) {
            currentMode = "full";
            setupFullArticleWebView();
            binding.webView.loadUrl(url);
            updateReadingModeUI();
            Toast.makeText(this, "Article loaded in app", Toast.LENGTH_SHORT).show();
        }
    }

    private void openUrlInExternalBrowser(String url) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(Intent.createChooser(intent, "Open link with"));
        } catch (Exception e) {
            Log.e(TAG, "Error opening external URL: " + e.getMessage(), e);
            Toast.makeText(this, "Unable to open link", Toast.LENGTH_SHORT).show();
        }
    }

    // FAVORITE FUNCTIONALITY
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.detail_menu, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem favoriteItem = menu.findItem(R.id.action_favorite);
        if (favoriteItem != null && article != null) {
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

    private void toggleFavorite() {
        if (article == null) {
            Log.w(TAG, "Cannot toggle favorite - article is null");
            return;
        }

        Log.d(TAG, "Toggling favorite for article: " + article.getTitle());

        executorService.execute(() -> {
            try {
                boolean newFavoriteStatus = !article.isFavorite();
                article.setFavorite(newFavoriteStatus);

                Article existingArticle = articleDao.getArticleByUrl(article.getUrl());

                if (existingArticle != null) {
                    existingArticle.setFavorite(newFavoriteStatus);
                    articleDao.updateArticle(existingArticle);
                } else {
                    articleDao.insertArticle(article);
                }

                mainHandler.post(() -> {
                    invalidateOptionsMenu();
                    String message = newFavoriteStatus ?
                            "Added to favorites" : "Removed from favorites";
                    Toast.makeText(DetailActivity.this, message, Toast.LENGTH_SHORT).show();
                });

            } catch (Exception e) {
                Log.e(TAG, "Error toggling favorite", e);
                mainHandler.post(() -> {
                    Toast.makeText(DetailActivity.this, "Error updating favorite", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    // SHARE FUNCTIONALITY
    private void shareArticleSafely() {
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
                shareText += "Read more: " + article.getUrl() + "\n\n";
            }
            shareText += "Shared via SnapNews";

            shareIntent.putExtra(Intent.EXTRA_TEXT, shareText);
            shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            startActivity(Intent.createChooser(shareIntent, "Share article"));
            Log.d(TAG, "Share intent started successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error sharing article: " + e.getMessage(), e);
            Toast.makeText(this, "Unable to share article", Toast.LENGTH_SHORT).show();
        }
    }

    // UTILITY METHODS
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

        // Estimasi berdasarkan content yang ada
        int estimatedFullWordCount = wordCount < 50 ? wordCount * 15 : wordCount * 3;
        int minutes = Math.max(1, estimatedFullWordCount / 200);

        return minutes + " min read";
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

    private void showLoading() {
        Log.d(TAG, "Showing loading state");
        if (binding != null && binding.progressBar != null) {
            binding.progressBar.setVisibility(View.VISIBLE);
        }
    }

    private void hideLoading() {
        Log.d(TAG, "Hiding loading state");
        if (binding != null && binding.progressBar != null) {
            binding.progressBar.setVisibility(View.GONE);
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

    // LIFECYCLE METHODS
    @Override
    public void onBackPressed() {
        if (binding.webView != null && binding.webView.canGoBack() && currentMode.equals("full")) {
            binding.webView.goBack();
        } else {
            super.onBackPressed();
        }
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
            // Cleanup WebView with null checks
            if (binding != null && binding.webView != null) {
                try {
                    binding.webView.stopLoading();
                    binding.webView.clearCache(true);
                    binding.webView.clearHistory();
                    binding.webView.destroy();
                } catch (Exception e) {
                    Log.e(TAG, "Error cleaning up WebView: " + e.getMessage(), e);
                }
            }

            // Cleanup executor service
            if (executorService != null && !executorService.isShutdown()) {
                try {
                    executorService.shutdown();
                } catch (Exception e) {
                    Log.e(TAG, "Error shutting down executor: " + e.getMessage(), e);
                }
            }

            // Clear handlers
            if (mainHandler != null) {
                try {
                    mainHandler.removeCallbacksAndMessages(null);
                } catch (Exception e) {
                    Log.e(TAG, "Error clearing handler: " + e.getMessage(), e);
                }
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