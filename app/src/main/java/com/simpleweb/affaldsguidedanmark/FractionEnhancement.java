package com.simpleweb.affaldsguidedanmark;

import android.content.res.Resources;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class FractionEnhancement {
    private static final String TAG = "FractionEnhancement";

    @SerializedName("summaryTitle")
    String summaryTitle;

    @SerializedName("summary")
    String summary;

    @SerializedName("mistakesTitle")
    String mistakesTitle;

    @SerializedName("mistakes")
    List<String> mistakes;

    @SerializedName("cleanDirtyTitle")
    String cleanDirtyTitle;

    @SerializedName("cleanRuleTitle")
    String cleanRuleTitle;

    @SerializedName("cleanRule")
    String cleanRule;

    @SerializedName("dirtyRuleTitle")
    String dirtyRuleTitle;

    @SerializedName("dirtyRule")
    String dirtyRule;

    @SerializedName("municipalityTitle")
    String municipalityTitle;

    @SerializedName("municipalityNote")
    String municipalityNote;

    @SerializedName("municipalityLinkText")
    String municipalityLinkText;

    @SerializedName("faqTitle")
    String faqTitle;

    @SerializedName("faqs")
    List<Faq> faqs;

    static class Faq {
        @SerializedName("question")
        String question;

        @SerializedName("answer")
        String answer;
    }

    static FractionEnhancement get(Resources resources, String fractionName, boolean useEnglish) {
        if (fractionName == null) {
            return null;
        }

        try (InputStream inputStream = resources.openRawResource(R.raw.fraction_enhancements);
             InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
            Gson gson = new Gson();
            Type type = new TypeToken<Map<String, Map<String, FractionEnhancement>>>() {}.getType();
            Map<String, Map<String, FractionEnhancement>> allEnhancements = gson.fromJson(reader, type);
            Map<String, FractionEnhancement> localeEnhancements = allEnhancements.get(useEnglish ? "en" : "da");

            if (localeEnhancements == null) {
                return null;
            }

            return localeEnhancements.get(fractionName);
        } catch (Exception e) {
            Log.e(TAG, "Error loading fraction enhancement JSON file", e);
            return null;
        }
    }

    List<String> getMistakes() {
        return mistakes != null ? mistakes : Collections.emptyList();
    }

    List<Faq> getFaqs() {
        return faqs != null ? faqs : Collections.emptyList();
    }
}
