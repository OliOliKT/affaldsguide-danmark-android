package com.simpleweb.affaldsguidedanmark;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/** The same public calendar endpoint and payloads used by the iOS app. Call off the main thread. */
final class CalendarApi {
    private static final String ENDPOINT = "https://mit-affald.dk/api/calendar";

    static final class Address {
        final String municipality, id, label;
        Address(String municipality, String id, String label) {
            this.municipality = municipality; this.id = id; this.label = label;
        }
    }

    static final class Pickup {
        final String date, kind, label;
        Pickup(String date, String kind, String label) {
            this.date = date; this.kind = kind; this.label = label;
        }
    }

    static final class HttpError extends Exception {
        final int status;
        HttpError(int status) { super("Calendar HTTP " + status); this.status = status; }
    }

    static List<String> municipalities() throws Exception {
        JSONArray values = request(null).getJSONArray("municipalities");
        List<String> result = new ArrayList<>();
        for (int i = 0; i < values.length(); i++) {
            String name = values.getString(i);
            if (!name.equalsIgnoreCase("Odense")) result.add(name);
        }
        return result;
    }

    static List<Address> search(String municipality, String postcode, String street, String houseNumber) throws Exception {
        JSONObject body = new JSONObject().put("action", "search").put("municipality", municipality)
                .put("postcode", postcode).put("street", street).put("house_number", houseNumber);
        JSONArray values = request(body).getJSONArray("addresses");
        List<Address> result = new ArrayList<>();
        for (int i = 0; i < values.length(); i++) {
            JSONObject value = values.getJSONObject(i);
            result.add(new Address(municipality, value.getString("id"), value.getString("label")));
        }
        return result;
    }

    static List<Pickup> pickups(Address address) throws Exception {
        JSONObject body = new JSONObject().put("action", "pickups")
                .put("municipality", address.municipality).put("address_id", address.id);
        JSONArray values = request(body).getJSONArray("events");
        List<Pickup> result = new ArrayList<>();
        for (int i = 0; i < values.length(); i++) {
            JSONObject value = values.getJSONObject(i);
            result.add(new Pickup(value.getString("date"), value.optString("kind"), value.getString("original_label")));
        }
        return result;
    }

    private static JSONObject request(JSONObject body) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(ENDPOINT).openConnection();
        connection.setConnectTimeout(15000);
        connection.setReadTimeout(40000);
        connection.setRequestProperty("Accept", "application/json");
        try {
            if (body != null) {
                connection.setRequestMethod("POST");
                connection.setDoOutput(true);
                connection.setRequestProperty("Content-Type", "application/json");
                try (OutputStream output = connection.getOutputStream()) {
                    output.write(body.toString().getBytes(StandardCharsets.UTF_8));
                }
            }
            int status = connection.getResponseCode();
            if (status < 200 || status >= 300) throw new HttpError(status);
            try (InputStream stream = connection.getInputStream();
                 BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                StringBuilder json = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) json.append(line);
                return new JSONObject(json.toString());
            }
        } finally {
            connection.disconnect();
        }
    }
}
