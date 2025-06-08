package com.example.snapnews.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import androidx.appcompat.app.AppCompatDelegate;

public class ThemeManager {
    private static final String PREFS_NAME = "SnapNewsThemePrefs";
    private static final String KEY_THEME_MODE = "theme_mode";

    // Theme modes
    public static final int THEME_LIGHT = 0;
    public static final int THEME_DARK = 1;
    public static final int THEME_SYSTEM = 2;

    // Theme names for display
    public static final String[] THEME_NAMES = {
            "Light", "Dark", "Match System"
    };

    private static ThemeManager instance;
    private final SharedPreferences prefs;
    private final Context context;

    private ThemeManager(Context context) {
        this.context = context.getApplicationContext();
        this.prefs = this.context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static synchronized ThemeManager getInstance(Context context) {
        if (instance == null) {
            instance = new ThemeManager(context);
        }
        return instance;
    }

    /**
     * Get current theme mode
     */
    public int getCurrentThemeMode() {
        return prefs.getInt(KEY_THEME_MODE, THEME_SYSTEM);
    }

    /**
     * Set theme mode
     */
    public void setThemeMode(int themeMode) {
        prefs.edit().putInt(KEY_THEME_MODE, themeMode).apply();
        applyTheme(themeMode);
    }

    /**
     * Apply theme based on mode
     */
    public void applyTheme(int themeMode) {
        switch (themeMode) {
            case THEME_LIGHT:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                break;
            case THEME_DARK:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                break;
            case THEME_SYSTEM:
            default:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                break;
        }
    }

    /**
     * Initialize theme on app start
     */
    public void initializeTheme() {
        int currentMode = getCurrentThemeMode();
        applyTheme(currentMode);
    }

    /**
     * Get theme name for display
     */
    public String getCurrentThemeName() {
        int mode = getCurrentThemeMode();
        if (mode >= 0 && mode < THEME_NAMES.length) {
            return THEME_NAMES[mode];
        }
        return THEME_NAMES[THEME_SYSTEM];
    }

    /**
     * Check if current theme is dark
     */
    public boolean isDarkTheme() {
        int themeMode = getCurrentThemeMode();

        if (themeMode == THEME_DARK) {
            return true;
        } else if (themeMode == THEME_LIGHT) {
            return false;
        } else {
            // THEME_SYSTEM - check system setting
            int nightModeFlags = context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
            return nightModeFlags == Configuration.UI_MODE_NIGHT_YES;
        }
    }

    /**
     * Get all available theme modes
     */
    public static String[] getThemeNames() {
        return THEME_NAMES.clone();
    }

    /**
     * Get theme mode by name
     */
    public static int getThemeModeByName(String themeName) {
        for (int i = 0; i < THEME_NAMES.length; i++) {
            if (THEME_NAMES[i].equals(themeName)) {
                return i;
            }
        }
        return THEME_SYSTEM;
    }
}