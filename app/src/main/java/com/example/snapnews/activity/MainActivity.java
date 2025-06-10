package com.example.snapnews.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.fragment.app.Fragment;

import com.example.snapnews.activity.DetailActivity;
import com.example.snapnews.models.Article;
import com.example.snapnews.fragment.FavoritesFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.example.snapnews.R;
import com.example.snapnews.databinding.ActivityMainBinding;
import com.example.snapnews.utils.ApiKeyManager;
import com.example.snapnews.utils.ThemeManager;

public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding binding;
    private ThemeManager themeManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        themeManager = ThemeManager.getInstance(this);
        themeManager.initializeTheme();

        super.onCreate(savedInstanceState);

        validateApiKeyConfiguration();

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupToolbar();
        setupBottomNavigation();
    }

    private void validateApiKeyConfiguration() {
        try {
            if (!ApiKeyManager.isApiKeyConfigured()) {
                throw new IllegalStateException("API Key not configured");
            }
        } catch (Exception e) {
            new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Configuration Error")
                    .setMessage("NEWS_API_KEY not found!\n\n" +
                            "Please add it to local.properties file:\n" +
                            "NEWS_API_KEY=your_actual_api_key_here")
                    .setPositiveButton("OK", (dialog, which) -> finish())
                    .setCancelable(false)
                    .show();
        }
    }

    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        binding.toolbarTitle.setText(getString(R.string.app_name));
    }

    private void setupBottomNavigation() {
        BottomNavigationView navView = binding.navView;

        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main);

        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_home, R.id.navigation_search, R.id.navigation_favorites)
                .build();

        NavigationUI.setupWithNavController(navView, navController);

        navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
            binding.toolbarTitle.setText(getString(R.string.app_name));
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_settings) {
            openSettings();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void openSettings() {
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main);
        return NavigationUI.navigateUp(navController, new AppBarConfiguration.Builder().build())
                || super.onSupportNavigateUp();
    }

    public void navigateToDetail(Article article) {
        Intent intent = new Intent(this, DetailActivity.class);
        intent.putExtra(DetailActivity.EXTRA_ARTICLE_TITLE, article.getTitle());
        intent.putExtra(DetailActivity.EXTRA_ARTICLE_DESCRIPTION, article.getDescription());
        intent.putExtra(DetailActivity.EXTRA_ARTICLE_URL, article.getUrl());
        intent.putExtra(DetailActivity.EXTRA_ARTICLE_IMAGE_URL, article.getUrlToImage());
        intent.putExtra(DetailActivity.EXTRA_ARTICLE_PUBLISHED_AT, article.getPublishedAt());
        intent.putExtra(DetailActivity.EXTRA_ARTICLE_CONTENT, article.getContent());
        intent.putExtra(DetailActivity.EXTRA_ARTICLE_AUTHOR, article.getAuthor());
        if (article.getSource() != null) {
            intent.putExtra(DetailActivity.EXTRA_ARTICLE_SOURCE, article.getSource().getName());
        }
        startActivity(intent);
    }

    public void onFavoriteChanged(Article article) {
        android.util.Log.d("MainActivity", "Favorite changed: " + article.getTitle() +
                " -> " + article.isFavorite());

        // Refresh favorites fragment jika sedang aktif atau akan dibuka
        refreshFavoritesFragment();
    }

    private void refreshFavoritesFragment() {
        try {
            // Find current fragment
            NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main);
            Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment_activity_main);

            if (currentFragment != null) {
                // Check if current fragment container has FavoritesFragment
                Fragment navHostFragment = currentFragment.getChildFragmentManager().getFragments().get(0);
                if (navHostFragment instanceof FavoritesFragment) {
                    // Refresh favorites fragment
                    ((FavoritesFragment) navHostFragment).refreshFavorites();
                    android.util.Log.d("MainActivity", "Refreshed FavoritesFragment");
                }
            }
        } catch (Exception e) {
            android.util.Log.e("MainActivity", "Error refreshing favorites fragment", e);
        }
    }

    public void refreshFavoritesOnNavigation() {
        // Delay sedikit untuk memastikan fragment sudah loaded
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
            refreshFavoritesFragment();
        }, 100);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (themeManager != null) {
            themeManager.initializeTheme();
        }

        refreshFavoritesOnNavigation();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}