package com.simpleweb.affaldsguidedanmark;

import android.graphics.Bitmap;
import android.util.Base64;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/** Sends only a reduced JPEG to our server; the Gemini API key stays on the server. */
final class OnlinePhotoRecognition {
    private static final String ENDPOINT = "https://mit-affald.dk/api/photo-recognition";
    private static final int MAX_IMAGE_BYTES = 2_000_000;

    static List<String> recognize(Bitmap source) throws Exception {
        if (source == null || source.getWidth() < 1 || source.getHeight() < 1) throw new IllegalArgumentException("Empty image");
        float scale = Math.min(1f, 1024f / Math.max(source.getWidth(), source.getHeight()));
        Bitmap image = scale < 1f ? Bitmap.createScaledBitmap(source,
                Math.max(1, Math.round(source.getWidth() * scale)),
                Math.max(1, Math.round(source.getHeight() * scale)), true) : source;
        byte[] jpeg;
        try {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            if (!image.compress(Bitmap.CompressFormat.JPEG, 75, buffer)) throw new IllegalArgumentException("JPEG encoding failed");
            jpeg = buffer.toByteArray();
        } finally {
            if (image != source) image.recycle();
        }
        if (jpeg.length > MAX_IMAGE_BYTES) throw new IllegalArgumentException("Image too large");
        JSONObject payload = new JSONObject().put("image", Base64.encodeToString(jpeg, Base64.NO_WRAP));
        HttpURLConnection connection = (HttpURLConnection) new URL(ENDPOINT).openConnection();
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        connection.setConnectTimeout(10000);
        connection.setReadTimeout(25000);
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("Accept", "application/json");
        try {
            try (OutputStream output = connection.getOutputStream()) {
                output.write(payload.toString().getBytes(StandardCharsets.UTF_8));
            }
            if (connection.getResponseCode() != 200)
                throw new IllegalStateException("Photo service HTTP " + connection.getResponseCode());
            StringBuilder response = new StringBuilder();
            try (InputStream input = connection.getInputStream();
                 BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) response.append(line);
            }
            JSONArray objects = new JSONObject(response.toString()).getJSONArray("objects");
            List<String> evidence = new ArrayList<>();
            for (int i = 0; i < objects.length(); i++) {
                JSONObject object = objects.getJSONObject(i);
                add(evidence, object.optString("danish"));
                add(evidence, object.optString("english"));
                add(evidence, object.optString("visible_text"));
            }
            return evidence;
        } finally {
            connection.disconnect();
        }
    }

    private static void add(List<String> values, String value) {
        if (value != null && !value.trim().isEmpty()) values.add(value.trim());
    }
}
