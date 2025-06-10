package com.example.snapnews.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import androidx.appcompat.app.AppCompatDelegate;

public class ThemeManager {
    private static final String PREFS_NAME = "SnapNewsThemePrefs";
    private static final String KEY_THEME_MODE = "theme_mode";

    public static final int THEME_LIGHT = 0;
    public static final int THEME_DARK = 1;
    public static final int THEME_SYSTEM = 2;

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

    public int getCurrentThemeMode() {
        return prefs.getInt(KEY_THEME_MODE, THEME_SYSTEM);
    }

    public void setThemeMode(int themeMode) {
        prefs.edit().putInt(KEY_THEME_MODE, themeMode).apply();
        applyTheme(themeMode);
    }

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

    public void initializeTheme() {
        int currentMode = getCurrentThemeMode();
        applyTheme(currentMode);
    }

    public String getCurrentThemeName() {
        int mode = getCurrentThemeMode();
        if (mode >= 0 && mode < THEME_NAMES.length) {
            return THEME_NAMES[mode];
        }
        return THEME_NAMES[THEME_SYSTEM];
    }

    public static String[] getThemeNames() {
        return THEME_NAMES.clone();
    }
}