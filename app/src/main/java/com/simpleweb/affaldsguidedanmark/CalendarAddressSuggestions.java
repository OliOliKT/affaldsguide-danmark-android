package com.simpleweb.affaldsguidedanmark;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Converts provider address labels into field-specific, deduplicated suggestions. */
final class CalendarAddressSuggestions {
    static final class Option {
        final String label, value;
        Option(String label, String value) { this.label = label; this.value = value; }
    }
    private static final Pattern ADDRESS = Pattern.compile("^(.+?)\\s+(\\d+\\s*[A-Za-z]?(?:\\s*[-/]\\s*\\d+\\s*[A-Za-z]?)?)(?:\\s+.*)?$");

    static List<Option> options(List<String> labels, String postcode, boolean streets) {
        Map<String, Option> unique = new LinkedHashMap<>();
        for (String label : labels) {
            String first = label.split(",", 2)[0].replaceFirst("\\s+" + Pattern.quote(postcode) + "\\b.*$", "").trim();
            Matcher matcher = ADDRESS.matcher(first);
            if (!matcher.matches()) continue;
            String value = matcher.group(streets ? 1 : 2).trim();
            unique.putIfAbsent(value.toLowerCase(Locale.ROOT), new Option(streets ? value : label, value));
        }
        return new ArrayList<>(unique.values());
    }
}
