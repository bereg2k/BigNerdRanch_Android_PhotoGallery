package com.bignerdranch.android.photogallery;

import android.content.Context;
import android.preference.PreferenceManager;

/**
 * Utility class to work with SharedPreferences and store search queries or result ids
 */
public class QueryPreferences {
    private static final String PREF_SEARCH_QUERY = "searchQuery";
    private static final String PREF_LAST_RESULT_ID = "lastResultId";

    private QueryPreferences() {
        throw new IllegalStateException("This is a Utility class!");
    }

    /**
     * get a current search query from internal memory
     *
     * @param context current context
     * @return current search query
     */
    public static String getStoredQuery(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getString(PREF_SEARCH_QUERY, null);
    }

    /**
     * get ID from the latest request's results.
     *
     * @param context current context
     * @return ID from the latest request's results
     */
    public static String getLastResultId(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getString(PREF_LAST_RESULT_ID, null);
    }

    /**
     * save a current search query to the internal memory
     *
     * @param context current context
     */
    public static void setPrefSearchQuery(Context context, String query) {
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putString(PREF_SEARCH_QUERY, query)
                .apply();
    }

    /**
     * save ID from the latest request's results.
     *
     * @param context current context
     */
    public static void setLastResultId(Context context, String lastResultId) {
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putString(PREF_LAST_RESULT_ID, lastResultId)
                .apply();
    }
}
