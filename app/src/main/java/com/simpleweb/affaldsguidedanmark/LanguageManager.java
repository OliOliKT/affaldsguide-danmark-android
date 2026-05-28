package com.simpleweb.affaldsguidedanmark;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.os.LocaleList;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;

import java.util.Locale;

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

    public static Context wrapContext(Context context) {
        String language = getSavedLanguage(context);
        Locale locale = new Locale(language);
        Locale.setDefault(locale);

        Resources resources = context.getResources();
        Configuration configuration = new Configuration(resources.getConfiguration());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            configuration.setLocales(new LocaleList(locale));
        } else {
            configuration.locale = locale;
        }

        return context.createConfigurationContext(configuration);
    }

    private static void setAppLocale(String language) {
        Locale.setDefault(new Locale(language));
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(language));
    }
}
