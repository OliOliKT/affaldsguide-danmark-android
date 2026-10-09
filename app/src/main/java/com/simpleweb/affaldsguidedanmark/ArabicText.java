package com.simpleweb.affaldsguidedanmark;

import android.content.res.Resources;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;

/** Reads the shared translation database used by the iOS app. */
final class ArabicText {
    private static Map<String, Map<String, String>> translations;

    private ArabicText() {}

    static String lookup(Resources resources, String key) {
        if (translations == null) {
            synchronized (ArabicText.class) {
                if (translations == null) {
                    try (InputStream stream = resources.openRawResource(R.raw.ui_translations);
                         InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                        Type type = new TypeToken<Map<String, Map<String, String>>>() {}.getType();
                        translations = new Gson().fromJson(reader, type);
                    } catch (Exception error) {
                        Log.e("ArabicText", "Could not read UI translations", error);
                        translations = Collections.emptyMap();
                    }
                }
            }
        }
        Map<String, String> entry = translations.get(key);
        return entry == null ? null : entry.get("ar");
    }

    static String value(Resources resources, String key, String fallback) {
        String translation = lookup(resources, key);
        return translation == null || translation.isEmpty() ? fallback : translation;
    }
}
