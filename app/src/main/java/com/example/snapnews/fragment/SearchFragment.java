package com.example.snapnews.fragment;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.snapnews.R;
import com.example.snapnews.adapter.FilterChipAdapter;
import com.example.snapnews.adapter.NewsAdapter;
import com.example.snapnews.database.ArticleDao;
import com.example.snapnews.database.NewsDatabaseHelper;
import com.example.snapnews.databinding.FragmentSearchBinding;
import com.example.snapnews.models.Article;
import com.example.snapnews.models.FilterChip;
import com.example.snapnews.models.NewsResponse;
import com.example.snapnews.network.RetrofitClient;
import com.example.snapnews.network.NewsApiService;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import java.util.ArrayList;
import java.util.List;
import com.example.snapnews.utils.ApiKeyManager;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SearchFragment extends Fragment {
    private static final String TAG = "SearchFragment";
    private FragmentSearchBinding binding;
    private NewsAdapter newsAdapter;
    private FilterChipAdapter categoryChipAdapter;
    private List<Article> searchResults = new ArrayList<>();
    private List<FilterChip> categoryFilters = new ArrayList<>();
    private NewsApiService newsApiService;
    private ArticleDao articleDao;
    private NewsDatabaseHelper dbHelper;
    private ExecutorService executorService;
    private Handler mainHandler;
    private Handler searchHandler;
    private Runnable searchRunnable;

    private String currentQuery = "";
    private FilterChip currentCategory;
    private int selectedCategoryPosition = -1;
    private boolean isRestoringState = false;

    private Call<NewsResponse> currentCall;

    private static final String KEY_SEARCH_QUERY = "search_query";
    private static final String KEY_SELECTED_CATEGORY_POSITION = "selected_category_position";
    private static final String KEY_HAS_RESULTS = "has_results";

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentSearchBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initializeComponents();
        setupRecyclerViews();
        setupSearchView();
        setupCategoryFilters();

        restoreState(savedInstanceState);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putString(KEY_SEARCH_QUERY, currentQuery);
        outState.putInt(KEY_SELECTED_CATEGORY_POSITION, selectedCategoryPosition);
        outState.putBoolean(KEY_HAS_RESULTS, !searchResults.isEmpty());

        Log.d(TAG, "State saved - Query: '" + currentQuery + "', Category position: " + selectedCategoryPosition + ", Has results: " + !searchResults.isEmpty());
    }

    private void restoreState(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            isRestoringState = true;

            String savedQuery = savedInstanceState.getString(KEY_SEARCH_QUERY, "");
            int savedCategoryPosition = savedInstanceState.getInt(KEY_SELECTED_CATEGORY_POSITION, -1);
            boolean hadResults = savedInstanceState.getBoolean(KEY_HAS_RESULTS, false);

            Log.d(TAG, "Restoring state - Query: '" + savedQuery + "', Category position: " + savedCategoryPosition + ", Had results: " + hadResults);

            // Restore search text
            if (!savedQuery.isEmpty()) {
                binding.editTextSearch.setText(savedQuery);
                currentQuery = savedQuery;
            }

            // Restore category selection
            if (savedCategoryPosition >= 0 && savedCategoryPosition < categoryFilters.size()) {
                selectedCategoryPosition = savedCategoryPosition;
                currentCategory = categoryFilters.get(savedCategoryPosition);
                categoryChipAdapter.setSelectedPosition(savedCategoryPosition);

                Log.d(TAG, "Restored category: " + currentCategory.getName());
            }

            // Restore results if had any
            if (hadResults || !savedQuery.isEmpty() || savedCategoryPosition >= 0) {
                // Delay execution to ensure UI is ready
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    performSearchOrFilter(currentQuery);
                    isRestoringState = false;
                }, 300);
            } else {
                showInitialState();
                isRestoringState = false;
            }
        } else {
            // No saved state - show initial state
            showInitialState();
        }
    }

    private void initializeComponents() {
        newsApiService = RetrofitClient.getNewsApiService();

        // Initialize SQLite Database Helper
        dbHelper = NewsDatabaseHelper.getInstance(requireContext());
        articleDao = new ArticleDao(dbHelper);

        executorService = Executors.newFixedThreadPool(2);
        mainHandler = new Handler(Looper.getMainLooper());
        searchHandler = new Handler(Looper.getMainLooper());

        Log.d(TAG, "Components initialized with SQLite database");
    }

    private void setupRecyclerViews() {
        newsAdapter = new NewsAdapter(searchResults, article -> {
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

        newsAdapter.setCurrentCategory(currentCategory);

        binding.recyclerViewSearch.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recyclerViewSearch.setAdapter(newsAdapter);

        // Categories RecyclerView
        LinearLayoutManager categoryLayoutManager = new LinearLayoutManager(
                getContext(), LinearLayoutManager.HORIZONTAL, false);
        binding.recyclerViewCategories.setLayoutManager(categoryLayoutManager);

        Log.d(TAG, "RecyclerViews setup completed with database and action listeners");
    }

    private void setupSearchView() {
        binding.editTextSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                // Don't trigger search during state restoration
                if (isRestoringState) return;

                String query = s.toString().trim();

                // Cancel previous search
                if (searchRunnable != null && searchHandler != null) {
                    searchHandler.removeCallbacks(searchRunnable);
                }

                // Always perform search/filter, even with empty query
                searchRunnable = () -> performSearchOrFilter(query);
                if (searchHandler != null) {
                    searchHandler.postDelayed(searchRunnable, 300);
                }
            }
        });

        binding.buttonSearch.setOnClickListener(v -> {
            String query = binding.editTextSearch.getText().toString().trim();
            performSearchOrFilter(query);
        });
    }

    private void setupCategoryFilters() {
        categoryFilters.clear();

        categoryFilters.add(new FilterChip("All", null, null));
        categoryFilters.add(new FilterChip("Business", "business", null));
        categoryFilters.add(new FilterChip("Technology", "technology", null));
        categoryFilters.add(new FilterChip("Sports", "sports", null));
        categoryFilters.add(new FilterChip("Health", "health", null));
        categoryFilters.add(new FilterChip("Entertainment", "entertainment", null));
        categoryFilters.add(new FilterChip("Science", "science", null));

        currentCategory = null;
        selectedCategoryPosition = -1;

        categoryChipAdapter = new FilterChipAdapter(categoryFilters, (filterChip, position) -> {
            Log.d(TAG, "Category filter clicked: " + filterChip.getName());
            onCategoryFilterChanged(filterChip, position);
        }, false, true);

        binding.recyclerViewCategories.setAdapter(categoryChipAdapter);
        Log.d(TAG, "Category filters setup completed with " + categoryFilters.size() + " categories - NO AUTO-SELECTION");
    }

    private void onCategoryFilterChanged(FilterChip filterChip, int position) {
        Log.d(TAG, "=== CATEGORY FILTER CHANGE ===");
        Log.d(TAG, "Old category: " + (currentCategory != null ? currentCategory.getName() : "null"));
        Log.d(TAG, "New category: " + filterChip.getName());

        // Check if this is a deselection (chip was clicked while selected)
        if (currentCategory != null && currentCategory.getName().equals(filterChip.getName()) &&
                categoryChipAdapter.getSelectedPosition() == -1) {
            // Category was deselected
            currentCategory = null;
            selectedCategoryPosition = -1;
            Log.d(TAG, "Category deselected - showing initial state");

            // Update NewsAdapter
            newsAdapter.setCurrentCategory(null);

            showInitialState();
            return;
        }

        // Category was selected
        currentCategory = filterChip;
        selectedCategoryPosition = position;

        // Update NewsAdapter with current category for badge display
        newsAdapter.setCurrentCategory(currentCategory);

        // Cancel current call
        cancelCurrentCall();

        // Auto-refresh with current query or filter-based search
        String query = binding.editTextSearch.getText().toString().trim();
        currentQuery = query; // Update current query

        autoRefreshWithFilters(query, "Category changed to " + filterChip.getName());
    }

    private void autoRefreshWithFilters(String query, String statusMessage) {
        if (!isAdded() || getContext() == null) {
            Log.w(TAG, "Fragment not attached, skipping auto-refresh");
            return;
        }

        currentQuery = query;
        Log.d(TAG, "Auto-refreshing with filters - Query: '" + query + "'");
        Log.d(TAG, "Selected category: " + (currentCategory != null ? currentCategory.getName() : "None"));

        // Show status message
        if (statusMessage != null && !statusMessage.isEmpty()) {
            Toast.makeText(getContext(), statusMessage, Toast.LENGTH_SHORT).show();
        }

        if (isNetworkAvailable()) {
            if (query.isEmpty()) {
                // Load filtered news without search query
                loadFilteredNews();
            } else {
                // Search with query and filters
                searchOnlineWithFilters(query);
            }
        } else {
            Log.w(TAG, "No network available, searching offline");
            searchOfflineWithFilters(query);
        }
    }

    private void performSearchOrFilter(String query) {
        if (!isAdded() || getContext() == null) {
            Log.w(TAG, "Fragment not attached, skipping search");
            return;
        }

        currentQuery = query;

        // Only perform search if there's a query OR a category selected
        if (query.isEmpty() && currentCategory == null) {
            Log.d(TAG, "No query and no category selected - showing initial state");
            showInitialState();
            return;
        }

        if (query.isEmpty()) {
            // Load filtered news based on current category
            loadFilteredNews();
        } else {
            // Search with query and category filter
            Log.d(TAG, "Performing search with query: " + query);
            searchOnlineWithFilters(query);
        }
    }

    private void loadFilteredNews() {
        if (!isAdded() || getContext() == null) {
            Log.w(TAG, "Fragment not attached, skipping filtered news load");
            return;
        }

        // Don't load if no category selected
        if (currentCategory == null) {
            Log.d(TAG, "No category selected - showing initial state");
            showInitialState();
            return;
        }

        Log.d(TAG, "Loading filtered news for category: " + currentCategory.getName());

        if (isNetworkAvailable()) {
            loadFilteredNewsOnline();
        } else {
            loadFilteredNewsOffline();
        }
    }

    private void loadFilteredNewsOnline() {
        if (!isAdded() || getContext() == null) {
            Log.w(TAG, "Fragment not attached, skipping online filtered news");
            return;
        }

        showLoading();
        Log.d(TAG, "Loading filtered news online");

        // Cancel previous call
        cancelCurrentCall();

        // Determine parameters for filtered news
        String category = null;
        String country = "us"; // Default country

        if (currentCategory != null && currentCategory.getCategory() != null) {
            category = currentCategory.getCategory();
        }

        // Use top-headlines for filtered news
        currentCall = newsApiService.getTopHeadlines(
                country,
                category,
                30, // More articles for better variety
                1,
                ApiKeyManager.getNewsApiKey()
        );

        currentCall.enqueue(new Callback<NewsResponse>() {
            @Override
            public void onResponse(@NonNull Call<NewsResponse> call, @NonNull Response<NewsResponse> response) {
                Log.d(TAG, "Filtered news API response received");

                if (!isAdded() || getContext() == null) {
                    Log.w(TAG, "Fragment not attached, ignoring filtered news response");
                    return;
                }

                hideLoading();

                if (response.isSuccessful() && response.body() != null) {
                    NewsResponse newsResponse = response.body();

                    if ("ok".equals(newsResponse.getStatus()) && newsResponse.getArticles() != null) {
                        List<Article> articles = newsResponse.getArticles();

                        Log.d(TAG, "Filtered news found " + articles.size() + " articles");

                        searchResults.clear();
                        searchResults.addAll(articles);
                        newsAdapter.notifyDataSetChanged();

                        // ADDED: Load favorite statuses after loading articles
                        loadFavoriteStatusForArticles();

                        if (searchResults.isEmpty()) {
                            showEmptyState("No articles found", "Try different category or check back later");
                        } else {
                            showResults();
                            String message = "📰 Showing " + articles.size() + " articles";
                            if (currentCategory != null && currentCategory.getCategory() != null) {
                                message += " in " + currentCategory.getName();
                            }
                            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Log.e(TAG, "Filtered news API error: " + newsResponse.getMessage());
                        showError("News Error", newsResponse.getMessage());
                    }
                } else {
                    Log.e(TAG, "Filtered news API response not successful: " + response.code());
                    showError("Network Error", "Failed to load news");
                }

                currentCall = null;
            }

            @Override
            public void onFailure(@NonNull Call<NewsResponse> call, @NonNull Throwable t) {
                Log.e(TAG, "Filtered news API call failed", t);

                if (!isAdded() || getContext() == null) {
                    Log.w(TAG, "Fragment not attached, ignoring filtered news failure");
                    return;
                }

                hideLoading();
                showError("Network Error", "Please check your internet connection");
                loadFilteredNewsOffline();

                currentCall = null;
            }
        });
    }

    private void loadFilteredNewsOffline() {
        if (!isAdded() || getContext() == null || executorService == null || executorService.isShutdown()) {
            Log.w(TAG, "Fragment not ready for offline filtered news load");
            return;
        }

        showLoading();
        Log.d(TAG, "Loading filtered news offline");

        executorService.execute(() -> {
            try {
                List<Article> cachedArticles = articleDao.getAllArticles();
                List<Article> filteredResults = filterResultsByCategory(cachedArticles);

                if (isAdded() && getContext() != null && mainHandler != null) {
                    mainHandler.post(() -> {
                        if (!isAdded() || getContext() == null) {
                            Log.w(TAG, "Fragment not attached, skipping offline filtered news UI update");
                            return;
                        }

                        hideLoading();
                        searchResults.clear();

                        if (filteredResults != null && !filteredResults.isEmpty()) {
                            Log.d(TAG, "Found " + filteredResults.size() + " cached articles with category filter");
                            searchResults.addAll(filteredResults);
                            newsAdapter.notifyDataSetChanged();

                            // ADDED: Load favorite statuses for offline results
                            refreshAllFavoriteStatuses();

                            showResults();

                            String message = "📱 Offline: " + filteredResults.size() + " cached articles";
                            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
                        } else {
                            Log.d(TAG, "No cached articles found with current category");
                            showEmptyState("No cached articles", "Connect to internet for latest news");
                        }
                    });
                }

            } catch (Exception e) {
                Log.e(TAG, "Error loading filtered news offline", e);
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

    private void searchOnlineWithFilters(String query) {
        if (!isAdded() || getContext() == null) {
            Log.w(TAG, "Fragment not attached, skipping online search with filters");
            return;
        }

        showLoading();
        Log.d(TAG, "Searching online with category filter for: " + query);

        // Cancel previous call
        cancelCurrentCall();

        // Build search query with category if needed
        String searchQuery = query;
        if (currentCategory != null && currentCategory.getCategory() != null) {
            searchQuery += " " + currentCategory.getCategory();
        }

        currentCall = newsApiService.searchNews(
                searchQuery,
                "publishedAt",
                50,
                1,
                ApiKeyManager.getNewsApiKey()
        );

        currentCall.enqueue(new Callback<NewsResponse>() {
            @Override
            public void onResponse(@NonNull Call<NewsResponse> call, @NonNull Response<NewsResponse> response) {
                Log.d(TAG, "Search with category filter API response received for query: " + query);

                if (!isAdded() || getContext() == null) {
                    Log.w(TAG, "Fragment not attached, ignoring search with filters response");
                    return;
                }

                hideLoading();

                if (response.isSuccessful() && response.body() != null) {
                    NewsResponse newsResponse = response.body();

                    if ("ok".equals(newsResponse.getStatus()) && newsResponse.getArticles() != null) {
                        List<Article> filteredResults = filterResultsByCategory(newsResponse.getArticles());

                        Log.d(TAG, "Search with category filter found " + filteredResults.size() + " articles");

                        searchResults.clear();
                        searchResults.addAll(filteredResults);
                        newsAdapter.notifyDataSetChanged();

                        // ADDED: Load favorite statuses after loading search results
                        loadFavoriteStatusForArticles();

                        if (searchResults.isEmpty()) {
                            showEmptyState("No results found", "Try different keywords or category");
                        } else {
                            showResults();
                            String message = "🔍 Found " + filteredResults.size() + " results for '" + query + "'";
                            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Log.e(TAG, "Search with category filter API error: " + newsResponse.getMessage());
                        showError("Search Error", newsResponse.getMessage());
                    }
                } else {
                    Log.e(TAG, "Search with category filter API response not successful: " + response.code());
                    showError("Search Error", "Failed to search news");
                }

                currentCall = null;
            }

            @Override
            public void onFailure(@NonNull Call<NewsResponse> call, @NonNull Throwable t) {
                Log.e(TAG, "Search with category filter API call failed", t);

                if (!isAdded() || getContext() == null) {
                    Log.w(TAG, "Fragment not attached, ignoring search with filters failure");
                    return;
                }

                hideLoading();
                showError("Network Error", "Please check your internet connection");
                searchOfflineWithFilters(query);

                currentCall = null;
            }
        });
    }

    private void searchOfflineWithFilters(String query) {
        if (!isAdded() || getContext() == null || executorService == null || executorService.isShutdown()) {
            Log.w(TAG, "Fragment not ready for offline search with filters");
            return;
        }

        showLoading();
        Log.d(TAG, "Searching offline with category filter for: " + query);

        executorService.execute(() -> {
            try {
                List<Article> results;
                if (query.isEmpty()) {
                    results = articleDao.getAllArticles();
                } else {
                    results = articleDao.searchArticles(query);
                }

                List<Article> filteredResults = filterResultsByCategory(results);

                if (isAdded() && getContext() != null && mainHandler != null) {
                    mainHandler.post(() -> {
                        if (!isAdded() || getContext() == null) {
                            Log.w(TAG, "Fragment not attached, skipping offline search with filters UI update");
                            return;
                        }

                        hideLoading();
                        searchResults.clear();

                        if (filteredResults != null && !filteredResults.isEmpty()) {
                            Log.d(TAG, "Found " + filteredResults.size() + " cached articles with search and category filter");
                            searchResults.addAll(filteredResults);
                            newsAdapter.notifyDataSetChanged();

                            // ADDED: Load favorite statuses for offline search results
                            refreshAllFavoriteStatuses();

                            showResults();
                        } else {
                            Log.d(TAG, "No cached results found with search and category filter");
                            showEmptyState("No offline results", "Connect to internet for more results");
                        }
                    });
                }

            } catch (Exception e) {
                Log.e(TAG, "Error searching offline with category filter", e);
                if (isAdded() && getContext() != null && mainHandler != null) {
                    mainHandler.post(() -> {
                        if (isAdded() && getContext() != null) {
                            hideLoading();
                            showError("Database Error", "Error searching cached news");
                        }
                    });
                }
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

    private List<Article> filterResultsByCategory(List<Article> articles) {
        if (currentCategory == null || currentCategory.getCategory() == null) {
            return articles; // Return all if no category filter
        }

        List<Article> filtered = new ArrayList<>();
        String category = currentCategory.getCategory().toLowerCase();

        for (Article article : articles) {
            if (matchesCategory(article, category)) {
                filtered.add(article);
            }
        }

        return filtered;
    }

    private boolean matchesCategory(Article article, String category) {
        String title = article.getTitle() != null ? article.getTitle().toLowerCase() : "";
        String description = article.getDescription() != null ? article.getDescription().toLowerCase() : "";
        String content = article.getContent() != null ? article.getContent().toLowerCase() : "";

        // Enhanced category matching
        return title.contains(category) ||
                description.contains(category) ||
                content.contains(category);
    }

    private void showInitialState() {
        Log.d(TAG, "Showing initial state");
        if (binding != null && isAdded()) {
            binding.layoutInitial.setVisibility(View.VISIBLE);
            binding.recyclerViewSearch.setVisibility(View.GONE);
            binding.progressBar.setVisibility(View.GONE);
            binding.layoutEmpty.setVisibility(View.GONE);
        }
    }

    private void showLoading() {
        Log.d(TAG, "Showing loading state");
        if (binding != null && isAdded()) {
            binding.layoutInitial.setVisibility(View.GONE);
            binding.recyclerViewSearch.setVisibility(View.GONE);
            binding.progressBar.setVisibility(View.VISIBLE);
            binding.layoutEmpty.setVisibility(View.GONE);
        }
    }

    private void hideLoading() {
        Log.d(TAG, "Hiding loading state");
        if (binding != null && isAdded()) {
            binding.progressBar.setVisibility(View.GONE);
        }
    }

    private void showResults() {
        Log.d(TAG, "Showing search results - Count: " + searchResults.size());
        if (binding != null && isAdded()) {
            binding.layoutInitial.setVisibility(View.GONE);
            binding.recyclerViewSearch.setVisibility(View.VISIBLE);
            binding.layoutEmpty.setVisibility(View.GONE);
        }
    }

    private void showEmptyState(String title, String message) {
        Log.d(TAG, "Showing empty state: " + title);
        if (binding != null && isAdded()) {
            binding.layoutInitial.setVisibility(View.GONE);
            binding.recyclerViewSearch.setVisibility(View.GONE);
            binding.layoutEmpty.setVisibility(View.VISIBLE);
            binding.textEmptyTitle.setText(title);
            binding.textEmptyMessage.setText(message);
        }
    }

    private void showError(String title, String message) {
        Log.e(TAG, "Showing error: " + title + " - " + message);
        showEmptyState(title, message);
    }

    private boolean isNetworkAvailable() {
        try {
            if (getContext() == null) {
                return false;
            }

            ConnectivityManager connectivityManager =
                    (ConnectivityManager) getContext().getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            boolean result = activeNetworkInfo != null && activeNetworkInfo.isConnected();
            Log.d(TAG, "Network available: " + result);
            return result;
        } catch (Exception e) {
            Log.e(TAG, "Error checking network", e);
            return false;
        }
    }

    // ADDED: Handle favorite changes in SearchFragment
    private void handleFavoriteChange(Article article) {
        Log.d(TAG, "Favorite changed for article: " + article.getTitle() +
                " -> " + article.isFavorite());

        // Notify MainActivity about favorite change
        if (getActivity() != null && getActivity() instanceof com.example.snapnews.activity.MainActivity) {
            ((com.example.snapnews.activity.MainActivity) getActivity()).onFavoriteChanged(article);
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
            Log.d(TAG, "Share intent started successfully from SearchFragment");
        } catch (Exception e) {
            Log.e(TAG, "Error sharing article from SearchFragment: " + e.getMessage(), e);
            if (getContext() != null) {
                Toast.makeText(getContext(), "Unable to share article", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void loadFavoriteStatusForArticles() {
        if (executorService == null || executorService.isShutdown() || !isAdded()) {
            Log.w(TAG, "Cannot load favorite status - fragment not ready");
            return;
        }

        Log.d(TAG, "Loading favorite status for " + searchResults.size() + " articles in SearchFragment");

        executorService.execute(() -> {
            try {
                // Load favorite status untuk semua articles dari database
                int updatedCount = 0;
                for (Article article : searchResults) {
                    if (article.getUrl() != null) {
                        Article existingArticle = articleDao.getArticleByUrl(article.getUrl());
                        if (existingArticle != null) {
                            boolean oldStatus = article.isFavorite();
                            article.setFavorite(existingArticle.isFavorite());
                            article.setId(existingArticle.getId());

                            if (oldStatus != article.isFavorite()) {
                                updatedCount++;
                                Log.d(TAG, "Updated favorite status in SearchFragment for: " + article.getTitle() +
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
                            Log.d(TAG, "SearchFragment favorite status loaded - " + finalUpdatedCount + " articles updated");
                        }
                    });
                }

            } catch (Exception e) {
                Log.e(TAG, "Error loading favorite status in SearchFragment", e);
            }
        });
    }

    private void refreshAllFavoriteStatuses() {
        if (newsAdapter != null) {
            Log.d(TAG, "Refreshing all favorite statuses via NewsAdapter in SearchFragment");
            newsAdapter.refreshFavoriteStatuses();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "SearchFragment resumed - Current state: Query='" + currentQuery + "', Category=" +
                (currentCategory != null ? currentCategory.getName() : "null") + ", Results=" + searchResults.size());

        if (searchResults != null && !searchResults.isEmpty()) {
            refreshAllFavoriteStatuses();
        }
    }

    @Override
    public void onDestroyView() {
        Log.d(TAG, "onDestroyView called - State will be preserved");

        // Cancel any ongoing search calls
        cancelCurrentCall();

        // Remove any pending search callbacks
        if (searchHandler != null && searchRunnable != null) {
            searchHandler.removeCallbacks(searchRunnable);
        }

        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy called");

        // Cancel any ongoing search calls
        cancelCurrentCall();

        // Remove any pending search callbacks
        if (searchHandler != null && searchRunnable != null) {
            searchHandler.removeCallbacks(searchRunnable);
        }

        // Shutdown executor service
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
            Log.d(TAG, "ExecutorService shutdown");
        }

        super.onDestroy();
        Log.d(TAG, "SearchFragment destroyed - State preserved for next creation");
    }
}