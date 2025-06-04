package com.example.snapnews.database;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import com.example.snapnews.models.Article;
import com.example.snapnews.models.Source;
import java.util.ArrayList;
import java.util.List;

public class ArticleDao {
    private static final String TAG = "ArticleDao";
    private final NewsDatabaseHelper dbHelper;

    public ArticleDao(NewsDatabaseHelper dbHelper) {
        this.dbHelper = dbHelper;
        Log.d(TAG, "ArticleDao initialized with NewsDatabaseHelper");
    }

    // GET ALL ARTICLES
    public List<Article> getAllArticles() {
        List<Article> articles = new ArrayList<>();
        SQLiteDatabase db = null;
        Cursor cursor = null;

        try {
            db = dbHelper.getReadableDatabase();
            String query = "SELECT * FROM " + NewsDatabaseHelper.TABLE_ARTICLES +
                    " ORDER BY " + NewsDatabaseHelper.COLUMN_TIMESTAMP + " DESC";

            cursor = db.rawQuery(query, null);
            Log.d(TAG, "getAllArticles - Found " + cursor.getCount() + " articles");

            if (cursor.moveToFirst()) {
                do {
                    Article article = cursorToArticle(cursor);
                    if (article != null) {
                        articles.add(article);
                    }
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting all articles", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            if (db != null) {
                db.close();
            }
        }

        return articles;
    }

    // GET FAVORITE ARTICLES
    public List<Article> getFavoriteArticles() {
        List<Article> articles = new ArrayList<>();
        SQLiteDatabase db = null;
        Cursor cursor = null;

        try {
            db = dbHelper.getReadableDatabase();
            String query = "SELECT * FROM " + NewsDatabaseHelper.TABLE_ARTICLES +
                    " WHERE " + NewsDatabaseHelper.COLUMN_IS_FAVORITE + " = 1" +
                    " ORDER BY " + NewsDatabaseHelper.COLUMN_TIMESTAMP + " DESC";

            cursor = db.rawQuery(query, null);
            Log.d(TAG, "getFavoriteArticles - Found " + cursor.getCount() + " favorite articles");

            if (cursor.moveToFirst()) {
                do {
                    Article article = cursorToArticle(cursor);
                    if (article != null) {
                        articles.add(article);
                    }
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting favorite articles", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            if (db != null) {
                db.close();
            }
        }

        return articles;
    }

    // SEARCH ARTICLES
    public List<Article> searchArticles(String query) {
        List<Article> articles = new ArrayList<>();
        SQLiteDatabase db = null;
        Cursor cursor = null;

        try {
            db = dbHelper.getReadableDatabase();
            String sqlQuery = "SELECT * FROM " + NewsDatabaseHelper.TABLE_ARTICLES +
                    " WHERE " + NewsDatabaseHelper.COLUMN_TITLE + " LIKE ? OR " +
                    NewsDatabaseHelper.COLUMN_DESCRIPTION + " LIKE ?" +
                    " ORDER BY " + NewsDatabaseHelper.COLUMN_TIMESTAMP + " DESC";

            String searchPattern = "%" + query + "%";
            String[] selectionArgs = {searchPattern, searchPattern};

            cursor = db.rawQuery(sqlQuery, selectionArgs);
            Log.d(TAG, "searchArticles - Found " + cursor.getCount() + " articles for query: " + query);

            if (cursor.moveToFirst()) {
                do {
                    Article article = cursorToArticle(cursor);
                    if (article != null) {
                        articles.add(article);
                    }
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error searching articles", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            if (db != null) {
                db.close();
            }
        }

        return articles;
    }

    // INSERT ARTICLE - PRESERVE EXISTING FAVORITE STATUS
    public void insertArticle(Article article) {
        SQLiteDatabase db = null;

        try {
            db = dbHelper.getWritableDatabase();

            // CRITICAL: Check if article already exists and preserve favorite status
            Article existingArticle = getArticleByUrlInternal(db, article.getUrl());
            if (existingArticle != null) {
                // Preserve favorite status from existing article
                article.setFavorite(existingArticle.isFavorite());
                article.setId(existingArticle.getId());
                Log.d(TAG, "Preserving favorite status: " + article.isFavorite() + " for article: " + article.getTitle());
            }

            ContentValues values = articleToContentValues(article);

            long result = db.insertWithOnConflict(
                    NewsDatabaseHelper.TABLE_ARTICLES,
                    null,
                    values,
                    SQLiteDatabase.CONFLICT_REPLACE
            );

            if (result != -1) {
                Log.d(TAG, "Article inserted/updated successfully with ID: " + result);
            } else {
                Log.e(TAG, "Failed to insert article");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error inserting article", e);
        } finally {
            if (db != null) {
                db.close();
            }
        }
    }

    // INSERT MULTIPLE ARTICLES - PRESERVE EXISTING FAVORITE STATUS
    public void insertArticles(List<Article> articles) {
        SQLiteDatabase db = null;

        try {
            db = dbHelper.getWritableDatabase();
            db.beginTransaction();

            for (Article article : articles) {
                // CRITICAL: Check and preserve favorite status for each article
                Article existingArticle = getArticleByUrlInternal(db, article.getUrl());
                if (existingArticle != null) {
                    // Preserve favorite status
                    article.setFavorite(existingArticle.isFavorite());
                    Log.d(TAG, "Preserving favorite status: " + article.isFavorite() + " for: " + article.getTitle());
                }

                ContentValues values = articleToContentValues(article);
                db.insertWithOnConflict(
                        NewsDatabaseHelper.TABLE_ARTICLES,
                        null,
                        values,
                        SQLiteDatabase.CONFLICT_REPLACE
                );
            }

            db.setTransactionSuccessful();
            Log.d(TAG, "Successfully inserted " + articles.size() + " articles with preserved favorites");

        } catch (Exception e) {
            Log.e(TAG, "Error inserting articles", e);
        } finally {
            if (db != null) {
                db.endTransaction();
                db.close();
            }
        }
    }

    // UPDATE ARTICLE - SPECIFICALLY FOR FAVORITE TOGGLE
    public void updateArticle(Article article) {
        SQLiteDatabase db = null;

        try {
            db = dbHelper.getWritableDatabase();
            ContentValues values = articleToContentValues(article);

            int rowsAffected = db.update(
                    NewsDatabaseHelper.TABLE_ARTICLES,
                    values,
                    NewsDatabaseHelper.COLUMN_URL + " = ?",
                    new String[]{article.getUrl()}
            );

            Log.d(TAG, "Updated " + rowsAffected + " articles - Favorite status: " + article.isFavorite());
        } catch (Exception e) {
            Log.e(TAG, "Error updating article", e);
        } finally {
            if (db != null) {
                db.close();
            }
        }
    }

    // UPDATE FAVORITE STATUS ONLY
    public void updateFavoriteStatus(String url, boolean isFavorite) {
        SQLiteDatabase db = null;

        try {
            db = dbHelper.getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put(NewsDatabaseHelper.COLUMN_IS_FAVORITE, isFavorite ? 1 : 0);

            int rowsAffected = db.update(
                    NewsDatabaseHelper.TABLE_ARTICLES,
                    values,
                    NewsDatabaseHelper.COLUMN_URL + " = ?",
                    new String[]{url}
            );

            Log.d(TAG, "Updated favorite status for " + rowsAffected + " articles to: " + isFavorite);
        } catch (Exception e) {
            Log.e(TAG, "Error updating favorite status", e);
        } finally {
            if (db != null) {
                db.close();
            }
        }
    }

    // DELETE ARTICLE
    public void deleteArticle(Article article) {
        SQLiteDatabase db = null;

        try {
            db = dbHelper.getWritableDatabase();
            int rowsDeleted = db.delete(
                    NewsDatabaseHelper.TABLE_ARTICLES,
                    NewsDatabaseHelper.COLUMN_URL + " = ?",
                    new String[]{article.getUrl()}
            );

            Log.d(TAG, "Deleted " + rowsDeleted + " articles");
        } catch (Exception e) {
            Log.e(TAG, "Error deleting article", e);
        } finally {
            if (db != null) {
                db.close();
            }
        }
    }

    // DELETE NON-FAVORITE ARTICLES - IMPROVED VERSION
    public void deleteNonFavoriteArticles() {
        SQLiteDatabase db = null;

        try {
            db = dbHelper.getWritableDatabase();

            // Get count of favorites before deletion for logging
            Cursor favoriteCursor = db.rawQuery(
                    "SELECT COUNT(*) FROM " + NewsDatabaseHelper.TABLE_ARTICLES +
                            " WHERE " + NewsDatabaseHelper.COLUMN_IS_FAVORITE + " = 1", null);

            int favoriteCount = 0;
            if (favoriteCursor.moveToFirst()) {
                favoriteCount = favoriteCursor.getInt(0);
            }
            favoriteCursor.close();

            // Delete non-favorites
            int rowsDeleted = db.delete(
                    NewsDatabaseHelper.TABLE_ARTICLES,
                    NewsDatabaseHelper.COLUMN_IS_FAVORITE + " = 0",
                    null
            );

            Log.d(TAG, "Deleted " + rowsDeleted + " non-favorite articles, preserved " + favoriteCount + " favorites");
        } catch (Exception e) {
            Log.e(TAG, "Error deleting non-favorite articles", e);
        } finally {
            if (db != null) {
                db.close();
            }
        }
    }

    // GET ARTICLE BY URL - PUBLIC METHOD
    public Article getArticleByUrl(String url) {
        SQLiteDatabase db = null;
        try {
            db = dbHelper.getReadableDatabase();
            return getArticleByUrlInternal(db, url);
        } finally {
            if (db != null) {
                db.close();
            }
        }
    }

    // GET ARTICLE BY URL - INTERNAL METHOD (REUSES DB CONNECTION)
    private Article getArticleByUrlInternal(SQLiteDatabase db, String url) {
        Cursor cursor = null;

        try {
            String query = "SELECT * FROM " + NewsDatabaseHelper.TABLE_ARTICLES +
                    " WHERE " + NewsDatabaseHelper.COLUMN_URL + " = ? LIMIT 1";

            cursor = db.rawQuery(query, new String[]{url});

            if (cursor.moveToFirst()) {
                Article article = cursorToArticle(cursor);
                Log.d(TAG, "Found article by URL: " + url + ", Favorite: " + (article != null ? article.isFavorite() : "null"));
                return article;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting article by URL", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return null;
    }

    // HELPER: Convert Cursor to Article object
    private Article cursorToArticle(Cursor cursor) {
        try {
            Article article = new Article();

            article.setId(cursor.getInt(cursor.getColumnIndexOrThrow(NewsDatabaseHelper.COLUMN_ID)));
            article.setTitle(cursor.getString(cursor.getColumnIndexOrThrow(NewsDatabaseHelper.COLUMN_TITLE)));
            article.setDescription(cursor.getString(cursor.getColumnIndexOrThrow(NewsDatabaseHelper.COLUMN_DESCRIPTION)));
            article.setUrl(cursor.getString(cursor.getColumnIndexOrThrow(NewsDatabaseHelper.COLUMN_URL)));
            article.setUrlToImage(cursor.getString(cursor.getColumnIndexOrThrow(NewsDatabaseHelper.COLUMN_URL_TO_IMAGE)));
            article.setPublishedAt(cursor.getString(cursor.getColumnIndexOrThrow(NewsDatabaseHelper.COLUMN_PUBLISHED_AT)));
            article.setContent(cursor.getString(cursor.getColumnIndexOrThrow(NewsDatabaseHelper.COLUMN_CONTENT)));
            article.setAuthor(cursor.getString(cursor.getColumnIndexOrThrow(NewsDatabaseHelper.COLUMN_AUTHOR)));

            // Create Source object
            String sourceId = cursor.getString(cursor.getColumnIndexOrThrow(NewsDatabaseHelper.COLUMN_SOURCE_ID));
            String sourceName = cursor.getString(cursor.getColumnIndexOrThrow(NewsDatabaseHelper.COLUMN_SOURCE_NAME));
            if (sourceId != null || sourceName != null) {
                Source source = new Source(sourceId, sourceName);
                article.setSource(source);
            }

            // CRITICAL: Properly set favorite status
            int favoriteInt = cursor.getInt(cursor.getColumnIndexOrThrow(NewsDatabaseHelper.COLUMN_IS_FAVORITE));
            article.setFavorite(favoriteInt == 1);

            article.setTimestamp(cursor.getLong(cursor.getColumnIndexOrThrow(NewsDatabaseHelper.COLUMN_TIMESTAMP)));

            return article;

        } catch (Exception e) {
            Log.e(TAG, "Error converting cursor to article", e);
            return null;
        }
    }

    // HELPER: Convert Article to ContentValues
    private ContentValues articleToContentValues(Article article) {
        ContentValues values = new ContentValues();

        values.put(NewsDatabaseHelper.COLUMN_TITLE, article.getTitle());
        values.put(NewsDatabaseHelper.COLUMN_DESCRIPTION, article.getDescription());
        values.put(NewsDatabaseHelper.COLUMN_URL, article.getUrl());
        values.put(NewsDatabaseHelper.COLUMN_URL_TO_IMAGE, article.getUrlToImage());
        values.put(NewsDatabaseHelper.COLUMN_PUBLISHED_AT, article.getPublishedAt());
        values.put(NewsDatabaseHelper.COLUMN_CONTENT, article.getContent());
        values.put(NewsDatabaseHelper.COLUMN_AUTHOR, article.getAuthor());

        // Handle Source object - flatten to separate columns
        if (article.getSource() != null) {
            values.put(NewsDatabaseHelper.COLUMN_SOURCE_ID, article.getSource().getId());
            values.put(NewsDatabaseHelper.COLUMN_SOURCE_NAME, article.getSource().getName());
        } else {
            values.putNull(NewsDatabaseHelper.COLUMN_SOURCE_ID);
            values.putNull(NewsDatabaseHelper.COLUMN_SOURCE_NAME);
        }

        // CRITICAL: Properly save favorite status
        values.put(NewsDatabaseHelper.COLUMN_IS_FAVORITE, article.isFavorite() ? 1 : 0);
        values.put(NewsDatabaseHelper.COLUMN_TIMESTAMP, article.getTimestamp());

        return values;
    }

    // DEBUG: Method to check favorite count
    public int getFavoriteCount() {
        SQLiteDatabase db = null;
        Cursor cursor = null;

        try {
            db = dbHelper.getReadableDatabase();
            cursor = db.rawQuery("SELECT COUNT(*) FROM " + NewsDatabaseHelper.TABLE_ARTICLES +
                    " WHERE " + NewsDatabaseHelper.COLUMN_IS_FAVORITE + " = 1", null);

            if (cursor.moveToFirst()) {
                int count = cursor.getInt(0);
                Log.d(TAG, "Current favorite count: " + count);
                return count;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting favorite count", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            if (db != null) {
                db.close();
            }
        }

        return 0;
    }
}