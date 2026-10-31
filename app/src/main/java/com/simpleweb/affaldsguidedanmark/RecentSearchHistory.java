package com.simpleweb.affaldsguidedanmark;

import java.text.Collator;
import java.text.Normalizer;
import java.util.*;

/** Search/filter operations preserve the stored newest-first order unless explicitly sorted. */
final class RecentSearchHistory {
    static final class Row {
        final String canonical, name;
        final List<String> searchNames;
        final Set<String> fractions;
        Row(String canonical, String name, List<String> searchNames, Collection<String> fractions) {
            this.canonical = canonical;
            this.name = name;
            this.searchNames = new ArrayList<>(searchNames);
            this.fractions = new LinkedHashSet<>(fractions);
        }
    }

    static List<Row> visible(List<Row> history, String query, String fraction, int sort, Locale locale) {
        String needle = normalized(query.trim());
        List<Row> result = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (Row row : history) {
            if (!seen.add(row.canonical)) continue;
            if (!fraction.isEmpty() && !row.fractions.contains(fraction)) continue;
            boolean matches = needle.isEmpty() || normalized(row.name).contains(needle);
            for (String name : row.searchNames) matches |= normalized(name).contains(needle);
            if (matches) result.add(row);
        }
        if (sort == 1) Collections.reverse(result);
        else if (sort == 2) {
            Collator collator = Collator.getInstance(locale);
            collator.setStrength(Collator.PRIMARY);
            result.sort((a, b) -> naturalCompare(a.name, b.name, collator));
        }
        return result;
    }

    private static String normalized(String text) {
        return Normalizer.normalize(text, Normalizer.Form.NFD).replaceAll("\\p{M}+", "").toLowerCase(Locale.ROOT);
    }

    private static int naturalCompare(String a, String b, Collator collator) {
        String[] left = a.split("(?<=\\D)(?=\\d)|(?<=\\d)(?=\\D)");
        String[] right = b.split("(?<=\\D)(?=\\d)|(?<=\\d)(?=\\D)");
        for (int i = 0; i < Math.min(left.length, right.length); i++) {
            int order;
            if (left[i].matches("[0-9]+") && right[i].matches("[0-9]+")) {
                order = new java.math.BigInteger(left[i]).compareTo(new java.math.BigInteger(right[i]));
            } else order = collator.compare(left[i], right[i]);
            if (order != 0) return order;
        }
        return Integer.compare(left.length, right.length);
    }
}
