package com.example.snapnews.fragment;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.snapnews.adapter.NewsAdapter;
import com.example.snapnews.database.ArticleDao;
import com.example.snapnews.database.NewsDatabase;
import com.example.snapnews.databinding.FragmentFavoritesBinding;
import com.example.snapnews.models.Article;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FavoritesFragment extends Fragment {
    private FragmentFavoritesBinding binding;
    private NewsAdapter newsAdapter;
    private List<Article> favoriteArticles = new ArrayList<>();
    private ArticleDao articleDao;
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
        NewsDatabase database = NewsDatabase.getDatabase(requireContext());
        articleDao = database.articleDao();
        executorService = Executors.newFixedThreadPool(1);
        mainHandler = new Handler(Looper.getMainLooper());
    }

    private void setupRecyclerView() {
        newsAdapter = new NewsAdapter(favoriteArticles, article -> {
            if (getActivity() != null) {
                ((com.example.snapnews.activity.MainActivity) getActivity()).navigateToDetail(article);
            }
        });

        binding.recyclerViewFavorites.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recyclerViewFavorites.setAdapter(newsAdapter);
    }

    private void loadFavorites() {
        showLoading();

        executorService.execute(() -> {
            List<Article> favorites = articleDao.getFavoriteArticles();

            mainHandler.post(() -> {
                hideLoading();

                favoriteArticles.clear();
                if (favorites != null) {
                    favoriteArticles.addAll(favorites);
                }
                newsAdapter.notifyDataSetChanged();

                if (favoriteArticles.isEmpty()) {
                    showEmptyState();
                } else {
                    showContent();
                }
            });
        });
    }

    private void showLoading() {
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.recyclerViewFavorites.setVisibility(View.GONE);
        binding.layoutEmpty.setVisibility(View.GONE);
    }

    private void hideLoading() {
        binding.progressBar.setVisibility(View.GONE);
    }

    private void showContent() {
        binding.recyclerViewFavorites.setVisibility(View.VISIBLE);
        binding.layoutEmpty.setVisibility(View.GONE);
    }

    private void showEmptyState() {
        binding.recyclerViewFavorites.setVisibility(View.GONE);
        binding.layoutEmpty.setVisibility(View.VISIBLE);
    }

    @Override
    public void onResume() {
        super.onResume();
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
    }
}