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

        binding.recyclerViewFavorites.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recyclerViewFavorites.setAdapter(newsAdapter);

        Log.d(TAG, "RecyclerView setup completed");
    }

    private void loadFavorites() {
        showLoading();

        executorService.execute(() -> {
            Log.d(TAG, "Loading favorite articles from SQLite database");

            try {
                List<Article> favorites = articleDao.getFavoriteArticles();

                mainHandler.post(() -> {
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

            } catch (Exception e) {
                Log.e(TAG, "Error loading favorite articles from SQLite", e);
                mainHandler.post(() -> {
                    hideLoading();
                    showEmptyState();
                });
            }
        });
    }

    // Method untuk menambah/menghapus favorite (dipanggil dari DetailActivity)
    public void toggleFavorite(Article article) {
        executorService.execute(() -> {
            try {
                Log.d(TAG, "Toggling favorite status for article: " + article.getTitle());

                // Toggle status favorite
                article.setFavorite(!article.isFavorite());

                // Update di database SQLite
                articleDao.updateArticle(article);

                Log.d(TAG, "Article favorite status updated in SQLite: " + article.isFavorite());

                // Refresh tampilan di main thread
                mainHandler.post(() -> {
                    loadFavorites();
                });

            } catch (Exception e) {
                Log.e(TAG, "Error toggling favorite status in SQLite", e);
            }
        });
    }

    private void showLoading() {
        Log.d(TAG, "Showing loading state");
        if (binding != null) {
            binding.progressBar.setVisibility(View.VISIBLE);
            binding.recyclerViewFavorites.setVisibility(View.GONE);
            binding.layoutEmpty.setVisibility(View.GONE);
        }
    }

    private void hideLoading() {
        Log.d(TAG, "Hiding loading state");
        if (binding != null) {
            binding.progressBar.setVisibility(View.GONE);
        }
    }

    private void showContent() {
        Log.d(TAG, "Showing content - Favorite articles count: " + favoriteArticles.size());
        if (binding != null) {
            binding.recyclerViewFavorites.setVisibility(View.VISIBLE);
            binding.layoutEmpty.setVisibility(View.GONE);
        }
    }

    private void showEmptyState() {
        Log.d(TAG, "Showing empty state");
        if (binding != null) {
            binding.recyclerViewFavorites.setVisibility(View.GONE);
            binding.layoutEmpty.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "Fragment resumed - refreshing favorites from SQLite");
        // Refresh favorites when fragment becomes visible
        loadFavorites();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
        binding = null;
        Log.d(TAG, "FavoritesFragment destroyed - SQLite connections will be closed automatically");
    }
}