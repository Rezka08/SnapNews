package com.example.snapnews.network;

import com.example.snapnews.models.NewsResponse;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface NewsApiService {

    @GET("top-headlines")
    Call<NewsResponse> getTopHeadlines(
            @Query("country") String country,
            @Query("category") String category,
            @Query("pageSize") int pageSize,
            @Query("page") int page,
            @Query("apiKey") String apiKey
    );

    @GET("everything")
    Call<NewsResponse> searchNews(
            @Query("q") String query,
            @Query("sortBy") String sortBy,
            @Query("pageSize") int pageSize,
            @Query("page") int page,
            @Query("apiKey") String apiKey
    );

    @GET("top-headlines")
    Call<NewsResponse> getNewsByCategory(
            @Query("country") String country,
            @Query("category") String category,
            @Query("apiKey") String apiKey
    );
}
