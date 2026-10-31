package com.simpleweb.affaldsguidedanmark;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.annotation.Keep;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.util.*;

/** Private on-device log. Stored factors keep historical estimates stable after catalogue updates. */
final class SortingLogStore {
    @Keep static final class Entry {
        String id, product, municipality, factorVersion;
        long date;
        int weightGrams;
        LinkedHashMap<String,Double> factors;
        double estimate() { return SortingImpact.estimate(weightGrams, factors.values()); }
    }
    private final java.util.function.BiConsumer<Integer,Double> progressChanged;
    private final SharedPreferences prefs;
    private final Gson gson = new Gson();
    private List<Entry> entries;
    SortingLogStore(Context context) {
        this(context.getApplicationContext().getSharedPreferences("sorting_impact_v1", Context.MODE_PRIVATE),
                (count, kg) -> AchievementStore.get(context.getApplicationContext()).recordSortingProgress(count, kg));
    }
    SortingLogStore(SharedPreferences prefs, java.util.function.BiConsumer<Integer,Double> progressChanged) {
        this.prefs = prefs;
        this.progressChanged = progressChanged;
        entries = gson.fromJson(prefs.getString("entries", "[]"), new TypeToken<List<Entry>>(){}.getType());
        if (entries == null) throw new IllegalStateException("Unreadable sorting log");
        for (Entry entry : entries) {
            if (entry == null || entry.id == null || entry.product == null || entry.weightGrams < 1
                    || entry.weightGrams > 100000 || entry.factors == null || entry.factors.isEmpty()) {
                throw new IllegalStateException("Invalid sorting log entry");
            }
            for (Double factor : entry.factors.values()) {
                if (factor == null || !Double.isFinite(factor) || factor < 0) throw new IllegalStateException("Invalid saved factor");
            }
        }
    }
    List<Entry> entries() { return new ArrayList<>(entries); }
    double total() { double sum = 0; for (Entry entry : entries) sum += entry.estimate(); return sum; }
    void add(String product, String municipality, int grams, LinkedHashMap<String,Double> factors) {
        if (grams < 1 || grams > 100000 || factors.isEmpty()) throw new IllegalArgumentException("Invalid sorting entry");
        for (Double factor : factors.values()) if (factor == null || !Double.isFinite(factor) || factor < 0) throw new IllegalArgumentException("Invalid factor");
        Entry entry = new Entry(); entry.id = UUID.randomUUID().toString(); entry.date = System.currentTimeMillis();
        entry.product = product; entry.municipality = municipality; entry.weightGrams = grams;
        entry.factors = new LinkedHashMap<>(factors); entry.factorVersion = SortingImpact.VERSION;
        List<Entry> next = entries(); next.add(0, entry); save(next);
    }
    void edit(String id, int grams) {
        if (grams < 1 || grams > 100000) throw new IllegalArgumentException("Invalid weight");
        List<Entry> next = gson.fromJson(gson.toJson(entries), new TypeToken<List<Entry>>(){}.getType());
        for (Entry entry : next) if (entry.id.equals(id)) entry.weightGrams = grams;
        save(next);
    }
    void delete(String id) { List<Entry> next = entries(); next.removeIf(entry -> entry.id.equals(id)); save(next); }
    private void save(List<Entry> next) {
        if (!prefs.edit().putString("entries", gson.toJson(next)).commit()) throw new IllegalStateException("Cannot save sorting log");
        entries = next;
        progressChanged.accept(entries.size(), total());
    }
}
