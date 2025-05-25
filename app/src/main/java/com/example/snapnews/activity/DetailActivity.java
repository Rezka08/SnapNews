package com.example.snapnews.activity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import com.snapnews.R;
import com.snapnews.database.ArticleDao;
import com.snapnews.database.NewsDatabase;
import com.snapnews.databinding.ActivityDetailBinding;
import com.snapnews.model.Article;
import com.snapnews.utils.DateUtils;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DetailActivity extends AppCompatActivity {
    private ActivityDetailBinding binding;
    private Article article;
    private ArticleDao articleDao;
    private ExecutorService executorService;
    private boolean isFavorite = false;

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
        binding = ActivityDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        initializeDatabase();
        getArticleFromIntent();
        setupUI();
        checkIfFavorite();
    }

    private void initializeDatabase() {
        NewsDatabase database = NewsDatabase.getDatabase(this);
        articleDao = database.articleDao();
        executorService = Executors.newFixedThreadPool(2);
    }

    private void getArticleFromIntent() {
        Intent intent = getIntent();
        article = new Article();
        article.setTitle(intent.getStringExtra(EXTRA_ARTICLE_TITLE));
        article.setDescription(intent.getStringExtra(EXTRA_ARTICLE_DESCRIPTION));
        article.setUrl(intent.getStringExtra(EXTRA_ARTICLE_URL));
        article.setUrlToImage(intent.getStringExtra(EXTRA_ARTICLE_IMAGE_URL));
        article.setPublishedAt(intent.getStringExtra(EXTRA_ARTICLE_PUBLISHED_AT));
        article.setContent(intent.getStringExtra(EXTRA_ARTICLE_CONTENT));
        article.setAuthor(intent.getStringExtra(EXTRA_ARTICLE_AUTHOR));
        // Note: Source would need to be serialized separately for full implementation
    }

    private void setupUI() {
        if (article == null) return;

        // Set title
        binding.textTitle.setText(article.getTitle());
        setTitle(article.getTitle());

        // Set description
        if (article.getDescription() != null && !article.getDescription().isEmpty()) {
            binding.textDescription.setText(article.getDescription());
        } else {
            binding.textDescription.setVisibility(View.GONE);
        }

        // Set author and date
        String authorDate = "";
        if (article.getAuthor() != null && !article.getAuthor().isEmpty()) {
            authorDate = "By " + article.getAuthor();
        }
        if (article.getPublishedAt() != null) {
            String formattedDate = DateUtils.formatDate(article.getPublishedAt());
            if (!authorDate.isEmpty()) {
                authorDate += " • " + formattedDate;
            } else {
                authorDate = formattedDate;
            }
        }
        binding.textAuthorDate.setText(authorDate);

        // Load image
        if (article.getUrlToImage() != null && !article.getUrlToImage().isEmpty()) {
            Glide.with(this)
                    .load(article.getUrlToImage())
                    .placeholder(R.drawable.placeholder_image)
                    .error(R.drawable.error_image)
                    .into(binding.imageArticle);
        } else {
            binding.imageArticle.setImageResource(R.drawable.placeholder_image);
        }

        // Setup WebView for content
        setupWebView();

        // Setup buttons
        binding.fabShare.setOnClickListener(v -> shareArticle());
        binding.fabOpenBrowser.setOnClickListener(v -> openInBrowser());
    }

    private void setupWebView() {
        binding.webView.getSettings().setJavaScriptEnabled(true);
        binding.webView.getSettings().setDomStorageEnabled(true);
        binding.webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                binding.progressBar.setVisibility(View.VISIBLE);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                binding.progressBar.setVisibility(View.GONE);
            }
        });

        // Load article content or URL
        if (article.getContent() != null && !article.getContent().isEmpty()) {
            String htmlContent = "<html><body style='padding: 16px; font-family: sans-serif; line-height: 1.6;'>"
                    + article.getContent().replace("\n", "<br>") + "</body></html>";
            binding.webView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null);
        } else if (article.getUrl() != null) {
            binding.webView.loadUrl(article.getUrl());
        }
    }

    private void checkIfFavorite() {
        executorService.execute(() -> {
            Article existingArticle = articleDao.getArticleByUrl(article.getUrl());
            runOnUiThread(() -> {
                isFavorite = existingArticle != null && existingArticle.isFavorite();
                invalidateOptionsMenu();
            });
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.detail_menu, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem favoriteItem = menu.findItem(R.id.action_favorite);
        favoriteItem.setIcon(isFavorite ? R.drawable.ic_favorite_filled : R.drawable.ic_favorite_border);
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        } else if (item.getItemId() == R.id.action_favorite) {
            toggleFavorite();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void toggleFavorite() {
        executorService.execute(() -> {
            Article existingArticle = articleDao.getArticleByUrl(article.getUrl());

            if (existingArticle != null) {
                existingArticle.setFavorite(!existingArticle.isFavorite());
                articleDao.updateArticle(existingArticle);
                isFavorite = existingArticle.isFavorite();
            } else {
                article.setFavorite(true);
                articleDao.insertArticle(article);
                isFavorite = true;
            }

            runOnUiThread(() -> {
                invalidateOptionsMenu();
                String message = isFavorite ? "Added to favorites" : "Removed from favorites";
                Toast.makeText(DetailActivity.this, message, Toast.LENGTH_SHORT).show();
            });
        });
    }

    private void shareArticle() {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        String shareText = article.getTitle() + "\n\n" + article.getUrl();
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareText);
        startActivity(Intent.createChooser(shareIntent, "Share article"));
    }

    private void openInBrowser() {
        if (article.getUrl() != null) {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(article.getUrl()));
            startActivity(browserIntent);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
        binding = null;
    }
}