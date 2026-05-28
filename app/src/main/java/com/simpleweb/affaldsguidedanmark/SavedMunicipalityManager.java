package com.simpleweb.affaldsguidedanmark;

import android.content.Context;
import android.content.SharedPreferences;

public final class SavedMunicipalityManager {
    private static final String PREFS_NAME = "saved_municipality_preferences";
    private static final String KEY_MUNICIPALITY_NAME = "saved_municipality_name";

    private SavedMunicipalityManager() {
    }

    public static void save(Context context, String municipalityName) {
        if (municipalityName == null || municipalityName.trim().isEmpty()) {
            return;
        }

        getPreferences(context)
                .edit()
                .putString(KEY_MUNICIPALITY_NAME, municipalityName)
                .apply();
    }

    public static void remove(Context context) {
        getPreferences(context)
                .edit()
                .remove(KEY_MUNICIPALITY_NAME)
                .apply();
    }

    public static String getSavedMunicipalityName(Context context) {
        return getPreferences(context).getString(KEY_MUNICIPALITY_NAME, "");
    }

    public static boolean isSaved(Context context, String municipalityName) {
        String savedMunicipalityName = getSavedMunicipalityName(context);
        return savedMunicipalityName != null
                && municipalityName != null
                && savedMunicipalityName.equalsIgnoreCase(municipalityName);
    }

    private static SharedPreferences getPreferences(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
}
