package com.simpleweb.affaldsguidedanmark;

import android.content.res.Resources;
import android.util.Log;

import androidx.annotation.Keep;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

final class MunicipalitySortingRuleDB {
    private static final String TAG = "MunicipalityRules";
    private final Map<String, Rule> rulesByMunicipalityAndProduct = new HashMap<>();

    MunicipalitySortingRuleDB(Resources resources) {
        try (InputStream stream = resources.openRawResource(R.raw.kommunesortering_data);
             InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            Type type = new TypeToken<List<Rule>>() {}.getType();
            List<Rule> rules = new Gson().fromJson(reader, type);
            if (rules == null) return;
            for (Rule rule : rules) {
                if (rule == null || rule.municipalityName == null) continue;
                add(rule, rule.product);
                add(rule, rule.productEnglish);
            }
        } catch (Exception error) {
            Log.e(TAG, "Could not load local sorting rules", error);
        }
    }

    Rule ruleFor(String municipalityName, TrashDB.TrashItem item) {
        if (item == null) return null;
        Rule rule = ruleFor(municipalityName, item.product);
        return rule != null ? rule : ruleFor(municipalityName, item.productEn);
    }

    Rule ruleFor(String municipalityName, String productName) {
        if (municipalityName == null || municipalityName.trim().isEmpty()
                || productName == null || productName.trim().isEmpty()) return null;
        return rulesByMunicipalityAndProduct.get(key(municipalityName, productName));
    }

    private void add(Rule rule, String productName) {
        if (productName != null && !productName.trim().isEmpty()) {
            rulesByMunicipalityAndProduct.putIfAbsent(key(rule.municipalityName, productName), rule);
        }
    }

    private static String key(String municipalityName, String productName) {
        return normalize(municipalityName) + "|" + normalize(productName);
    }

    private static String normalize(String value) {
        String normalized = value.toLowerCase(Locale.ROOT)
                .replace("æ", "ae").replace("ø", "oe").replace("å", "aa");
        StringBuilder result = new StringBuilder(normalized.length());
        for (int i = 0; i < normalized.length(); i++) {
            char character = normalized.charAt(i);
            if (Character.isLetterOrDigit(character)) result.append(character);
        }
        return result.toString();
    }

    @Keep
    static final class Rule {
        @SerializedName("Kommune") String municipalityName;
        @SerializedName("Produkt") String product;
        @SerializedName("Produkt_en") String productEnglish;
        @SerializedName("Sortering") Map<String, String> sorting;
        @SerializedName("Sortering_en") Map<String, String> sortingEnglish;
        @SerializedName("Note") String note;
        @SerializedName("Note_en") String noteEnglish;
        @SerializedName("KildeUrl") String sourceURL;
        @SerializedName("SidstTjekket") String lastChecked;

        Map<String, String> sortingFor(boolean useEnglish) {
            Map<String, String> selected = useEnglish && sortingEnglish != null ? sortingEnglish : sorting;
            return selected == null ? Collections.emptyMap() : selected;
        }

        String noteFor(boolean useEnglish) {
            return useEnglish && noteEnglish != null && !noteEnglish.trim().isEmpty() ? noteEnglish : note;
        }
    }
}
