package com.example.snapnews.fragment;

import android.content.Context;
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
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.snapnews.adapter.NewsAdapter;
import com.example.snapnews.database.ArticleDao;
import com.example.snapnews.database.NewsDatabaseHelper;
import com.example.snapnews.databinding.FragmentSearchBinding;
import com.example.snapnews.models.Article;
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
    private List<Article> searchResults = new ArrayList<>();
    private NewsApiService newsApiService;
    private ArticleDao articleDao;
    private NewsDatabaseHelper dbHelper;
    private ExecutorService executorService;
    private Handler mainHandler;
    private Handler searchHandler;
    private Runnable searchRunnable;
    private String currentQuery = "";

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentSearchBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initializeComponents();
        setupRecyclerView();
        setupSearchView();
        showInitialState();
    }

    private void initializeComponents() {
        newsApiService = RetrofitClient.getNewsApiService();

        // PERUBAHAN: Inisialisasi SQLite Database Helper
        dbHelper = NewsDatabaseHelper.getInstance(requireContext());
        articleDao = new ArticleDao(dbHelper);

        executorService = Executors.newFixedThreadPool(2);
        mainHandler = new Handler(Looper.getMainLooper());
        searchHandler = new Handler(Looper.getMainLooper());

        Log.d(TAG, "Components initialized with SQLite database");
    }

    private void setupRecyclerView() {
        newsAdapter = new NewsAdapter(searchResults, article -> {
            if (getActivity() != null) {
                ((com.example.snapnews.activity.MainActivity) getActivity()).navigateToDetail(article);
            }
        });

        binding.recyclerViewSearch.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recyclerViewSearch.setAdapter(newsAdapter);

        Log.d(TAG, "RecyclerView setup completed");
    }

    private void setupSearchView() {
        binding.editTextSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                String query = s.toString().trim();

                // Cancel previous search
                if (searchRunnable != null) {
                    searchHandler.removeCallbacks(searchRunnable);
                }

                if (query.isEmpty()) {
                    showInitialState();
                    return;
                }

                // Delay search to avoid too many API calls
                searchRunnable = () -> performSearch(query);
                searchHandler.postDelayed(searchRunnable, 500);
            }
        });

        binding.buttonSearch.setOnClickListener(v -> {
            String query = binding.editTextSearch.getText().toString().trim();
            if (!query.isEmpty()) {
                performSearch(query);
            }
        });
    }

    private void performSearch(String query) {
        if (query.equals(currentQuery)) return;

        currentQuery = query;
        Log.d(TAG, "Performing search for: " + query);

        if (isNetworkAvailable()) {
            searchOnline(query);
        } else {
            Log.w(TAG, "No network available, searching offline in SQLite");
            searchOffline(query);
        }
    }

    private void searchOnline(String query) {
        showLoading();
        Log.d(TAG, "Searching online for: " + query);

        Call<NewsResponse> call = newsApiService.searchNews(
                query,
                "publishedAt",
                50,
                1,
                ApiKeyManager.getNewsApiKey()
        );

        call.enqueue(new Callback<NewsResponse>() {
            @Override
            public void onResponse(@NonNull Call<NewsResponse> call, @NonNull Response<NewsResponse> response) {
                Log.d(TAG, "Search API response received for query: " + query);
                hideLoading();

                if (response.isSuccessful() && response.body() != null) {
                    NewsResponse newsResponse = response.body();

                    if ("ok".equals(newsResponse.getStatus()) && newsResponse.getArticles() != null) {
                        Log.d(TAG, "Search found " + newsResponse.getArticles().size() + " articles online");

                        searchResults.clear();
                        searchResults.addAll(newsResponse.getArticles());
                        newsAdapter.notifyDataSetChanged();

                        if (searchResults.isEmpty()) {
                            showEmptyState("No results found", "Try searching with different keywords");
                        } else {
                            showResults();
                        }
                    } else {
                        Log.e(TAG, "Search API error: " + newsResponse.getMessage());
                        showError("Search Error", newsResponse.getMessage());
                    }
                } else {
                    Log.e(TAG, "Search API response not successful: " + response.code());
                    showError("Search Error", "Failed to search news");
                }
            }

            @Override
            public void onFailure(@NonNull Call<NewsResponse> call, @NonNull Throwable t) {
                Log.e(TAG, "Search API call failed", t);
                hideLoading();
                showError("Network Error", "Please check your internet connection");
                searchOffline(query);
            }
        });
    }

    private void searchOffline(String query) {
        showLoading();
        Log.d(TAG, "Searching offline in SQLite for: " + query);

        executorService.execute(() -> {
            try {
                List<Article> results = articleDao.searchArticles(query);

                mainHandler.post(() -> {
                    hideLoading();

                    searchResults.clear();
                    if (results != null && !results.isEmpty()) {
                        Log.d(TAG, "Found " + results.size() + " articles in SQLite for query: " + query);
                        searchResults.addAll(results);
                        newsAdapter.notifyDataSetChanged();
                        showResults();
                    } else {
                        Log.d(TAG, "No offline results found in SQLite for query: " + query);
                        showEmptyState("No offline results", "Connect to internet for more results");
                    }
                });

            } catch (Exception e) {
                Log.e(TAG, "Error searching in SQLite database", e);
                mainHandler.post(() -> {
                    hideLoading();
                    showError("Database Error", "Error searching cached news");
                });
            }
        });
    }

    private void showInitialState() {
        Log.d(TAG, "Showing initial state");
        if (binding != null) {
            binding.layoutInitial.setVisibility(View.VISIBLE);
            binding.recyclerViewSearch.setVisibility(View.GONE);
            binding.progressBar.setVisibility(View.GONE);
            binding.layoutEmpty.setVisibility(View.GONE);
            searchResults.clear();
            newsAdapter.notifyDataSetChanged();
        }
    }

    private void showLoading() {
        Log.d(TAG, "Showing loading state");
        if (binding != null) {
            binding.layoutInitial.setVisibility(View.GONE);
            binding.recyclerViewSearch.setVisibility(View.GONE);
            binding.progressBar.setVisibility(View.VISIBLE);
            binding.layoutEmpty.setVisibility(View.GONE);
        }
    }

    private void hideLoading() {
        Log.d(TAG, "Hiding loading state");
        if (binding != null) {
            binding.progressBar.setVisibility(View.GONE);
        }
    }

    private void showResults() {
        Log.d(TAG, "Showing search results - Count: " + searchResults.size());
        if (binding != null) {
            binding.layoutInitial.setVisibility(View.GONE);
            binding.recyclerViewSearch.setVisibility(View.VISIBLE);
            binding.layoutEmpty.setVisibility(View.GONE);
        }
    }

    private void showEmptyState(String title, String message) {
        Log.d(TAG, "Showing empty state: " + title);
        if (binding != null) {
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
            ConnectivityManager connectivityManager =
                    (ConnectivityManager) requireContext().getSystemService(Context.CONNECTIVITY_SERVICE);
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
    public void onDestroy() {
        super.onDestroy();
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
        if (searchHandler != null && searchRunnable != null) {
            searchHandler.removeCallbacks(searchRunnable);
        }
        binding = null;
        Log.d(TAG, "SearchFragment destroyed - SQLite connections will be closed automatically");
    }
}