package com.example.snapnews.activity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
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

public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding binding;
    private SharedPreferences sharedPreferences;
    private static final String PREFS_NAME = "SnapNewsPrefs";
    private static final String KEY_DARK_MODE = "dark_mode";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Initialize theme before super.onCreate()
        initializeTheme();

        super.onCreate(savedInstanceState);

        // Validate API key configuration
        validateApiKeyConfiguration();

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);

        setupBottomNavigation();
        setupSharedPreferences();
    }

    private void validateApiKeyConfiguration() {
        try {
            if (!ApiKeyManager.isApiKeyConfigured()) {
                throw new IllegalStateException("API Key not configured");
            }
        } catch (Exception e) {
            // Show error dialog and exit
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

    private void initializeTheme() {
        sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        boolean isDarkMode = sharedPreferences.getBoolean(KEY_DARK_MODE, false);

        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
    }

    private void setupBottomNavigation() {
        BottomNavigationView navView = binding.navView;

        // Setup navigation controller
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main);

        // Setup app bar configuration
        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_home, R.id.navigation_search, R.id.navigation_favorites)
                .build();

        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(navView, navController);
    }

    private void setupSharedPreferences() {
        sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);

        // Update theme icon based on current mode
        MenuItem themeItem = menu.findItem(R.id.action_toggle_theme);
        boolean isDarkMode = sharedPreferences.getBoolean(KEY_DARK_MODE, false);
        themeItem.setIcon(isDarkMode ? R.drawable.ic_light_mode : R.drawable.ic_dark_mode);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_toggle_theme) {
            toggleTheme();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void toggleTheme() {
        boolean isDarkMode = sharedPreferences.getBoolean(KEY_DARK_MODE, false);
        boolean newMode = !isDarkMode;

        // Save preference
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(KEY_DARK_MODE, newMode);
        editor.apply();

        // Apply theme
        if (newMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }

        // Recreate activity to apply theme
        recreate();
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
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}