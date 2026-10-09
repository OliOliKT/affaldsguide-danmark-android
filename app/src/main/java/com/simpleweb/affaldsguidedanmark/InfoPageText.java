package com.simpleweb.affaldsguidedanmark;

import android.content.Context;
import android.graphics.Typeface;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.StyleSpan;
import android.text.util.Linkify;
import android.widget.TextView;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;

/** Displays the three-language information pages from the bundled translation JSON. */
final class InfoPageText {
    private static Map<String, Map<String, String>> translations;

    private InfoPageText() {}

    static String get(Context context, String key) {
        if (translations == null) {
            synchronized (InfoPageText.class) {
                if (translations == null) {
                    try (InputStream stream = context.getResources().openRawResource(R.raw.ui_translations);
                         InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                        Type type = new TypeToken<Map<String, Map<String, String>>>() {}.getType();
                        translations = new Gson().fromJson(reader, type);
                    } catch (Exception error) {
                        translations = Collections.emptyMap();
                    }
                }
            }
        }
        Map<String, String> entry = translations.get(key);
        if (entry == null) return "";
        String language = LanguageManager.getSavedLanguage(context);
        String text = entry.get(language);
        return text != null ? text : entry.getOrDefault("da", "");
    }

    static void set(TextView view, String key) {
        String raw = get(view.getContext(), key);
        SpannableStringBuilder styled = new SpannableStringBuilder();
        int index = 0;
        while (index < raw.length()) {
            if (raw.startsWith("**", index)) {
                int end = raw.indexOf("**", index + 2);
                if (end >= 0) {
                    int start = styled.length();
                    styled.append(raw, index + 2, end);
                    styled.setSpan(new StyleSpan(Typeface.BOLD), start, styled.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    index = end + 2;
                    continue;
                }
            }
            if (raw.charAt(index) == '*') {
                int end = raw.indexOf('*', index + 1);
                if (end >= 0) {
                    int start = styled.length();
                    styled.append(raw, index + 1, end);
                    styled.setSpan(new StyleSpan(Typeface.ITALIC), start, styled.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    index = end + 1;
                    continue;
                }
            }
            styled.append(raw.charAt(index++));
        }
        view.setText(styled);
        Linkify.addLinks(view, Linkify.EMAIL_ADDRESSES | Linkify.WEB_URLS);
    }
}
