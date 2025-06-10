package com.example.snapnews.fragment;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.snapnews.activity.MainActivity;
import com.example.snapnews.network.RetrofitClient;
import com.example.snapnews.R;
import com.example.snapnews.adapter.FilterChipAdapter;
import com.example.snapnews.adapter.NewsAdapter;
import com.example.snapnews.database.ArticleDao;
import com.example.snapnews.database.NewsDatabaseHelper;
import com.example.snapnews.databinding.FragmentHomeBinding;
import com.example.snapnews.models.Article;
import com.example.snapnews.models.FilterChip;
import com.example.snapnews.models.NewsResponse;
import com.example.snapnews.network.NewsApiService;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import java.util.ArrayList;
import java.util.List;
import com.example.snapnews.utils.ApiKeyManager;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HomeFragment extends Fragment {
    private static final String TAG = "HomeFragment";
    private FragmentHomeBinding binding;
    private NewsAdapter newsAdapter;
    private FilterChipAdapter filterChipAdapter;
    private List<Article> articles = new ArrayList<>();
    private List<FilterChip> filterChips = new ArrayList<>();
    private NewsApiService newsApiService;

    private ArticleDao articleDao;
    private NewsDatabaseHelper dbHelper;

    private ExecutorService executorService;
    private Handler mainHandler;
    private FilterChip currentFilter;

    private Call<NewsResponse> currentCall;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initializeComponents();
        setupFilterChips();
        setupRecyclerViews();
        setupSwipeRefresh();
        loadNews();
    }

    private void initializeComponents() {
        newsApiService = RetrofitClient.getNewsApiService();

        // Initialize NewsDatabaseHelper
        dbHelper = NewsDatabaseHelper.getInstance(requireContext());
        articleDao = new ArticleDao(dbHelper);

        // Initialize threading components
        executorService = Executors.newFixedThreadPool(2);
        mainHandler = new Handler(Looper.getMainLooper());
        Log.d(TAG, "Components initialized with NewsDatabaseHelper");
    }

    private void setupFilterChips() {
        filterChips.clear();

        filterChips.add(new FilterChip("Latest", null, "us"));
        filterChips.add(new FilterChip("Business", "business", "us"));
        filterChips.add(new FilterChip("Technology", "technology", "us"));
        filterChips.add(new FilterChip("Entertainment", "entertainment", "us"));
        filterChips.add(new FilterChip("Sports", "sports", "us"));
        filterChips.add(new FilterChip("Health", "health", "us"));

        currentFilter = filterChips.get(0);
        currentFilter.setSelected(true);

        filterChipAdapter = new FilterChipAdapter(filterChips, new FilterChipAdapter.OnChipClickListener() {
            @Override
            public void onChipClick(FilterChip filterChip, int position) {
                Log.d(TAG, "Filter clicked: " + filterChip.getName());
                onFilterChipClicked(filterChip, position);
            }
        });

        Log.d(TAG, "Filter chips setup completed with " + filterChips.size() + " chips");
    }

    private void setupRecyclerViews() {
        LinearLayoutManager chipLayoutManager = new LinearLayoutManager(
                getContext(), LinearLayoutManager.HORIZONTAL, false);
        binding.recyclerViewFilterChips.setLayoutManager(chipLayoutManager);
        binding.recyclerViewFilterChips.setAdapter(filterChipAdapter);

        newsAdapter = new NewsAdapter(articles, article -> {
            if (getActivity() != null) {
                ((com.example.snapnews.activity.MainActivity) getActivity()).navigateToDetail(article);
            }
        });

        newsAdapter.initializeDatabase(dbHelper);

        newsAdapter.setOnItemActionListener(new NewsAdapter.OnItemActionListener() {
            @Override
            public void onShareClick(Article article) {
                shareArticle(article);
            }

            @Override
            public void onBookmarkClick(Article article) {
                // Same as favorite for consistency
                handleFavoriteChange(article);
            }

            @Override
            public void onFavoriteClick(Article article) {
                handleFavoriteChange(article);
            }

            @Override
            public void onFavoriteChanged(Article article) {
                // Handle favorite change callback
                handleFavoriteChange(article);
            }
        });

        binding.recyclerViewNews.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recyclerViewNews.setAdapter(newsAdapter);

        Log.d(TAG, "RecyclerViews setup completed with database sync");
    }

    private void handleFavoriteChange(Article article) {
        Log.d(TAG, "Favorite changed for article: " + article.getTitle() +
                " -> " + article.isFavorite());

        // Notify MainActivity about favorite change (optional)
        if (getActivity() != null && getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).onFavoriteChanged(article);
        }
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
                shareText += "Read more: " + article.getUrl() + "\n\n";
            }
            shareText += "Shared via SnapNews";

            shareIntent.putExtra(Intent.EXTRA_TEXT, shareText);
            shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            startActivity(Intent.createChooser(shareIntent, "Share article"));
            Log.d(TAG, "Share intent started successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error sharing article: " + e.getMessage(), e);
            if (getContext() != null) {
                Toast.makeText(getContext(), "Unable to share article", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void setupSwipeRefresh() {
        binding.swipeRefreshLayout.setOnRefreshListener(() -> {
            Log.d(TAG, "Swipe refresh triggered - forcing API call");
            loadNewsFromApi();
        });

        binding.swipeRefreshLayout.setColorSchemeResources(
                R.color.primary_color,
                R.color.secondary_color
        );
    }

    // IMPROVED: Better favorite status loading with error handling
    private void loadFavoriteStatusForArticles() {
        if (executorService == null || executorService.isShutdown() || !isAdded()) {
            Log.w(TAG, "Cannot load favorite status - fragment not ready");
            return;
        }

        Log.d(TAG, "Loading favorite status for " + articles.size() + " articles");

        executorService.execute(() -> {
            try {
                // Load favorite status untuk semua articles dari database
                int updatedCount = 0;
                for (Article article : articles) {
                    if (article.getUrl() != null) {
                        Article existingArticle = articleDao.getArticleByUrl(article.getUrl());
                        if (existingArticle != null) {
                            boolean oldStatus = article.isFavorite();
                            article.setFavorite(existingArticle.isFavorite());
                            article.setId(existingArticle.getId());

                            if (oldStatus != article.isFavorite()) {
                                updatedCount++;
                                Log.d(TAG, "Updated favorite status for: " + article.getTitle() +
                                        " -> " + article.isFavorite());
                            }
                        }
                    }
                }

                // Update UI di main thread
                if (isAdded() && mainHandler != null) {
                    final int finalUpdatedCount = updatedCount;
                    mainHandler.post(() -> {
                        if (isAdded() && newsAdapter != null) {
                            newsAdapter.notifyDataSetChanged();
                            Log.d(TAG, "Favorite status loaded - " + finalUpdatedCount + " articles updated");
                        }
                    });
                }

            } catch (Exception e) {
                Log.e(TAG, "Error loading favorite status", e);
            }
        });
    }

    // ADDED: Method to refresh favorite statuses using NewsAdapter
    private void refreshAllFavoriteStatuses() {
        if (newsAdapter != null) {
            Log.d(TAG, "Refreshing all favorite statuses via NewsAdapter");
            newsAdapter.refreshFavoriteStatuses();
        }
    }

    private void onFilterChipClicked(FilterChip filterChip, int position) {
        Log.d(TAG, "=== FILTER CHANGE TRIGGERED ===");
        Log.d(TAG, "Old filter: " + (currentFilter != null ? currentFilter.getName() : "null"));
        Log.d(TAG, "New filter: " + filterChip.getName());
        Log.d(TAG, "Position: " + position);

        currentFilter = filterChip;

        // Cancel current network call if any
        cancelCurrentCall();

        // Clear existing articles to show change immediately
        articles.clear();
        newsAdapter.notifyDataSetChanged();

        // Force API call for new filter
        Log.d(TAG, "FORCING API CALL for filter test");
        loadNewsFromApi();
    }

    private void loadNews() {
        Log.d(TAG, "=== LOADING NEWS ===");
        Log.d(TAG, "Current filter: " + (currentFilter != null ? currentFilter.getName() : "null"));

        boolean networkAvailable = isNetworkAvailable();
        Log.d(TAG, "Network check result: " + networkAvailable);

        if (networkAvailable) {
            loadNewsFromApi();
        } else {
            Log.w(TAG, "No network available, loading from NewsDatabaseHelper");
            loadNewsFromDatabase();
        }
    }

    private void loadNewsFromApi() {
        // Check if fragment is still valid
        if (!isAdded() || getContext() == null) {
            Log.w(TAG, "Fragment not attached, skipping API call");
            return;
        }

        showLoading();

        String category = currentFilter.getCategory();
        String country = currentFilter.getCountry();

        Log.d(TAG, "=== API CALL DETAILS ===");
        Log.d(TAG, "Filter: " + currentFilter.getName());
        Log.d(TAG, "Category: " + category);
        Log.d(TAG, "Country: " + country);

        // Cancel previous call
        cancelCurrentCall();

        currentCall = newsApiService.getTopHeadlines(
                country,
                category,
                20,
                1,
                ApiKeyManager.getNewsApiKey()
        );

        String url = currentCall.request().url().toString();
        Log.d(TAG, "Full API URL: " + url);

        currentCall.enqueue(new Callback<NewsResponse>() {
            @Override
            public void onResponse(@NonNull Call<NewsResponse> call, @NonNull Response<NewsResponse> response) {
                Log.d(TAG, "=== API RESPONSE ===");
                Log.d(TAG, "Response code: " + response.code());
                Log.d(TAG, "Response successful: " + response.isSuccessful());

                // Check if fragment is still valid
                if (!isAdded() || getContext() == null) {
                    Log.w(TAG, "Fragment not attached, ignoring response");
                    return;
                }

                hideLoading();

                if (response.isSuccessful() && response.body() != null) {
                    NewsResponse newsResponse = response.body();
                    Log.d(TAG, "Response status: " + newsResponse.getStatus());
                    Log.d(TAG, "Total results: " + newsResponse.getTotalResults());

                    if ("ok".equals(newsResponse.getStatus()) && newsResponse.getArticles() != null) {
                        int articleCount = newsResponse.getArticles().size();
                        Log.d(TAG, "Articles received: " + articleCount);

                        // Clear and add new articles
                        articles.clear();
                        articles.addAll(newsResponse.getArticles());

                        Log.d(TAG, "Articles added to list. Current list size: " + articles.size());

                        // Save to NewsDatabaseHelper first
                        saveArticlesToDatabase(newsResponse.getArticles());

                        // IMPROVED: Load favorite statuses after saving
                        loadFavoriteStatusForArticles();

                        // Notify adapter
                        newsAdapter.notifyDataSetChanged();
                        Log.d(TAG, "Adapter notified");

                        showContent();

                        String message = "✅ " + currentFilter.getName() + ": " + articleCount + " articles loaded";
                        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
                        Log.d(TAG, message);

                    } else {
                        String errorMsg = "API returned error - Status: " + newsResponse.getStatus();
                        if (newsResponse.getMessage() != null) {
                            errorMsg += ", Message: " + newsResponse.getMessage();
                        }
                        Log.e(TAG, errorMsg);
                        handleApiError(newsResponse.getMessage());
                    }
                } else {
                    String errorMsg = "Response not successful - Code: " + response.code();
                    Log.e(TAG, errorMsg);

                    try {
                        if (response.errorBody() != null) {
                            String errorBody = response.errorBody().string();
                            Log.e(TAG, "Error body: " + errorBody);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error reading error body", e);
                    }

                    handleApiError("Failed to load " + currentFilter.getName() + " news");
                }

                // Clear current call reference
                currentCall = null;
            }

            @Override
            public void onFailure(@NonNull Call<NewsResponse> call, @NonNull Throwable t) {
                Log.e(TAG, "=== API CALL FAILED ===", t);

                // Check if fragment is still valid
                if (!isAdded() || getContext() == null) {
                    Log.w(TAG, "Fragment not attached, ignoring failure");
                    return;
                }

                hideLoading();
                handleNetworkError();

                // Clear current call reference
                currentCall = null;
            }
        });
    }

    private void cancelCurrentCall() {
        if (currentCall != null && !currentCall.isCanceled()) {
            currentCall.cancel();
            currentCall = null;
            Log.d(TAG, "Previous API call canceled");
        }
    }

    private void loadNewsFromDatabase() {
        // Check if fragment is still valid and executor is available
        if (!isAdded() || getContext() == null || executorService == null || executorService.isShutdown()) {
            Log.w(TAG, "Fragment not ready or executor unavailable, skipping database load");
            return;
        }

        showLoading();

        executorService.execute(() -> {
            Log.d(TAG, "Loading news from NewsDatabaseHelper");

            try {
                List<Article> cachedArticles = articleDao.getAllArticles();

                // Check if fragment is still valid before updating UI
                if (isAdded() && getContext() != null && mainHandler != null) {
                    mainHandler.post(() -> {
                        // Double check if fragment is still valid
                        if (!isAdded() || getContext() == null) {
                            Log.w(TAG, "Fragment not attached, skipping UI update");
                            return;
                        }

                        hideLoading();

                        if (cachedArticles != null && !cachedArticles.isEmpty()) {
                            Log.d(TAG, "Loaded " + cachedArticles.size() + " articles from NewsDatabaseHelper");
                            articles.clear();
                            articles.addAll(cachedArticles);
                            newsAdapter.notifyDataSetChanged();

                            // ADDED: Refresh favorite statuses after loading from database
                            refreshAllFavoriteStatuses();

                            showContent();

                            String message = "📱 Offline: showing cached " + currentFilter.getName() + " news";
                            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
                        } else {
                            Log.w(TAG, "No cached articles found in NewsDatabaseHelper");
                            showError("No cached news available", "Connect to internet and pull down to refresh");
                        }
                    });
                }
            } catch (Exception e) {
                Log.e(TAG, "Error loading from NewsDatabaseHelper", e);
                if (isAdded() && getContext() != null && mainHandler != null) {
                    mainHandler.post(() -> {
                        if (isAdded() && getContext() != null) {
                            hideLoading();
                            showError("Database Error", "Error loading cached news");
                        }
                    });
                }
            }
        });
    }

    private void saveArticlesToDatabase(List<Article> articles) {
        if (executorService == null || executorService.isShutdown() || !isAdded()) {
            Log.w(TAG, "Executor unavailable or fragment not attached, skipping database save");
            return;
        }

        executorService.execute(() -> {
            try {
                Log.d(TAG, "Saving " + articles.size() + " articles to NewsDatabaseHelper");

                // Set timestamp for each article
                for (Article article : articles) {
                    if (article.getTimestamp() == 0) {
                        article.setTimestamp(System.currentTimeMillis());
                    }
                }

                articleDao.insertArticles(articles);
                Log.d(TAG, "Articles saved successfully to NewsDatabaseHelper");

            } catch (Exception e) {
                Log.e(TAG, "Error saving articles to NewsDatabaseHelper", e);
            }
        });
    }

    private void showLoading() {
        Log.d(TAG, "Showing loading state");
        if (binding != null && isAdded()) {
            binding.progressBar.setVisibility(View.VISIBLE);
            binding.recyclerViewNews.setVisibility(View.GONE);
            binding.layoutError.setVisibility(View.GONE);
            binding.swipeRefreshLayout.setRefreshing(false);
        }
    }

    private void hideLoading() {
        Log.d(TAG, "Hiding loading state");
        if (binding != null && isAdded()) {
            binding.progressBar.setVisibility(View.GONE);
            binding.swipeRefreshLayout.setRefreshing(false);
        }
    }

    private void showContent() {
        Log.d(TAG, "Showing content - Articles count: " + articles.size());
        if (binding != null && isAdded()) {
            binding.recyclerViewNews.setVisibility(View.VISIBLE);
            binding.layoutError.setVisibility(View.GONE);
        }
    }

    private void showError(String title, String message) {
        Log.d(TAG, "Showing error: " + title + " - " + message);
        if (binding != null && isAdded()) {
            binding.recyclerViewNews.setVisibility(View.GONE);
            binding.layoutError.setVisibility(View.VISIBLE);
            binding.textErrorTitle.setText(title);
            binding.textErrorMessage.setText(message);

            binding.buttonRetry.setOnClickListener(v -> {
                binding.layoutError.setVisibility(View.GONE);
                loadNews();
            });
        }
    }

    private void handleApiError(String message) {
        Log.e(TAG, "Handling API error: " + message);

        if (message != null && message.toLowerCase().contains("api key")) {
            showError("API Key Error", "Please check your API key configuration");
        } else {
            showError("Server Error", message != null ? message : "Failed to load news");
        }

        loadNewsFromDatabase();
    }

    private void handleNetworkError() {
        Log.e(TAG, "Handling network error");
        showError("Network Error", "Please check your internet connection");
        loadNewsFromDatabase();
    }

    private boolean isNetworkAvailable() {
        try {
            if (getContext() == null) {
                return false;
            }

            ConnectivityManager connectivityManager =
                    (ConnectivityManager) getContext().getSystemService(Context.CONNECTIVITY_SERVICE);

            if (connectivityManager == null) {
                Log.e(TAG, "ConnectivityManager is null");
                return false;
            }

            NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            boolean result = activeNetworkInfo != null && activeNetworkInfo.isConnected();
            Log.d(TAG, "Network available: " + result);

            return result;

        } catch (Exception e) {
            Log.e(TAG, "Error checking network", e);
            return false;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "Fragment resumed - refreshing favorite status");

        // IMPROVED: Refresh favorite status untuk semua articles ketika fragment resumed
        if (articles != null && !articles.isEmpty()) {
            // Use the new method from NewsAdapter
            refreshAllFavoriteStatuses();
        }
    }

    @Override
    public void onDestroyView() {
        Log.d(TAG, "onDestroyView called");

        // Cancel any ongoing network calls
        cancelCurrentCall();

        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy called");

        // Cancel any ongoing network calls
        cancelCurrentCall();

        if (newsAdapter != null) {
            newsAdapter.cleanup();
        }

        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
            Log.d(TAG, "ExecutorService shutdown");
        }

        super.onDestroy();
        Log.d(TAG, "HomeFragment destroyed - NewsDatabaseHelper connections will be closed automatically");
    }
}