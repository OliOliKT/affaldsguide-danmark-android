package com.simpleweb.affaldsguidedanmark;

import java.text.Normalizer;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Status supported by the municipality's recorded fraction count, descriptions, and confirmations. */
public final class MunicipalityFractionStatus {
    public enum Status { USED, NOT_USED, UNCONFIRMED }

    public static final String[] CATEGORIES = {
            "Restaffald", "Madaffald", "Pap", "Plast", "Glas", "Farligt affald",
            "Papir", "Metal", "Tekstilaffald", "Mad- og drikkekartoner"
    };

    private MunicipalityFractionStatus() {}

    public static Status forCategory(Municipality municipality, String category) {
        if (municipality == null || category == null) return Status.UNCONFIRMED;
        Municipality.Details details = municipality.getDetails();
        if (details == null) return Status.UNCONFIRMED;
        if (isConfirmed(details, category)) return Status.USED;

        Integer count = details.getWasteFractionCount();
        if (count == null) return Status.UNCONFIRMED;
        if (count >= CATEGORIES.length) return Status.USED;

        Set<String> detected = detectCategories(municipality);
        if (detected.contains(category)) return Status.USED;
        return detected.size() == count ? Status.NOT_USED : Status.UNCONFIRMED;
    }

    public static String confirmedSource(Municipality municipality, String category) {
        if (municipality == null || municipality.getDetails() == null) return null;
        List<Municipality.ConfirmedFraction> confirmations = municipality.getDetails().getConfirmedFractions();
        if (confirmations == null) return null;
        for (Municipality.ConfirmedFraction confirmation : confirmations) {
            if (category.equals(confirmation.getCategory())) return confirmation.getSourceUrl();
        }
        return null;
    }

    private static boolean isConfirmed(Municipality.Details details, String category) {
        List<Municipality.ConfirmedFraction> confirmations = details.getConfirmedFractions();
        if (confirmations == null) return false;
        for (Municipality.ConfirmedFraction confirmation : confirmations) {
            if (category.equals(confirmation.getCategory())) return true;
        }
        return false;
    }

    private static Set<String> detectCategories(Municipality municipality) {
        StringBuilder source = new StringBuilder();
        append(source, municipality.getWasteRules());
        append(source, municipality.getWasteRules(true));
        Municipality.Details details = municipality.getDetails();
        if (details.getSchemes() != null) {
            for (Municipality.Scheme scheme : details.getSchemes()) {
                append(source, scheme.getTitle(false));
                append(source, scheme.getDescription(false));
                append(source, scheme.getTitle(true));
                append(source, scheme.getDescription(true));
            }
        }
        String normalized = Normalizer.normalize(source.toString().toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "").replace('ø', 'o').replace("æ", "ae");
        String[] words = normalized.split("[^\\p{L}]+");
        Set<String> detected = new HashSet<>();
        for (String word : words) {
            if (word.startsWith("restaffald") || word.startsWith("dagrenovation")) detected.add(CATEGORIES[0]);
            if (word.startsWith("madaffald") || word.startsWith("bioaffald")) detected.add(CATEGORIES[1]);
            if (word.equals("pap")) detected.add(CATEGORIES[2]);
            if (word.startsWith("plast")) detected.add(CATEGORIES[3]);
            if (word.startsWith("glas")) detected.add(CATEGORIES[4]);
            if (word.startsWith("miljoboks") || word.startsWith("miljokasse")) detected.add(CATEGORIES[5]);
            if (word.startsWith("papir")) detected.add(CATEGORIES[6]);
            if (word.startsWith("metal")) detected.add(CATEGORIES[7]);
            if (word.startsWith("tekstil")) detected.add(CATEGORIES[8]);
            if (word.startsWith("karton")) detected.add(CATEGORIES[9]);
        }
        if (normalized.contains("farligt affald")) detected.add(CATEGORIES[5]);
        return detected;
    }

    private static void append(StringBuilder builder, String text) {
        if (text != null) builder.append(text).append(' ');
    }
}
