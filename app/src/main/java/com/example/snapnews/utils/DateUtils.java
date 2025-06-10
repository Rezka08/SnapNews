package com.example.snapnews.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class DateUtils {
    private static final String API_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss'Z'";
    private static final String DISPLAY_DATE_FORMAT = "MMM dd, yyyy";

    public static String formatDate(String dateString) {
        try {
            SimpleDateFormat apiFormat = new SimpleDateFormat(API_DATE_FORMAT, Locale.getDefault());
            apiFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
            Date date = apiFormat.parse(dateString);

            SimpleDateFormat displayFormat = new SimpleDateFormat(DISPLAY_DATE_FORMAT, Locale.getDefault());
            return displayFormat.format(date);
        } catch (ParseException e) {
            e.printStackTrace();
            return dateString;
        }
    }

    public static String getTimeAgo(String dateString) {
        try {
            SimpleDateFormat apiFormat = new SimpleDateFormat(API_DATE_FORMAT, Locale.getDefault());
            apiFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
            Date date = apiFormat.parse(dateString);

            long timeMillis = date.getTime();
            long currentTimeMillis = System.currentTimeMillis();
            long timeDifference = currentTimeMillis - timeMillis;

            long seconds = timeDifference / 1000;
            long minutes = seconds / 60;
            long hours = minutes / 60;
            long days = hours / 24;

            if (days > 0) {
                return days == 1 ? "1 day ago" : days + " days ago";
            } else if (hours > 0) {
                return hours == 1 ? "1 hour ago" : hours + " hours ago";
            } else if (minutes > 0) {
                return minutes == 1 ? "1 minute ago" : minutes + " minutes ago";
            } else {
                return "Just now";
            }
        } catch (ParseException e) {
            e.printStackTrace();
            return formatDate(dateString);
        }
    }
}