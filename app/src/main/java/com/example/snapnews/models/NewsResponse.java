package com.example.snapnews.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class NewsResponse {
    @SerializedName("status")
    private String status;

    @SerializedName("totalResults")
    private int totalResults;

    @SerializedName("articles")
    private List<Article> articles;

    @SerializedName("message")
    private String message;

    public NewsResponse() {}

    public String getStatus() { return status; }

    public int getTotalResults() { return totalResults; }

    public List<Article> getArticles() { return articles; }

    public String getMessage() { return message; }
}