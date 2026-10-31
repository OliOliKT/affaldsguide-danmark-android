package com.simpleweb.affaldsguidedanmark;

import org.junit.Test;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Collections;
import static org.junit.Assert.*;

public class AchievementLevelsTest {
    @Test public void levelsMatchIosAndRespectBoundaries() {
        assertEquals(10, AchievementStore.ALL.size());
        for (AchievementStore.Achievement achievement : AchievementStore.ALL) {
            assertEquals(0, achievement.levelFor(0));
            for (int i = 0; i < achievement.thresholds.length; i++) {
                assertEquals(i, achievement.levelFor(achievement.thresholds[i] - 1));
                assertEquals(i + 1, achievement.levelFor(achievement.thresholds[i]));
            }
            assertEquals(achievement.thresholds.length, achievement.levelFor(Integer.MAX_VALUE));
        }
        assertArrayEquals(new int[]{3,30,80}, AchievementStore.ALL.get(1).thresholds);
        assertArrayEquals(new int[]{3,50,100}, AchievementStore.ALL.get(8).thresholds);
    }
    @Test public void legacyMilestonesPreserveEarnedStars() {
        assertEquals(3, AchievementStore.legacyLevel("firstAnswer", new HashSet<>(Arrays.asList("tenItems", "hundredItems"))));
        assertEquals(2, AchievementStore.legacyLevel("recyclingCenter", Collections.singleton("neighborhoodRegular")));
        assertEquals(3, AchievementStore.legacyLevel("helpfulEye", Collections.singleton("trueWasteExpert")));
        assertEquals(2, AchievementStore.legacyLevel("tenDayStreak", Collections.singleton("thirtyDayStreak")));
        assertEquals(0, AchievementStore.legacyLevel("firstAnswer", Collections.singleton("firstAnswer")));
        assertEquals(1, AchievementStore.legacyLevel("calendarAddress", Collections.singleton("calendarAddress")));
    }
}
