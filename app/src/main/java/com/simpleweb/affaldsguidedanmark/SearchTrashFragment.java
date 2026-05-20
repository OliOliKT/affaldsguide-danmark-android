package com.simpleweb.affaldsguidedanmark;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.MultiAutoCompleteTextView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.apache.commons.text.similarity.JaccardSimilarity;
import org.apache.commons.text.similarity.JaroWinklerSimilarity;
import org.apache.commons.text.similarity.LevenshteinDistance;

public class SearchTrashFragment extends Fragment {

    private static final String TAG = "SearchTrashFragment";
    private static final String RECENT_SEARCHES_PREFS = "RecentSearches";
    private static final String RECENT_SEARCHES_KEY = "queries";
    private static final int MAX_RECENT_SEARCHES = 10;
    private static final int MAX_EMPTY_STATE_SUGGESTIONS = 3;
    private static final double MIN_FUZZY_SUGGESTION_SCORE = 0.56;
    private TrashDB trashDB;
    private final List<String> cachedProduktList = new ArrayList<>();
    private final JaccardSimilarity jaccardSimilarity = new JaccardSimilarity();
    private final JaroWinklerSimilarity jaroWinklerSimilarity = new JaroWinklerSimilarity();
    private final LevenshteinDistance levenshteinDistance = LevenshteinDistance.getDefaultInstance();
    private ImageButton search;
    private MultiAutoCompleteTextView inputText;
    private ImageView twoItemsImage1;
    private ImageView twoItemsImage2;
    private ImageView oneItemImage1;
    private TextView resultText;
    private TextView twoItemsImage1Text;
    private TextView twoItemsImage2Text;
    private TextView oneItemImage1Text;
    private ImageView twoItemsIcon1;
    private ImageView twoItemsIcon2;
    private ImageView oneItemIcon1;
    private ImageView insertTrashImage;
    private LinearLayout emptyStateContainer;
    private TextView emptyStateTitle;
    private TextView exampleSearchPizza;
    private TextView exampleSearchBattery;
    private TextView exampleSearchCoffeeFilter;
    private TextView productDescriptionToggle;
    private TextView productDescriptionText;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View v = inflater.inflate(R.layout.fragment_search_trash, container, false);

        Resources resources = requireContext().getResources();
        trashDB = new TrashDB(resources);

        search = v.findViewById(R.id.search_button);
        inputText = v.findViewById(R.id.insert_trash);
        resultText = v.findViewById(R.id.result_text);

        twoItemsImage1 = v.findViewById(R.id.twoItemsImage1);
        twoItemsImage2 = v.findViewById(R.id.twoItemsImage2);
        oneItemImage1 = v.findViewById(R.id.oneItemImage1);

        twoItemsIcon1 = v.findViewById(R.id.twoItemsIcon1);
        twoItemsIcon2 = v.findViewById(R.id.twoItemsIcon2);
        oneItemIcon1 = v.findViewById(R.id.oneItemIcon1);

        twoItemsImage1Text = v.findViewById(R.id.twoItemsImage1Text);
        twoItemsImage2Text = v.findViewById(R.id.twoItemsImage2Text);
        oneItemImage1Text = v.findViewById(R.id.oneItemImage1Text);

        insertTrashImage = v.findViewById(R.id.indtastAffaldBillede);
        emptyStateContainer = v.findViewById(R.id.emptyStateContainer);
        emptyStateTitle = v.findViewById(R.id.emptyStateTitle);
        exampleSearchPizza = v.findViewById(R.id.exampleSearchPizza);
        exampleSearchBattery = v.findViewById(R.id.exampleSearchBattery);
        exampleSearchCoffeeFilter = v.findViewById(R.id.exampleSearchCoffeeFilter);
        productDescriptionToggle = v.findViewById(R.id.productDescriptionToggle);
        productDescriptionText = v.findViewById(R.id.productDescriptionText);

        setInsertTrashImageForLanguage();

        NativeAdHelper.loadNativeAd(requireContext(), v);

        inputText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    search.performClick();
                    return true;
                }
                return false;
            }
        });

        View customMultiAutoCompleteView = v.findViewById(R.id.customLayout);

        MultiAutoCompleteTextView multiAutoCompleteTextView = customMultiAutoCompleteView.findViewById(R.id.insert_trash);
        multiAutoCompleteTextView.setDropDownAnchor(R.id.customLayout);

        ImageView clearButton = customMultiAutoCompleteView.findViewById(R.id.clearButton);

        inputText.setOnItemClickListener((parent, view, position, id) -> {
            String selectedSuggestion = (String) parent.getItemAtPosition(position);
            inputText.setText(SuggestionAdapter.getSearchValue(selectedSuggestion));
            inputText.setSelection(inputText.length());
            search.performClick();
        });

        multiAutoCompleteTextView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                clearButton.setVisibility(charSequence.length() > 0 ? View.VISIBLE : View.GONE);
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        });

        clearButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                multiAutoCompleteTextView.setText("");
                multiAutoCompleteTextView.requestFocus();

                InputMethodManager imm = (InputMethodManager) view.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.showSoftInput(multiAutoCompleteTextView, InputMethodManager.SHOW_IMPLICIT);
            }
        });

        setupExampleSearch(exampleSearchPizza);
        setupExampleSearch(exampleSearchBattery);
        setupExampleSearch(exampleSearchCoffeeFilter);

        search.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                insertTrashImage.setImageDrawable(null);
                clearTextAndImages();
                resultText.setOnClickListener(null);
                String what = SuggestionAdapter.getSearchValue(inputText.getText().toString()).trim();
                boolean useEnglish = LanguageManager.isEnglish(requireContext());
                inputText.setText(what);
                inputText.setSelection(inputText.length());
                if (!what.equals("")) {
                    saveRecentSearch(what);
                }

                trashDB.searchProductJson(what, useEnglish, new TrashDB.OnSearchCompleteListener() {
                    @Override
                    public void onSearchComplete(Map<String, String> sorteringMap) {

                        String isSpecialText = "";
                        for (String key : sorteringMap.keySet()) {
                            if (key.equals("Genbrugsplads")) {
                                isSpecialText = "genbrugsplads";
                                break;
                            }
                            if (key.equals("Politistation")) {
                                isSpecialText = "politistation";
                                break;
                            }
                            if (key.equals("Apotek")) {
                                isSpecialText = "apotek";
                                break;
                            }
                            if (key.equals("Vask")) {
                                isSpecialText = "vask";
                                break;
                            }
                        }

                        if (what.equals("")) {
                            resultText.setText(R.string.tomt_søgefelt);
                        } else if (sorteringMap.containsKey("not found") && cachedProduktList != null) {
                            emptyStateContainer.setVisibility(View.VISIBLE);
                            resultText.setText(getString(R.string.affald_ikke_fundet_uden_forslag, what));
                            updateEmptyStateSuggestions(what);
                        } else if (sorteringMap.containsKey("error")) {
                            insertTrashImage.setImageResource(R.drawable.error_image);
                            emptyStateContainer.setVisibility(View.VISIBLE);
                        } else {
                            emptyStateContainer.setVisibility(View.GONE);
                            trashDB.colorProductName(resultText, what, getActivity(), isSpecialText, useEnglish);
                            setupProductDescription(what, useEnglish);
                            int numImages = sorteringMap.size();

                            if (numImages == 2) {
                                int index = 0;
                                for (Map.Entry<String, String> entry : sorteringMap.entrySet()) {
                                    String key = entry.getKey();
                                    String value = entry.getValue();
                                    if (index == 0) {
                                        trashDB.setImageViewAndText(twoItemsImage1, twoItemsImage1Text, getResources(), useEnglish ? trashDB.translateSortingKey(key) : key, value, useEnglish);
                                        twoItemsIcon1.setVisibility(ImageView.VISIBLE);
                                    } else if (index == 1) {
                                        trashDB.setImageViewAndText(twoItemsImage2, twoItemsImage2Text, getResources(), useEnglish ? trashDB.translateSortingKey(key) : key, value, useEnglish);
                                        twoItemsIcon2.setVisibility(ImageView.VISIBLE);
                                    }

                                    index++;
                                }
                            } else if (numImages == 1) {
                                for (Map.Entry<String, String> entry : sorteringMap.entrySet()) {
                                    String key = entry.getKey();
                                    String value = "";
                                    if (!entry.getValue().equals("Hele genstand")) {
                                        value = entry.getValue();
                                        oneItemIcon1.setVisibility(ImageView.VISIBLE);
                                    }
                                    trashDB.setImageViewAndText(oneItemImage1, oneItemImage1Text, getResources(), useEnglish ? trashDB.translateSortingKey(key) : key, value, useEnglish);
                                }
                            }
                        }
                        Context context = v.getContext();
                        InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
                        imm.hideSoftInputFromWindow(inputText.getWindowToken(), 0);
                    }
                });
            }
        });
        return v;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupAutoCompleteAdapter(cachedProduktList);
        fetchProduktList();
        if (getArguments() != null) {
            String initialQuery = getArguments().getString("initialQuery", "");
            if (!initialQuery.isEmpty()) {
                inputText.setText(initialQuery);
                inputText.setSelection(inputText.length());
                search.post(() -> search.performClick());
            }
        }
    }

    private void fetchProduktList() {
        if (trashDB.trashItems != null) {
            synchronized (cachedProduktList) {
                cachedProduktList.clear();

                for (TrashDB.TrashItem item : trashDB.trashItems) {
                    String productName = item.getDisplayProduct(LanguageManager.isEnglish(requireContext()));
                    if (productName != null && !productName.isEmpty()) {
                        cachedProduktList.add(productName);
                    }
                }

                setupAutoCompleteAdapter(cachedProduktList);
                Log.d(TAG, "Fetched autocomplete suggestions from JSON.");
            }
        } else {
            Log.w(TAG, "JSON data not available for autocomplete suggestions.");
            setupAutoCompleteAdapter(cachedProduktList);
        }
    }

    private void clearTextAndImages() {
        twoItemsImage1.setImageResource(0);
        twoItemsImage2.setImageResource(0);
        oneItemImage1.setImageResource(0);
        emptyStateContainer.setVisibility(View.GONE);
        oneItemIcon1.setVisibility(ImageView.GONE);
        twoItemsIcon1.setVisibility(ImageView.GONE);
        twoItemsIcon2.setVisibility(ImageView.GONE);
        twoItemsImage1Text.setText("");
        twoItemsImage2Text.setText("");
        oneItemImage1Text.setText("");
        productDescriptionToggle.setVisibility(View.GONE);
        productDescriptionText.setVisibility(View.GONE);
        productDescriptionText.setText("");
        productDescriptionToggle.setText(R.string.laes_mere_om_affaldet);
        productDescriptionToggle.setOnClickListener(null);

    }

    private void setInsertTrashImageForLanguage() {
        boolean useEnglish = LanguageManager.isEnglish(requireContext());
        insertTrashImage.setImageResource(useEnglish
                ? R.drawable.indtast_affald_billede_en
                : R.drawable.indtast_affald_billede);
    }

    private void setupProductDescription(String productName, boolean useEnglish) {
        String description = trashDB.getProductDescription(productName, useEnglish);
        if (description == null || description.trim().isEmpty()) {
            productDescriptionToggle.setVisibility(View.GONE);
            productDescriptionText.setVisibility(View.GONE);
            return;
        }

        productDescriptionText.setText(description.trim());
        productDescriptionText.setVisibility(View.GONE);
        productDescriptionToggle.setText(R.string.laes_mere_om_affaldet);
        productDescriptionToggle.setVisibility(View.VISIBLE);
        productDescriptionToggle.setOnClickListener(view -> {
            boolean shouldShow = productDescriptionText.getVisibility() != View.VISIBLE;
            productDescriptionText.setVisibility(shouldShow ? View.VISIBLE : View.GONE);
            productDescriptionToggle.setText(shouldShow ? R.string.skjul_beskrivelse : R.string.laes_mere_om_affaldet);
        });
    }

    private void setupExampleSearch(TextView exampleView) {
        exampleView.setOnClickListener(view -> {
            inputText.setText(exampleView.getText().toString());
            inputText.setSelection(inputText.length());
            search.performClick();
        });
    }

    private void updateEmptyStateSuggestions(String query) {
        List<String> suggestions = findSimilarSuggestions(query);
        boolean hasFuzzySuggestions = !suggestions.isEmpty();

        if (!hasFuzzySuggestions) {
            suggestions = getDefaultSuggestions();
        }

        emptyStateTitle.setText(hasFuzzySuggestions
                ? R.string.mente_du_en_af_disse
                : R.string.proev_en_af_disse_soegninger);

        TextView[] suggestionViews = {exampleSearchPizza, exampleSearchBattery, exampleSearchCoffeeFilter};
        for (int i = 0; i < suggestionViews.length; i++) {
            if (i < suggestions.size()) {
                suggestionViews[i].setText(suggestions.get(i));
                suggestionViews[i].setVisibility(View.VISIBLE);
            } else {
                suggestionViews[i].setVisibility(View.GONE);
            }
        }
    }

    private List<String> getDefaultSuggestions() {
        List<String> defaults = new ArrayList<>();
        defaults.add(getString(R.string.example_pizzabakke));
        defaults.add(getString(R.string.example_batteri));
        defaults.add(getString(R.string.example_kaffefilter));
        return defaults;
    }

    private void saveRecentSearch(String query) {
        SharedPreferences preferences = requireContext().getSharedPreferences(RECENT_SEARCHES_PREFS, Context.MODE_PRIVATE);
        List<String> searches = new ArrayList<>();
        String storedSearches = preferences.getString(RECENT_SEARCHES_KEY, "");

        if (!storedSearches.isEmpty()) {
            for (String storedSearch : storedSearches.split("\\n")) {
                if (!storedSearch.equalsIgnoreCase(query) && !storedSearch.trim().isEmpty()) {
                    searches.add(storedSearch);
                }
            }
        }

        searches.add(0, query);
        if (searches.size() > MAX_RECENT_SEARCHES) {
            searches = searches.subList(0, MAX_RECENT_SEARCHES);
        }

        preferences.edit().putString(RECENT_SEARCHES_KEY, String.join("\n", searches)).apply();
    }

    private void setupAutoCompleteAdapter(List<String> produktList) {
        SuggestionAdapter suggestionAdapter = new SuggestionAdapter(requireActivity(), produktList);

        inputText.setAdapter(suggestionAdapter);
        inputText.setThreshold(1);
        inputText.setTokenizer(new SuggestionTokenizer());
    }

    private double calculateFuzzyScore(String input, String suggestion) {
        String normalizedInput = normalizeForFuzzySearch(input);
        String normalizedSuggestion = normalizeForFuzzySearch(suggestion);

        if (normalizedInput.isEmpty() || normalizedSuggestion.isEmpty()) {
            return 0.0;
        }

        if (normalizedInput.equals(normalizedSuggestion)) {
            return 1.0;
        }

        double jaccardScore = jaccardSimilarity.apply(normalizedInput, normalizedSuggestion);
        double jaroScore = jaroWinklerSimilarity.apply(normalizedInput, normalizedSuggestion);
        int distance = levenshteinDistance.apply(normalizedInput, normalizedSuggestion);
        double levenshteinScore = 1.0 - ((double) distance / Math.max(normalizedInput.length(), normalizedSuggestion.length()));
        double containsBoost = normalizedSuggestion.contains(normalizedInput) || normalizedInput.contains(normalizedSuggestion) ? 0.16 : 0.0;
        double prefixBoost = normalizedSuggestion.startsWith(normalizedInput.substring(0, Math.min(normalizedInput.length(), 2))) ? 0.06 : 0.0;

        return Math.min(1.0, (jaccardScore * 0.25) + (jaroScore * 0.45) + (levenshteinScore * 0.30) + containsBoost + prefixBoost);
    }

    private String normalizeForFuzzySearch(String value) {
        return value == null
                ? ""
                : value.toLowerCase()
                .replace("æ", "ae")
                .replace("ø", "oe")
                .replace("å", "aa")
                .replaceAll("[^a-z0-9]+", "");
    }

    private List<String> findSimilarSuggestions(String input) {
        List<FuzzySuggestion> rankedSuggestions = new ArrayList<>();

        if (input == null || input.trim().length() < 2) {
            return new ArrayList<>();
        }

        for (String suggestion : cachedProduktList) {
            double score = calculateFuzzyScore(input, suggestion);
            if (score >= MIN_FUZZY_SUGGESTION_SCORE) {
                rankedSuggestions.add(new FuzzySuggestion(suggestion, score));
            }
        }

        rankedSuggestions.sort(Comparator
                .comparingDouble((FuzzySuggestion suggestion) -> suggestion.score)
                .reversed()
                .thenComparing(suggestion -> suggestion.value));

        List<String> suggestions = new ArrayList<>();
        for (FuzzySuggestion suggestion : rankedSuggestions) {
            if (!suggestions.contains(suggestion.value)) {
                suggestions.add(suggestion.value);
            }

            if (suggestions.size() == MAX_EMPTY_STATE_SUGGESTIONS) {
                break;
            }
        }

        return suggestions;
    }

    private static class FuzzySuggestion {
        private final String value;
        private final double score;

        private FuzzySuggestion(String value, double score) {
            this.value = value;
            this.score = score;
        }
    }

}
