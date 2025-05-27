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

    // INSERT ARTICLE
    public void insertArticle(Article article) {
        SQLiteDatabase db = null;

        try {
            db = dbHelper.getWritableDatabase();
            ContentValues values = articleToContentValues(article);

            // Use INSERT OR REPLACE untuk handle conflict
            long result = db.insertWithOnConflict(
                    NewsDatabaseHelper.TABLE_ARTICLES,
                    null,
                    values,
                    SQLiteDatabase.CONFLICT_REPLACE
            );

            if (result != -1) {
                Log.d(TAG, "Article inserted successfully with ID: " + result);
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

    // INSERT MULTIPLE ARTICLES
    public void insertArticles(List<Article> articles) {
        SQLiteDatabase db = null;

        try {
            db = dbHelper.getWritableDatabase();
            db.beginTransaction();

            for (Article article : articles) {
                ContentValues values = articleToContentValues(article);
                db.insertWithOnConflict(
                        NewsDatabaseHelper.TABLE_ARTICLES,
                        null,
                        values,
                        SQLiteDatabase.CONFLICT_REPLACE
                );
            }

            db.setTransactionSuccessful();
            Log.d(TAG, "Successfully inserted " + articles.size() + " articles");

        } catch (Exception e) {
            Log.e(TAG, "Error inserting articles", e);
        } finally {
            if (db != null) {
                db.endTransaction();
                db.close();
            }
        }
    }

    // UPDATE ARTICLE
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

            Log.d(TAG, "Updated " + rowsAffected + " articles");
        } catch (Exception e) {
            Log.e(TAG, "Error updating article", e);
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

    // DELETE NON-FAVORITE ARTICLES
    public void deleteNonFavoriteArticles() {
        SQLiteDatabase db = null;

        try {
            db = dbHelper.getWritableDatabase();
            int rowsDeleted = db.delete(
                    NewsDatabaseHelper.TABLE_ARTICLES,
                    NewsDatabaseHelper.COLUMN_IS_FAVORITE + " = 0",
                    null
            );

            Log.d(TAG, "Deleted " + rowsDeleted + " non-favorite articles");
        } catch (Exception e) {
            Log.e(TAG, "Error deleting non-favorite articles", e);
        } finally {
            if (db != null) {
                db.close();
            }
        }
    }

    // GET ARTICLE BY URL
    public Article getArticleByUrl(String url) {
        SQLiteDatabase db = null;
        Cursor cursor = null;

        try {
            db = dbHelper.getReadableDatabase();
            String query = "SELECT * FROM " + NewsDatabaseHelper.TABLE_ARTICLES +
                    " WHERE " + NewsDatabaseHelper.COLUMN_URL + " = ? LIMIT 1";

            cursor = db.rawQuery(query, new String[]{url});

            if (cursor.moveToFirst()) {
                Article article = cursorToArticle(cursor);
                Log.d(TAG, "Found article by URL: " + url);
                return article;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting article by URL", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            if (db != null) {
                db.close();
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

            article.setFavorite(cursor.getInt(cursor.getColumnIndexOrThrow(NewsDatabaseHelper.COLUMN_IS_FAVORITE)) == 1);
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

        values.put(NewsDatabaseHelper.COLUMN_IS_FAVORITE, article.isFavorite() ? 1 : 0);
        values.put(NewsDatabaseHelper.COLUMN_TIMESTAMP, article.getTimestamp());

        return values;
    }
}