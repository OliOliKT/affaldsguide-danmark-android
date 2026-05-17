package com.simpleweb.affaldsguidedanmark;

import android.content.Context;
import android.graphics.Typeface;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import org.apache.commons.text.similarity.LevenshteinDistance;

public class SuggestionAdapter extends ArrayAdapter<String> implements Filterable {
    public static final String FUZZY_SUGGESTION_PREFIX = "Mente du ";
    public static final String FUZZY_SUGGESTION_PREFIX_EN = "Did you mean ";

    private static final int MAX_SUGGESTIONS = 5;
    private static final int MIN_FUZZY_QUERY_LENGTH = 3;

    private final List<String> suggestions;
    private final LevenshteinDistance levenshteinDistance = new LevenshteinDistance();
    private String currentQuery = "";

    public SuggestionAdapter(Context context, List<String> suggestions) {
        super(context, R.layout.trash_suggestion, suggestions);
        this.suggestions = new ArrayList<>(suggestions);
    }

    @Override
    public Filter getFilter() {
        return new SuggestionFilter();
    }

    private class SuggestionFilter extends Filter {
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            FilterResults filterResults = new FilterResults();
            List<String> filteredSuggestions = new ArrayList<>();
            List<String> partialSuggestions = new ArrayList<>();

            if (constraint != null) {
                String searchString = normalizeQuery(constraint);
                for (String suggestion : suggestions) {
                    String normalizedSuggestion = normalizeText(suggestion);
                    if (normalizedSuggestion.startsWith(searchString)) {
                        filteredSuggestions.add(suggestion);
                    } else if (normalizedSuggestion.contains(searchString)) {
                        partialSuggestions.add(suggestion);
                    }
                }
            }

            Collections.sort(filteredSuggestions, String.CASE_INSENSITIVE_ORDER);
            Collections.sort(partialSuggestions, String.CASE_INSENSITIVE_ORDER);
            filteredSuggestions.addAll(partialSuggestions);

            if (filteredSuggestions.isEmpty() && constraint != null) {
                filteredSuggestions.addAll(findSimilarSuggestions(normalizeQuery(constraint)));
            }

            if (filteredSuggestions.size() > MAX_SUGGESTIONS) {
                filteredSuggestions = new ArrayList<>(filteredSuggestions.subList(0, MAX_SUGGESTIONS));
            }

            filterResults.values = filteredSuggestions;
            filterResults.count = filteredSuggestions.size();
            return filterResults;
        }

        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            currentQuery = normalizeQuery(constraint);
            clear();
            addAll((List<String>) results.values);
            notifyDataSetChanged();
        }
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder;

        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.trash_suggestion, parent, false);
            viewHolder = new ViewHolder();
            viewHolder.suggestionTextView = convertView.findViewById(R.id.suggestion_text);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        String suggestion = getItem(position);
        viewHolder.suggestionTextView.setText(getHighlightedSuggestion(suggestion));

        return convertView;
    }

    public static String getSearchValue(String suggestion) {
        if (suggestion == null) {
            return null;
        }

        if (suggestion.startsWith(FUZZY_SUGGESTION_PREFIX)) {
            return suggestion.substring(FUZZY_SUGGESTION_PREFIX.length()).trim();
        }

        if (suggestion.startsWith(FUZZY_SUGGESTION_PREFIX.trim())) {
            return suggestion.substring(FUZZY_SUGGESTION_PREFIX.trim().length()).trim();
        }

        if (suggestion.startsWith(FUZZY_SUGGESTION_PREFIX_EN)) {
            return suggestion.substring(FUZZY_SUGGESTION_PREFIX_EN.length()).trim();
        }

        if (suggestion.startsWith(FUZZY_SUGGESTION_PREFIX_EN.trim())) {
            return suggestion.substring(FUZZY_SUGGESTION_PREFIX_EN.trim().length()).trim();
        }

        return suggestion;
    }

    private String normalizeQuery(CharSequence constraint) {
        if (constraint == null) {
            return "";
        }

        String query = constraint.toString();
        int lastCommaIndex = query.lastIndexOf(',');
        if (lastCommaIndex >= 0) {
            query = query.substring(lastCommaIndex + 1);
        }

        return normalizeText(query.trim());
    }

    private String normalizeText(String value) {
        return value.toLowerCase(new Locale("da", "DK"));
    }

    private List<String> findSimilarSuggestions(String searchString) {
        List<ScoredSuggestion> scoredSuggestions = new ArrayList<>();
        if (searchString.length() < MIN_FUZZY_QUERY_LENGTH) {
            return new ArrayList<>();
        }

        for (String suggestion : suggestions) {
            String normalizedSuggestion = normalizeText(suggestion);
            int distance = getBestDistance(searchString, normalizedSuggestion);
            int allowedDistance = Math.max(1, Math.min(4, searchString.length() / 3));

            if (distance <= allowedDistance) {
                scoredSuggestions.add(new ScoredSuggestion(suggestion, distance));
            }
        }

        Collections.sort(scoredSuggestions, (first, second) -> {
            int distanceComparison = Integer.compare(first.distance, second.distance);
            if (distanceComparison != 0) {
                return distanceComparison;
            }
            return String.CASE_INSENSITIVE_ORDER.compare(first.suggestion, second.suggestion);
        });

        List<String> similarSuggestions = new ArrayList<>();
        for (ScoredSuggestion scoredSuggestion : scoredSuggestions) {
            similarSuggestions.add(getFuzzyPrefix() + scoredSuggestion.suggestion);
            if (similarSuggestions.size() == MAX_SUGGESTIONS) {
                break;
            }
        }

        return similarSuggestions;
    }

    private int getBestDistance(String searchString, String suggestion) {
        int fullDistance = levenshteinDistance.apply(searchString, suggestion);
        int prefixLength = Math.min(suggestion.length(), searchString.length() + 2);
        String suggestionPrefix = suggestion.substring(0, prefixLength);
        int prefixDistance = levenshteinDistance.apply(searchString, suggestionPrefix);
        return Math.min(fullDistance, prefixDistance);
    }

    private SpannableString getHighlightedSuggestion(String suggestion) {
        SpannableString highlightedSuggestion = new SpannableString(suggestion);
        String fuzzyPrefix = getFuzzyPrefix();
        if (suggestion != null && suggestion.startsWith(fuzzyPrefix)) {
            int startIndex = fuzzyPrefix.length();
            int endIndex = suggestion.length();
            highlightedSuggestion.setSpan(
                    new ForegroundColorSpan(ContextCompat.getColor(getContext(), R.color.green_light)),
                    startIndex,
                    endIndex,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            );
            highlightedSuggestion.setSpan(
                    new StyleSpan(Typeface.BOLD),
                    startIndex,
                    endIndex,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            );
            return highlightedSuggestion;
        }

        if (currentQuery.isEmpty()) {
            return highlightedSuggestion;
        }

        int startIndex = normalizeText(suggestion).indexOf(currentQuery);
        if (startIndex < 0) {
            return highlightedSuggestion;
        }

        int endIndex = startIndex + currentQuery.length();
        highlightedSuggestion.setSpan(
                new ForegroundColorSpan(ContextCompat.getColor(getContext(), R.color.green_light)),
                startIndex,
                endIndex,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        );
        highlightedSuggestion.setSpan(
                new StyleSpan(Typeface.BOLD),
                startIndex,
                endIndex,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        );
        return highlightedSuggestion;
    }

    private String getFuzzyPrefix() {
        String prefix = getContext().getString(R.string.mente_du_prefix).trim();
        return prefix + " ";
    }

    private static class ViewHolder {
        TextView suggestionTextView;
    }

    private static class ScoredSuggestion {
        final String suggestion;
        final int distance;

        ScoredSuggestion(String suggestion, int distance) {
            this.suggestion = suggestion;
            this.distance = distance;
        }
    }
}
