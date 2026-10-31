package com.simpleweb.affaldsguidedanmark;

import java.time.Instant;
import java.time.ZoneId;

/** Timing uses calendar days, including daylight-saving transitions, like iOS. */
final class AppReviewPolicy {
    private AppReviewPolicy() {}
    static boolean eligible(long now, long firstUse, int activeDays, boolean completedTask,
                            long lastAttempt, String lastVersion, String version, ZoneId zone) {
        if (!completedTask || firstUse <= 0 || activeDays < 3 || version.equals(lastVersion)) return false;
        if (Instant.ofEpochMilli(now).isBefore(Instant.ofEpochMilli(firstUse).atZone(zone).plusDays(7).toInstant())) return false;
        return lastAttempt <= 0 || !Instant.ofEpochMilli(now).isBefore(
                Instant.ofEpochMilli(lastAttempt).atZone(zone).plusDays(120).toInstant());
    }
    static int activeDays(int previous, long previousDay, long today) {
        return previousDay == today ? previous : Math.min(3, previous + 1);
    }
}
