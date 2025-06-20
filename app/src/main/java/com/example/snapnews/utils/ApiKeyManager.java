package com.example.snapnews.utils;

import com.example.snapnews.BuildConfig;

public class ApiKeyManager {

    public static String getNewsApiKey() {
        String apiKey = BuildConfig.NEWS_API_KEY;

        if (apiKey == null || apiKey.isEmpty()) {
            throw new IllegalStateException(
                    "NEWS_API_KEY not found! Please add it to local.properties file.\n" +
                            "Add this line to local.properties:\n" +
                            "NEWS_API_KEY=your_actual_api_key_here"
            );
        }

        return apiKey;
    }

    public static boolean isApiKeyConfigured() {
        String apiKey = BuildConfig.NEWS_API_KEY;
        return apiKey != null && !apiKey.isEmpty();
    }
}