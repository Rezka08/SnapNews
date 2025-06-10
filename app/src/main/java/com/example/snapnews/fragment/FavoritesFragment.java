package com.example.snapnews.fragment;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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
import com.example.snapnews.databinding.FragmentFavoritesBinding;
import com.example.snapnews.models.Article;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FavoritesFragment extends Fragment {
    private static final String TAG = "FavoritesFragment";
    private FragmentFavoritesBinding binding;
    private NewsAdapter newsAdapter;
    private List<Article> favoriteArticles = new ArrayList<>();
    private ArticleDao articleDao;
    private NewsDatabaseHelper dbHelper;
    private ExecutorService executorService;
    private Handler mainHandler;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentFavoritesBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initializeComponents();
        setupRecyclerView();
        loadFavorites();
    }

    private void initializeComponents() {
        dbHelper = NewsDatabaseHelper.getInstance(requireContext());
        articleDao = new ArticleDao(dbHelper);

        executorService = Executors.newFixedThreadPool(1);
        mainHandler = new Handler(Looper.getMainLooper());

        Log.d(TAG, "Components initialized with SQLite database");
    }

    private void setupRecyclerView() {
        newsAdapter = new NewsAdapter(favoriteArticles, article -> {
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
                // Handle remove from favorites
                handleFavoriteRemove(article);
            }

            @Override
            public void onFavoriteClick(Article article) {
                // Handle remove from favorites
                handleFavoriteRemove(article);
            }

            @Override
            public void onFavoriteChanged(Article article) {
                // ADDED: Handle favorite change dalam favorites fragment
                handleFavoriteRemove(article);
            }
        });

        binding.recyclerViewFavorites.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recyclerViewFavorites.setAdapter(newsAdapter);

        Log.d(TAG, "RecyclerView setup completed with database sync");
    }

    private void handleFavoriteRemove(Article article) {
        if (!article.isFavorite()) {
            // Article was unfavorited, remove from list
            favoriteArticles.remove(article);
            newsAdapter.notifyDataSetChanged();

            // Check if list is now empty
            if (favoriteArticles.isEmpty()) {
                showEmptyState();
            }

            Log.d(TAG, "Article removed from favorites: " + article.getTitle());
        }
    }

    private void shareArticle(Article article) {
        try {
            android.content.Intent shareIntent = new android.content.Intent(android.content.Intent.ACTION_SEND);
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

            shareIntent.putExtra(android.content.Intent.EXTRA_TEXT, shareText);
            shareIntent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK);

            startActivity(android.content.Intent.createChooser(shareIntent, "Share article"));
            Log.d(TAG, "Share intent started successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error sharing article: " + e.getMessage(), e);
            if (getContext() != null) {
                android.widget.Toast.makeText(getContext(), "Unable to share article",
                        android.widget.Toast.LENGTH_SHORT).show();
            }
        }
    }

    public void refreshFavorites() {
        Log.d(TAG, "Refresh favorites called from MainActivity");
        if (isAdded() && getContext() != null) {
            loadFavorites();
        }
    }

    private void loadFavorites() {
        // Check if fragment is still valid and executor is available
        if (!isAdded() || getContext() == null || executorService == null || executorService.isShutdown()) {
            Log.w(TAG, "Fragment not ready or executor unavailable, skipping favorites load");
            return;
        }

        showLoading();

        executorService.execute(() -> {
            Log.d(TAG, "Loading favorite articles from SQLite database");

            try {
                List<Article> favorites = articleDao.getFavoriteArticles();

                // Check if fragment is still valid before updating UI
                if (isAdded() && getContext() != null && mainHandler != null) {
                    mainHandler.post(() -> {
                        // Double check if fragment is still valid
                        if (!isAdded() || getContext() == null) {
                            Log.w(TAG, "Fragment not attached, skipping favorites UI update");
                            return;
                        }

                        hideLoading();

                        favoriteArticles.clear();
                        if (favorites != null && !favorites.isEmpty()) {
                            Log.d(TAG, "Loaded " + favorites.size() + " favorite articles from SQLite");
                            favoriteArticles.addAll(favorites);
                            newsAdapter.notifyDataSetChanged();
                            showContent();
                        } else {
                            Log.d(TAG, "No favorite articles found in SQLite database");
                            showEmptyState();
                        }
                    });
                }

            } catch (Exception e) {
                Log.e(TAG, "Error loading favorite articles from SQLite", e);
                if (isAdded() && getContext() != null && mainHandler != null) {
                    mainHandler.post(() -> {
                        if (isAdded() && getContext() != null) {
                            hideLoading();
                            showEmptyState();
                        }
                    });
                }
            }
        });
    }

    private void showLoading() {
        Log.d(TAG, "Showing loading state");
        if (binding != null && isAdded()) {
            binding.progressBar.setVisibility(View.VISIBLE);
            binding.recyclerViewFavorites.setVisibility(View.GONE);
            binding.layoutEmpty.setVisibility(View.GONE);
        }
    }

    private void hideLoading() {
        Log.d(TAG, "Hiding loading state");
        if (binding != null && isAdded()) {
            binding.progressBar.setVisibility(View.GONE);
        }
    }

    private void showContent() {
        Log.d(TAG, "Showing content - Favorite articles count: " + favoriteArticles.size());
        if (binding != null && isAdded()) {
            binding.recyclerViewFavorites.setVisibility(View.VISIBLE);
            binding.layoutEmpty.setVisibility(View.GONE);
        }
    }

    private void showEmptyState() {
        Log.d(TAG, "Showing empty state");
        if (binding != null && isAdded()) {
            binding.recyclerViewFavorites.setVisibility(View.GONE);
            binding.layoutEmpty.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "Fragment resumed - refreshing favorites from SQLite");
        loadFavorites();
    }

    @Override
    public void onDestroyView() {
        Log.d(TAG, "onDestroyView called");

        if (newsAdapter != null) {
            newsAdapter.cleanup();
        }

        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy called");

        if (newsAdapter != null) {
            newsAdapter.cleanup();
        }

        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
            Log.d(TAG, "ExecutorService shutdown");
        }

        super.onDestroy();
        Log.d(TAG, "FavoritesFragment destroyed - SQLite connections will be closed automatically");
    }
}