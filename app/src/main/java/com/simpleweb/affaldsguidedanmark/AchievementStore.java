package com.simpleweb.affaldsguidedanmark;

import android.content.Context;
import android.content.SharedPreferences;

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
        final int target;

        Achievement(String id, int target) {
            this.id = id;
            this.target = target;
        }
    }

    static final List<Achievement> ALL = Collections.unmodifiableList(Arrays.asList(
            new Achievement("firstAnswer", 1),
            new Achievement("tenItems", 50),
            new Achievement("hundredItems", 100),
            new Achievement("twoHundredFiftyItems", 500),
            new Achievement("tenPhotoSearches", 10),
            new Achievement("hundredCameraItems", 100),
            new Achievement("fiveCategories", 5),
            new Achievement("allFractions", 12),
            new Achievement("localExpert", 2),
            new Achievement("localRulesExpert", 25),
            new Achievement("recyclingCenter", 1),
            new Achievement("neighborhoodRegular", 5),
            new Achievement("helpfulEye", 1),
            new Achievement("trueWasteExpert", 10),
            new Achievement("tenDayStreak", 7),
            new Achievement("thirtyDayStreak", 30)
    ));

    static List<Achievement> visibleAchievements() {
        List<Achievement> visible = new ArrayList<>();
        visible.addAll(ALL);
        return visible;
    }

    private static final String PREFS = "achievement_progress_v1";
    private static AchievementStore instance;
    private final SharedPreferences preferences;
    private final Deque<String> pendingUnlocks = new ArrayDeque<>();
    private Runnable unlockListener;

    private AchievementStore(Context context) {
        preferences = context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        if (!SavedMunicipalityManager.getSavedMunicipalityName(context).isEmpty()
                && !preferences.getBoolean("municipalitySelected", false)) {
            preferences.edit().putBoolean("municipalitySelected", true).apply();
        }
        checkUnlocks();
    }

    static synchronized AchievementStore get(Context context) {
        if (instance == null) instance = new AchievementStore(context);
        return instance;
    }

    void setUnlockListener(Runnable listener) {
        unlockListener = listener;
        if (listener != null && !pendingUnlocks.isEmpty()) listener.run();
    }

    String takePendingUnlock() {
        return pendingUnlocks.pollFirst();
    }

    int count(Achievement achievement) {
        switch (achievement.id) {
            case "firstAnswer": return Math.min(set("items").size(), 1);
            case "tenItems": return Math.min(set("items").size(), 50);
            case "hundredItems": return Math.min(set("items").size(), 100);
            case "twoHundredFiftyItems": return Math.min(set("items").size(), 500);
            case "fiveCategories": return Math.min(set("categories").size(), 5);
            case "tenPhotoSearches": return Math.min(preferences.getInt("photoSearches", 0), 10);
            case "hundredCameraItems": return Math.min(preferences.getInt("photoSearches", 0), 100);
            case "allFractions": return Math.min(set("fractions").size(), 12);
            case "localRulesExpert": return Math.min(set("localRuleItems").size(), 25);
            case "localExpert": return (preferences.getBoolean("municipalitySelected", false) ? 1 : 0)
                    + (preferences.getBoolean("municipalityViewed", false) ? 1 : 0);
            case "recyclingCenter": return preferences.getBoolean("recyclingCenterMapOpened", false) ? 1 : 0;
            case "neighborhoodRegular": return Math.min(set("mappedRecyclingCenters").size(), 5);
            case "helpfulEye": return Math.min(preferences.getInt("correctionCount", 0), 1);
            case "trueWasteExpert": return Math.min(preferences.getInt("correctionCount", 0), 10);
            case "tenDayStreak": return Math.min(preferences.getInt("currentStreak", 0), 7);
            case "thirtyDayStreak": return Math.min(preferences.getInt("currentStreak", 0), 30);
            default: return 0;
        }
    }

    boolean isUnlocked(Achievement achievement) {
        return set("unlocked").contains(achievement.id);
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
        preferences.edit().putLong("lastActiveDay", today).putInt("currentStreak", streak).apply();
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

    private void checkUnlocks() {
        Set<String> unlocked = set("unlocked");
        List<String> newlyUnlocked = new ArrayList<>();
        for (Achievement achievement : ALL) {
            if (!unlocked.contains(achievement.id) && count(achievement) >= achievement.target) {
                unlocked.add(achievement.id);
                newlyUnlocked.add(achievement.id);
            }
        }
        if (newlyUnlocked.isEmpty()) return;
        preferences.edit().putStringSet("unlocked", unlocked).apply();
        pendingUnlocks.addAll(newlyUnlocked);
        if (unlockListener != null) unlockListener.run();
    }
}
