package com.simpleweb.affaldsguidedanmark;

import android.content.res.Resources;
import com.google.gson.Gson;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** The same catalogue, product exceptions and equal-weight allocation as iOS. */
final class SortingImpact {
    static final String VERSION = "researched-subtypes-v2-provisional-2026-10-21";
    @androidx.annotation.Keep static final class Catalogue {
        Map<String, Double> fractionFallbacks;
        Map<String, Map<String, Double>> fractionTypes;
    }
    final Catalogue catalogue;
    SortingImpact(Resources resources) {
        try (InputStreamReader reader = new InputStreamReader(resources.openRawResource(R.raw.type_impact_factors), StandardCharsets.UTF_8)) {
            catalogue = new Gson().fromJson(reader, Catalogue.class);
        } catch (Exception error) { throw new IllegalStateException("Cannot load impact factors", error); }
    }
    SortingImpact(Catalogue catalogue) { this.catalogue = catalogue; }
    boolean accepts(String fraction) { return catalogue.fractionFallbacks.containsKey(fraction) || fraction.equals("Genbrugsplads"); }
    List<String> choices(Collection<String> fractions) {
        LinkedHashSet<String> result = new LinkedHashSet<>();
        for (String fraction : fractions) if (catalogue.fractionFallbacks.containsKey(fraction)) result.add(fraction);
        if (result.isEmpty() && fractions.contains("Genbrugsplads")) result.add("Genbrugsplads");
        return new ArrayList<>(result);
    }
    double factor(String fraction, Map<String,String> type, String product) {
        if (!catalogue.fractionFallbacks.containsKey(fraction) || fraction.equals("Restaffald") || fraction.equals("Farligt affald")) return 0;
        String subtype = type == null ? null : type.get(fraction);
        if ("Metal".equals(fraction) && "staal".equals(subtype) && "Blikdåse".equals(product)) return .26;
        if ("Pap".equals(fraction) && "karton".equals(subtype) && Arrays.asList("Karton","Kartonemballage","Kartonæske","Gajol pakke","Pladeomslag","Samlekort","Pokémon kort","Tebrevsæske").contains(product)) return 2.63;
        if ("Småt elektronik".equals(fraction) && "computerudstyr".equals(subtype)) {
            if (Arrays.asList("Bærbar","Laptop","E-bogslæser").contains(product)) return 1.88;
            if (Arrays.asList("Computermus","Tastatur").contains(product)) return 2.69;
            if (Arrays.asList("Dokumentscanner","Printer","Kopimaskine").contains(product)) return 1.94;
        }
        Map<String,Double> types = catalogue.fractionTypes.get(fraction);
        return types != null && types.containsKey(subtype) ? types.get(subtype) : catalogue.fractionFallbacks.get(fraction);
    }
    static double estimate(int grams, Collection<Double> factors) {
        if (factors.isEmpty()) return 0;
        double sum = 0; for (double factor : factors) sum += factor;
        return grams / 1000.0 * sum / factors.size();
    }
    static Integer grams(String input, boolean kg) {
        try {
            double grams = Double.parseDouble(input.trim().replace(',', '.')) * (kg ? 1000 : 1);
            return Double.isFinite(grams) && grams >= 1 && grams <= 100000 ? (int)Math.round(grams) : null;
        } catch (NumberFormatException error) { return null; }
    }
    static String csvCell(String value) {
        String safe = value == null ? "" : value;
        String trimmed = safe.trim();
        if (!trimmed.isEmpty() && "=+-@".indexOf(trimmed.charAt(0)) >= 0) safe = "'" + safe;
        return "\"" + safe.replace("\"", "\"\"") + "\"";
    }
}
