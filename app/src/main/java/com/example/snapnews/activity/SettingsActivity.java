package com.example.snapnews.activity;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.MenuItem;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.example.snapnews.R;
import com.example.snapnews.databinding.ActivitySettingsBinding;
import com.example.snapnews.utils.ThemeManager;

public class SettingsActivity extends AppCompatActivity {
    private ActivitySettingsBinding binding;
    private ThemeManager themeManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        themeManager = ThemeManager.getInstance(this);
        themeManager.initializeTheme();

        super.onCreate(savedInstanceState);
        binding = ActivitySettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupToolbar();
        setupThemeSettings();
    }

    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Settings");
        }
    }

    private void setupThemeSettings() {
        // Update current theme display
        updateThemeDisplay();

        // Theme selection click listener
        binding.themeCard.setOnClickListener(v -> showThemeSelectionDialog());
    }

    private void updateThemeDisplay() {
        String currentTheme = themeManager.getCurrentThemeName();
        binding.textThemeSelected.setText(currentTheme);

        // Update description based on current theme
        String description;
        switch (themeManager.getCurrentThemeMode()) {
            case ThemeManager.THEME_LIGHT:
                description = "Always use light theme";
                break;
            case ThemeManager.THEME_DARK:
                description = "Always use dark theme";
                break;
            case ThemeManager.THEME_SYSTEM:
            default:
                description = "Follow system setting";
                break;
        }
        binding.textThemeDescription.setText(description);
    }

    private void showThemeSelectionDialog() {
        String[] themeOptions = ThemeManager.getThemeNames();
        int currentSelection = themeManager.getCurrentThemeMode();

        new AlertDialog.Builder(this)
                .setTitle("Choose Theme")
                .setSingleChoiceItems(themeOptions, currentSelection, (dialog, which) -> {
                    // Only proceed if theme actually changed
                    if (which != currentSelection) {
                        themeManager.setThemeMode(which);
                        updateThemeDisplay();
                        dialog.dismiss();

                        // Show a toast to indicate theme change
                        String themeName = ThemeManager.getThemeNames()[which];
                        android.widget.Toast.makeText(this,
                                "Theme changed to " + themeName,
                                android.widget.Toast.LENGTH_SHORT).show();

                        // Delay the recreation slightly to allow UI to settle
                        new Handler(Looper.getMainLooper()).postDelayed(() -> {
                            // Close SettingsActivity gracefully and let MainActivity handle the theme change
                            finish();
                        }, 500);
                    } else {
                        dialog.dismiss();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}