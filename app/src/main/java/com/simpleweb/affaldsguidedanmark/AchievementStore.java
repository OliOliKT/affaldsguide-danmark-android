package com.simpleweb.affaldsguidedanmark;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONObject;

import java.time.LocalDate;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Local, per-device progress for the same achievements as the iOS app. */
final class AchievementStore {
    static final class Achievement {
        final String id;
        final int[] thresholds;

        Achievement(String id, int... thresholds) {
            this.id = id;
            this.thresholds = thresholds;
        }

        int levelFor(int count) {
            int level = 0;
            for (int threshold : thresholds) if (count >= threshold) level++;
            return level;
        }
    }

    static final class Unlock {
        final Achievement achievement;
        final int level;
        Unlock(Achievement achievement, int level) {
            this.achievement = achievement;
            this.level = level;
        }
    }

    static final List<Achievement> ALL = Collections.unmodifiableList(Arrays.asList(
            new Achievement("firstAnswer", 3, 50, 100),
            new Achievement("tenPhotoSearches", 3, 30, 80),
            new Achievement("fiveCategories", 5, 8, 12),
            new Achievement("localRulesExpert", 25),
            new Achievement("recyclingCenter", 1, 5, 10),
            new Achievement("calendarAddress", 1),
            new Achievement("helpfulEye", 1, 5, 10),
            new Achievement("tenDayStreak", 7, 30, 100),
            new Achievement("tenLoggedItems", 3, 50, 100),
            new Achievement("tenKgClimateSavings", 10000)
    ));

    static List<Achievement> visibleAchievements() {
        List<Achievement> visible = new ArrayList<>();
        visible.addAll(ALL);
        return visible;
    }

    private static final String PREFS = "achievement_progress_v1";
    private static AchievementStore instance;
    private final SharedPreferences preferences;
    private final Deque<Unlock> pendingUnlocks = new ArrayDeque<>();
    private Runnable unlockListener;

    private AchievementStore(Context context) {
        preferences = context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        if (!SavedMunicipalityManager.getSavedMunicipalityName(context).isEmpty()
                && !preferences.getBoolean("municipalitySelected", false)) {
            preferences.edit().putBoolean("municipalitySelected", true).apply();
        }
        try {
            String savedAddress = context.getApplicationContext()
                    .getSharedPreferences("collection_calendar", Context.MODE_PRIVATE)
                    .getString("address", "");
            JSONObject address = new JSONObject(savedAddress);
            if (!address.optString("id").isEmpty() && !address.optString("municipality").isEmpty()) {
                preferences.edit().putBoolean("calendarAddressSaved", true).apply();
            }
        } catch (Exception ignored) {
            // No valid saved calendar address yet.
        }
        migrateProgress();
        checkUnlocks(false);
    }

    static synchronized AchievementStore get(Context context) {
        if (instance == null) instance = new AchievementStore(context);
        return instance;
    }

    void setUnlockListener(Runnable listener) {
        unlockListener = listener;
        if (listener != null && !pendingUnlocks.isEmpty()) listener.run();
    }

    Unlock takePendingUnlock() {
        return pendingUnlocks.pollFirst();
    }

    int count(Achievement achievement) {
        switch (achievement.id) {
            case "firstAnswer": return set("items").size();
            case "fiveCategories":
                Set<String> categories = set("categories");
                categories.addAll(set("fractions"));
                return categories.size();
            case "tenPhotoSearches": return preferences.getInt("photoSearches", 0);
            case "localRulesExpert": return set("localRuleItems").size();
            case "recyclingCenter": return Math.max(set("mappedRecyclingCenters").size(),
                    preferences.getBoolean("recyclingCenterMapOpened", false) ? 1 : 0);
            case "calendarAddress": return preferences.getBoolean("calendarAddressSaved", false) ? 1 : 0;
            case "helpfulEye": return preferences.getInt("correctionCount", 0);
            case "tenDayStreak": return Math.max(preferences.getInt("bestStreak", 0), preferences.getInt("currentStreak", 0));
            case "tenLoggedItems": return preferences.getInt("loggedItemCount", 0);
            case "tenKgClimateSavings": return preferences.getInt("estimatedSavingsGrams", 0);
            default: return 0;
        }
    }

    int earnedLevel(Achievement achievement) {
        return preferences.getInt("level." + achievement.id, 0);
    }

    int nextTarget(Achievement achievement) {
        return achievement.thresholds[Math.min(earnedLevel(achievement), achievement.thresholds.length - 1)];
    }

    boolean isUnlocked(Achievement achievement) { return earnedLevel(achievement) > 0; }

    static String detail(Context context, Achievement achievement, int target) {
        String key = "achievements." + achievement.id + ".detail";
        String text = target == 1 ? InfoPageText.get(context, key + ".one") : "";
        if (text.isEmpty()) text = InfoPageText.get(context, key);
        return text.replace("{0}", String.valueOf(target));
    }

    // Called by the sorting log when that feature is available on Android.
    void recordSortingProgress(int itemCount, double savingsKg) {
        int grams = Double.isFinite(savingsKg) ? (int) Math.min(10000, Math.max(0, savingsKg * 1000 + 0.000001)) : 0;
        preferences.edit().putInt("loggedItemCount", Math.max(0, itemCount))
                .putInt("estimatedSavingsGrams", grams).apply();
        checkUnlocks();
    }

    private void migrateProgress() {
        if (preferences.getBoolean("levelsMigrated", false)) return;
        Set<String> legacy = set("unlocked");
        SharedPreferences.Editor editor = preferences.edit();
        for (Achievement achievement : ALL) {
            int level = legacyLevel(achievement.id, legacy);
            editor.putInt("level." + achievement.id, Math.max(earnedLevel(achievement), level));
        }
        editor.putInt("bestStreak", Math.max(preferences.getInt("bestStreak", 0), preferences.getInt("currentStreak", 0)))
                .putBoolean("levelsMigrated", true).apply();
    }

    static int legacyLevel(String id, Set<String> legacy) {
        switch (id) {
            case "firstAnswer": return legacy.contains("hundredItems") || legacy.contains("twoHundredFiftyItems") ? 3 : legacy.contains("tenItems") ? 2 : 0;
            case "tenPhotoSearches": return legacy.contains("hundredCameraItems") ? 3 : legacy.contains(id) ? 1 : 0;
            case "fiveCategories": return legacy.contains("allFractions") ? 3 : legacy.contains(id) ? 1 : 0;
            case "recyclingCenter": return legacy.contains("neighborhoodRegular") ? 2 : legacy.contains(id) ? 1 : 0;
            case "helpfulEye": return legacy.contains("trueWasteExpert") ? 3 : legacy.contains(id) ? 1 : 0;
            case "tenDayStreak": return legacy.contains("thirtyDayStreak") ? 2 : legacy.contains(id) ? 1 : 0;
            case "tenLoggedItems": return legacy.contains("fiftyLoggedItems") ? 2 : legacy.contains(id) ? 1 : 0;
            default: return legacy.contains(id) ? 1 : 0;
        }
    }

    void recordItem(String danishName, String primaryCategory, boolean localRule) {
        if (danishName == null || danishName.isEmpty()) return;
        boolean changed = add("items", danishName);
        if (primaryCategory != null && !primaryCategory.isEmpty()) changed |= add("categories", primaryCategory);
        if (localRule) changed |= add("localRuleItems", danishName);
        if (changed) checkUnlocks();
    }

    void recordFraction(String name) {
        if (name != null && add("fractions", name)) checkUnlocks();
    }

    void recordMunicipalitySelected() { setFlag("municipalitySelected"); }
    void recordMunicipalityViewed() { setFlag("municipalityViewed"); }
    void recordCalendarAddressSaved() { setFlag("calendarAddressSaved"); }

    void recordRecyclingCenterMap(String centerId) {
        boolean changed = !preferences.getBoolean("recyclingCenterMapOpened", false);
        preferences.edit().putBoolean("recyclingCenterMapOpened", true).apply();
        if (centerId != null && !centerId.isEmpty()) changed |= add("mappedRecyclingCenters", centerId);
        if (changed) checkUnlocks();
    }

    void recordCorrection() {
        preferences.edit().putInt("correctionCount", preferences.getInt("correctionCount", 0) + 1).apply();
        checkUnlocks();
    }

    void recordPhotoSearch() {
        preferences.edit().putInt("photoSearches", preferences.getInt("photoSearches", 0) + 1).apply();
        checkUnlocks();
    }

    void recordAppUse() {
        long today = LocalDate.now().toEpochDay();
        long previous = preferences.getLong("lastActiveDay", Long.MIN_VALUE);
        if (previous == today) return;
        int streak = previous == today - 1 ? preferences.getInt("currentStreak", 0) + 1 : 1;
        preferences.edit().putLong("lastActiveDay", today).putInt("currentStreak", streak)
                .putInt("bestStreak", Math.max(streak, preferences.getInt("bestStreak", 0))).apply();
        checkUnlocks();
    }

    private void setFlag(String key) {
        if (preferences.getBoolean(key, false)) return;
        preferences.edit().putBoolean(key, true).apply();
        checkUnlocks();
    }

    private Set<String> set(String key) {
        return new HashSet<>(preferences.getStringSet(key, Collections.emptySet()));
    }

    private boolean add(String key, String value) {
        Set<String> values = set(key);
        if (!values.add(value)) return false;
        preferences.edit().putStringSet(key, values).apply();
        return true;
    }

    private void checkUnlocks() { checkUnlocks(true); }

    private void checkUnlocks(boolean notify) {
        SharedPreferences.Editor editor = preferences.edit();
        boolean changed = false;
        for (Achievement achievement : ALL) {
            int level = achievement.levelFor(count(achievement));
            if (level <= earnedLevel(achievement)) continue;
            editor.putInt("level." + achievement.id, level);
            changed = true;
            if (notify) {
                pendingUnlocks.removeIf(unlock -> unlock.achievement.id.equals(achievement.id));
                pendingUnlocks.addLast(new Unlock(achievement, level));
            }
        }
        if (!changed) return;
        editor.apply();
        if (notify && unlockListener != null) unlockListener.run();
    }
}
