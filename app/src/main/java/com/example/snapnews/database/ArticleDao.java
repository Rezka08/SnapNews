package com.example.snapnews.database;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import com.example.snapnews.models.Article;
import java.util.List;

@Dao
public interface ArticleDao {
    @Query("SELECT * FROM articles ORDER BY timestamp DESC")
    List<Article> getAllArticles();

    @Query("SELECT * FROM articles WHERE isFavorite = 1 ORDER BY timestamp DESC")
    List<Article> getFavoriteArticles();

    @Query("SELECT * FROM articles WHERE title LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%'")
    List<Article> searchArticles(String query);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertArticle(Article article);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertArticles(List<Article> articles);

    @Update
    void updateArticle(Article article);

    @Delete
    void deleteArticle(Article article);

    @Query("DELETE FROM articles WHERE isFavorite = 0")
    void deleteNonFavoriteArticles();

    @Query("SELECT * FROM articles WHERE url = :url LIMIT 1")
    Article getArticleByUrl(String url);
}