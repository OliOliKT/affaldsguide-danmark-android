package com.simpleweb.affaldsguidedanmark;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.text.Editable;
import android.text.SpannableString;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.text.style.UnderlineSpan;
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
import java.util.List;
import java.util.Map;

import org.apache.commons.text.similarity.JaccardSimilarity;

public class SearchTrashFragment extends Fragment {

    private static final String TAG = "SearchTrashFragment";
    private static final double MIN_JACCARD_SIMILARITY_THRESHOLD = 0.5;
    private static final String RECENT_SEARCHES_PREFS = "RecentSearches";
    private static final String RECENT_SEARCHES_KEY = "queries";
    private static final int MAX_RECENT_SEARCHES = 10;
    private TrashDB trashDB;
    private final List<String> cachedProduktList = new ArrayList<>();
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
        productDescriptionToggle = v.findViewById(R.id.productDescriptionToggle);
        productDescriptionText = v.findViewById(R.id.productDescriptionText);

        insertTrashImage.setImageResource(R.drawable.indtast_affald_billede);

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

        setupExampleSearch(v, R.id.exampleSearchPizza);
        setupExampleSearch(v, R.id.exampleSearchBattery);
        setupExampleSearch(v, R.id.exampleSearchCoffeeFilter);

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
                            String suggestedCorrection = findSimilarSuggestion(what);
                            if (suggestedCorrection != null && inputText.length() > 3) {
                                String suggestionText = getString(R.string.affald_ikke_fundet_med_forslag, what, suggestedCorrection);

                                SpannableString spannableString = new SpannableString(suggestionText);
                                int startIndex = suggestionText.lastIndexOf(suggestedCorrection);

                                if (startIndex != -1) {
                                    int endIndex = startIndex + suggestedCorrection.length();

                                    spannableString.setSpan(new UnderlineSpan(), startIndex, endIndex, 0);
                                    spannableString.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.green_light)), startIndex, endIndex, 0);
                                }

                                resultText.setText(spannableString);

                                resultText.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View view) {
                                        inputText.setText(suggestedCorrection);
                                        search.performClick();
                                    }
                                });
                            } else {
                                resultText.setText(getString(R.string.affald_ikke_fundet_uden_forslag, what));
                            }
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

    private void setupExampleSearch(View rootView, int textViewId) {
        TextView exampleView = rootView.findViewById(textViewId);
        exampleView.setOnClickListener(view -> {
            inputText.setText(exampleView.getText().toString());
            inputText.setSelection(inputText.length());
            search.performClick();
        });
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

    private double calculateJaccardSimilarity(String str1, String str2) {
        JaccardSimilarity jaccardSimilarity = new JaccardSimilarity();
        return jaccardSimilarity.apply(str1, str2);
    }

    private String findSimilarSuggestion(String input) {
        String suggestedCorrection = null;
        double maxSimilarity = 0.0;

        input = input.substring(0, 1).toUpperCase() + input.substring(1);

        if (cachedProduktList.contains(input)) {
            suggestedCorrection = input;
            return suggestedCorrection;
        }

        for (String suggestion : cachedProduktList) {
            double similarity = calculateJaccardSimilarity(input, suggestion);
            if (similarity > maxSimilarity) {
                maxSimilarity = similarity;
                suggestedCorrection = suggestion;
            }
        }

        if (maxSimilarity < MIN_JACCARD_SIMILARITY_THRESHOLD) {
            suggestedCorrection = null;
        }

        return suggestedCorrection;
    }

}
