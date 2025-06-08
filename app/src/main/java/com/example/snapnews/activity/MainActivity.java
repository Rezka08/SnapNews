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

import com.example.snapnews.activity.DetailActivity;
import com.example.snapnews.models.Article;
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

        // Disable default title and use custom title
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        // Set custom title
        binding.toolbarTitle.setText(getString(R.string.app_name));
    }

    private void setupBottomNavigation() {
        BottomNavigationView navView = binding.navView;

        // Setup navigation controller
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main);

        // Setup app bar configuration WITHOUT automatic title changes
        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_home, R.id.navigation_search, R.id.navigation_favorites)
                .build();

        // Only setup bottom navigation, NOT the action bar
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

    @Override
    protected void onResume() {
        super.onResume();
        if (themeManager != null) {
            themeManager.initializeTheme();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}