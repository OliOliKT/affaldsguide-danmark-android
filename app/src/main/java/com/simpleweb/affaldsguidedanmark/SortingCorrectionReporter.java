package com.simpleweb.affaldsguidedanmark;

import org.json.JSONObject;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/** Sends a sorting correction using the same request format as the iOS app. */
final class SortingCorrectionReporter {
    private SortingCorrectionReporter() {}

    static boolean submit(String productName, String productNameDa, String currentSorting,
                          String suggestedCategory, String suggestedCategoryLabel,
                          String comment, String locale) {
        HttpURLConnection connection = null;
        try {
            JSONObject payload = new JSONObject();
            payload.put("productName", productName);
            payload.put("productNameDa", productNameDa);
            payload.put("currentSorting", currentSorting);
            payload.put("suggestedCategory", suggestedCategory);
            payload.put("suggestedCategoryLabel", suggestedCategoryLabel);
            payload.put("comment", comment);
            payload.put("website", "");
            payload.put("locale", locale);
            payload.put("pageUrl", "Affaldsguide Danmark Android");

            connection = (HttpURLConnection) new URL("https://mit-affald.dk/api/report-sorting-error").openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            connection.setConnectTimeout(20000);
            connection.setReadTimeout(20000);
            connection.setDoOutput(true);
            try (OutputStream stream = connection.getOutputStream()) {
                stream.write(payload.toString().getBytes(StandardCharsets.UTF_8));
            }
            int responseCode = connection.getResponseCode();
            return responseCode >= 200 && responseCode < 300;
        } catch (Exception error) {
            return false;
        } finally {
            if (connection != null) connection.disconnect();
        }
    }
}
