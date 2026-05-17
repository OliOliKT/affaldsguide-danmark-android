package com.simpleweb.affaldsguidedanmark;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;

public class LanguageManager {
    private static final String PREFS_NAME = "LanguagePreferences";
    private static final String LANGUAGE_KEY = "language";
    public static final String DANISH = "da";
    public static final String ENGLISH = "en";

    public static void applySavedLanguage(Context context) {
        setAppLocale(getSavedLanguage(context));
    }

    public static void saveLanguage(Context context, String language) {
        SharedPreferences preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        preferences.edit().putString(LANGUAGE_KEY, language).apply();
        setAppLocale(language);
    }

    public static String getSavedLanguage(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return preferences.getString(LANGUAGE_KEY, DANISH);
    }

    public static boolean isEnglish(Context context) {
        return ENGLISH.equals(getSavedLanguage(context));
    }

    private static void setAppLocale(String language) {
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(language));
    }
}
