package com.simpleweb.affaldsguidedanmark;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.Locale;

/** Matches the iOS result URL and the website's canonical Danish item slugs. */
final class SortingShareLink {
    private SortingShareLink() {}

    static String build(String canonicalProduct, String language, String municipality) {
        String slug = Normalizer.normalize(canonicalProduct, Normalizer.Form.NFC).trim()
                .toLowerCase(Locale.ROOT).replace("å", "aa").replace("ø", "oe").replace("æ", "ae")
                .replaceAll("[^a-z0-9]+", "-").replaceAll("^-+|-+$", "");
        if (slug.isEmpty()) throw new IllegalArgumentException("Missing item slug");
        String prefix = "en".equals(language) || "ar".equals(language) ? "/" + language : "";
        // Keep an explicit empty municipality so the recipient sees general guidance,
        // even if their browser has saved a different municipality.
        String encoded;
        try {
            encoded = URLEncoder.encode(municipality == null ? "" : municipality, StandardCharsets.UTF_8.name())
                    .replace("+", "%20");
        } catch (java.io.UnsupportedEncodingException impossible) {
            throw new AssertionError(impossible);
        }
        return "https://mit-affald.dk" + prefix + "/skrald/" + slug + "?municipality=" + encoded + "&share=2";
    }
}
