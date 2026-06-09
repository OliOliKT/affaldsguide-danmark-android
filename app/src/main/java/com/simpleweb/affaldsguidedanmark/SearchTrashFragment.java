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
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

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
    private static final String SUCCESSFUL_SEARCH_EXAMPLES_KEY_PREFIX = "successful_queries_";
    private static final int MAX_RECENT_SEARCHES = 10;
    private static final int MAX_EMPTY_STATE_SUGGESTIONS = 3;
    private static final int MAX_INTRO_EXAMPLES = 4;
    private static final int MIN_SUCCESSFUL_SEARCHES_FOR_RECENT_CHIPS = 4;
    private TrashDB trashDB;
    private final List<String> cachedProduktList = new ArrayList<>();
    private final JaccardSimilarity jaccardSimilarity = new JaccardSimilarity();
    private final JaroWinklerSimilarity jaroWinklerSimilarity = new JaroWinklerSimilarity();
    private final LevenshteinDistance levenshteinDistance = LevenshteinDistance.getDefaultInstance();
    private ImageButton search;
    private MultiAutoCompleteTextView inputText;
    private ImageView twoItemsImage1;
    private ImageView twoItemsImage2;
    private ImageView threeItemsImage1;
    private ImageView threeItemsImage2;
    private ImageView threeItemsImage3;
    private ImageView oneItemImage1;
    private TextView resultText;
    private TextView twoItemsImage1Text;
    private TextView twoItemsImage2Text;
    private TextView threeItemsImage1Text;
    private TextView threeItemsImage2Text;
    private TextView threeItemsImage3Text;
    private TextView oneItemImage1Text;
    private LinearLayout twoItemsImage1InfoRow;
    private LinearLayout twoItemsImage2InfoRow;
    private LinearLayout threeItemsImage1InfoRow;
    private LinearLayout threeItemsImage2InfoRow;
    private LinearLayout threeItemsImage3InfoRow;
    private LinearLayout oneItemImage1InfoRow;
    private ImageView twoItemsIcon1;
    private ImageView twoItemsIcon2;
    private ImageView threeItemsIcon1;
    private ImageView threeItemsIcon2;
    private ImageView threeItemsIcon3;
    private ImageView oneItemIcon1;
    private ImageView insertTrashImage;
    private LinearLayout searchIntroContainer;
    private LinearLayout emptyStateContainer;
    private TextView emptyStateTitle;
    private TextView exampleSearchPizza;
    private TextView exampleSearchBattery;
    private TextView exampleSearchCoffeeFilter;
    private TextView introExampleSearchPizza;
    private TextView introExampleSearchBattery;
    private TextView introExampleSearchCoffeeFilter;
    private TextView introExampleSearchGlass;
    private LinearLayout introExampleSearchItem1;
    private LinearLayout introExampleSearchItem2;
    private LinearLayout introExampleSearchItem3;
    private LinearLayout introExampleSearchItem4;
    private ImageView introExampleImage1;
    private ImageView introExampleImage2;
    private ImageView introExampleImage3;
    private ImageView introExampleImage4;
    private TextView searchIntroExamplesLabel;
    private TextView searchIntroSeeAll;
    private LinearLayout savedMunicipalityContainer;
    private TextView savedMunicipalityName;
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
        threeItemsImage1 = v.findViewById(R.id.threeItemsImage1);
        threeItemsImage2 = v.findViewById(R.id.threeItemsImage2);
        threeItemsImage3 = v.findViewById(R.id.threeItemsImage3);
        oneItemImage1 = v.findViewById(R.id.oneItemImage1);

        twoItemsIcon1 = v.findViewById(R.id.twoItemsIcon1);
        twoItemsIcon2 = v.findViewById(R.id.twoItemsIcon2);
        threeItemsIcon1 = v.findViewById(R.id.threeItemsIcon1);
        threeItemsIcon2 = v.findViewById(R.id.threeItemsIcon2);
        threeItemsIcon3 = v.findViewById(R.id.threeItemsIcon3);
        oneItemIcon1 = v.findViewById(R.id.oneItemIcon1);
        insertTrashImage = v.findViewById(R.id.indtastAffaldBillede);

        twoItemsImage1Text = v.findViewById(R.id.twoItemsImage1Text);
        twoItemsImage2Text = v.findViewById(R.id.twoItemsImage2Text);
        threeItemsImage1Text = v.findViewById(R.id.threeItemsImage1Text);
        threeItemsImage2Text = v.findViewById(R.id.threeItemsImage2Text);
        threeItemsImage3Text = v.findViewById(R.id.threeItemsImage3Text);
        oneItemImage1Text = v.findViewById(R.id.oneItemImage1Text);
        twoItemsImage1InfoRow = v.findViewById(R.id.twoItemsImage1InfoRow);
        twoItemsImage2InfoRow = v.findViewById(R.id.twoItemsImage2InfoRow);
        threeItemsImage1InfoRow = v.findViewById(R.id.threeItemsImage1InfoRow);
        threeItemsImage2InfoRow = v.findViewById(R.id.threeItemsImage2InfoRow);
        threeItemsImage3InfoRow = v.findViewById(R.id.threeItemsImage3InfoRow);
        oneItemImage1InfoRow = v.findViewById(R.id.oneItemImage1InfoRow);

        searchIntroContainer = v.findViewById(R.id.searchIntroContainer);
        emptyStateContainer = v.findViewById(R.id.emptyStateContainer);
        emptyStateTitle = v.findViewById(R.id.emptyStateTitle);
        exampleSearchPizza = v.findViewById(R.id.exampleSearchPizza);
        exampleSearchBattery = v.findViewById(R.id.exampleSearchBattery);
        exampleSearchCoffeeFilter = v.findViewById(R.id.exampleSearchCoffeeFilter);
        introExampleSearchPizza = v.findViewById(R.id.introExampleSearchPizza);
        introExampleSearchBattery = v.findViewById(R.id.introExampleSearchBattery);
        introExampleSearchCoffeeFilter = v.findViewById(R.id.introExampleSearchCoffeeFilter);
        introExampleSearchGlass = v.findViewById(R.id.introExampleSearchGlass);
        introExampleSearchItem1 = v.findViewById(R.id.introExampleSearchItem1);
        introExampleSearchItem2 = v.findViewById(R.id.introExampleSearchItem2);
        introExampleSearchItem3 = v.findViewById(R.id.introExampleSearchItem3);
        introExampleSearchItem4 = v.findViewById(R.id.introExampleSearchItem4);
        introExampleImage1 = v.findViewById(R.id.introExampleImage1);
        introExampleImage2 = v.findViewById(R.id.introExampleImage2);
        introExampleImage3 = v.findViewById(R.id.introExampleImage3);
        introExampleImage4 = v.findViewById(R.id.introExampleImage4);
        searchIntroExamplesLabel = v.findViewById(R.id.searchIntroExamplesLabel);
        searchIntroSeeAll = v.findViewById(R.id.searchIntroSeeAll);
        savedMunicipalityContainer = v.findViewById(R.id.savedMunicipalityContainer);
        savedMunicipalityName = v.findViewById(R.id.savedMunicipalityName);
        productDescriptionToggle = v.findViewById(R.id.productDescriptionToggle);
        productDescriptionText = v.findViewById(R.id.productDescriptionText);

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
        setupIntroExampleSearch(introExampleSearchItem1, introExampleSearchPizza);
        setupIntroExampleSearch(introExampleSearchItem2, introExampleSearchBattery);
        setupIntroExampleSearch(introExampleSearchItem3, introExampleSearchCoffeeFilter);
        setupIntroExampleSearch(introExampleSearchItem4, introExampleSearchGlass);
        searchIntroSeeAll.setOnClickListener(view -> Navigation.findNavController(view).navigate(R.id.fragment_recent_searches));
        updateIntroExampleSearches();
        updateSavedMunicipalityChip();
        setInsertTrashImageForLanguage();

        search.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clearTextAndImages();
                resultText.setOnClickListener(null);
                String what = SuggestionAdapter.getSearchValue(inputText.getText().toString()).trim();
                boolean useEnglish = LanguageManager.isEnglish(requireContext());
                inputText.setText(what);
                inputText.setSelection(inputText.length());

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
                            searchIntroContainer.setVisibility(View.VISIBLE);
                            insertTrashImage.setVisibility(ImageView.GONE);
                            resultText.setText(R.string.tomt_søgefelt);
                        } else if (sorteringMap.containsKey("not found") && cachedProduktList != null) {
                            emptyStateContainer.setVisibility(View.VISIBLE);
                            resultText.setText(getString(R.string.affald_ikke_fundet_uden_forslag, what));
                            updateEmptyStateSuggestions(what);
                        } else if (sorteringMap.containsKey("error")) {
                            emptyStateContainer.setVisibility(View.VISIBLE);
                        } else {
                            emptyStateContainer.setVisibility(View.GONE);
                            saveRecentSearch(what);
                            saveSuccessfulSearchExample(what, useEnglish);
                            updateIntroExampleSearches();
                            trashDB.colorProductName(resultText, what, getActivity(), isSpecialText, useEnglish);
                            setupProductDescription(what, useEnglish);
                            setProductDescriptionToggleTopMargin(22);
                            int numImages = sorteringMap.size();

                            if (numImages >= 3) {
                                threeItemsImage1.setVisibility(ImageView.VISIBLE);
                                threeItemsImage2.setVisibility(ImageView.VISIBLE);
                                threeItemsImage3.setVisibility(ImageView.VISIBLE);
                                threeItemsImage1InfoRow.setVisibility(View.VISIBLE);
                                threeItemsImage2InfoRow.setVisibility(View.VISIBLE);
                                threeItemsImage3InfoRow.setVisibility(View.VISIBLE);
                                ImageView[] images = {threeItemsImage1, threeItemsImage2, threeItemsImage3};
                                TextView[] labels = {threeItemsImage1Text, threeItemsImage2Text, threeItemsImage3Text};
                                ImageView[] icons = {threeItemsIcon1, threeItemsIcon2, threeItemsIcon3};
                                int index = 0;
                                for (Map.Entry<String, String> entry : sorteringMap.entrySet()) {
                                    if (index >= images.length) {
                                        break;
                                    }
                                    String key = entry.getKey();
                                    String value = entry.getValue();
                                    trashDB.setImageViewAndText(images[index], labels[index], getResources(), useEnglish ? trashDB.translateSortingKey(key) : key, value, useEnglish);
                                    icons[index].setVisibility(value == null || value.isEmpty() ? ImageView.INVISIBLE : ImageView.VISIBLE);
                                    index++;
                                }
                            } else if (numImages == 2) {
                                twoItemsImage1.setVisibility(ImageView.VISIBLE);
                                twoItemsImage2.setVisibility(ImageView.VISIBLE);
                                twoItemsImage1InfoRow.setVisibility(View.VISIBLE);
                                twoItemsImage2InfoRow.setVisibility(View.VISIBLE);
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
                                oneItemImage1.setVisibility(ImageView.VISIBLE);
                                for (Map.Entry<String, String> entry : sorteringMap.entrySet()) {
                                    String key = entry.getKey();
                                    String guidance = entry.getValue();
                                    String value = "";
                                    boolean hasGuidance = guidance != null
                                            && !guidance.isEmpty()
                                            && !guidance.equals("Hele genstand")
                                            && !guidance.equals("Whole item");
                                    if (hasGuidance) {
                                        value = guidance;
                                        oneItemImage1InfoRow.setVisibility(View.VISIBLE);
                                        oneItemIcon1.setVisibility(ImageView.VISIBLE);
                                    } else {
                                        oneItemImage1InfoRow.setVisibility(View.GONE);
                                        setProductDescriptionToggleTopMargin(14);
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

    @Override
    public void onResume() {
        super.onResume();
        updateSavedMunicipalityChip();
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
        threeItemsImage1.setImageResource(0);
        threeItemsImage2.setImageResource(0);
        threeItemsImage3.setImageResource(0);
        oneItemImage1.setImageResource(0);
        searchIntroContainer.setVisibility(View.VISIBLE);
        insertTrashImage.setVisibility(ImageView.GONE);
        emptyStateContainer.setVisibility(View.GONE);
        oneItemImage1.setVisibility(ImageView.GONE);
        twoItemsImage1.setVisibility(ImageView.GONE);
        twoItemsImage2.setVisibility(ImageView.GONE);
        threeItemsImage1.setVisibility(ImageView.GONE);
        threeItemsImage2.setVisibility(ImageView.GONE);
        threeItemsImage3.setVisibility(ImageView.GONE);
        oneItemImage1InfoRow.setVisibility(View.GONE);
        twoItemsImage1InfoRow.setVisibility(View.GONE);
        twoItemsImage2InfoRow.setVisibility(View.GONE);
        threeItemsImage1InfoRow.setVisibility(View.GONE);
        threeItemsImage2InfoRow.setVisibility(View.GONE);
        threeItemsImage3InfoRow.setVisibility(View.GONE);
        oneItemIcon1.setVisibility(ImageView.GONE);
        twoItemsIcon1.setVisibility(ImageView.GONE);
        twoItemsIcon2.setVisibility(ImageView.GONE);
        threeItemsIcon1.setVisibility(ImageView.GONE);
        threeItemsIcon2.setVisibility(ImageView.GONE);
        threeItemsIcon3.setVisibility(ImageView.GONE);
        twoItemsImage1Text.setText("");
        twoItemsImage2Text.setText("");
        threeItemsImage1Text.setText("");
        threeItemsImage2Text.setText("");
        threeItemsImage3Text.setText("");
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

    private void setProductDescriptionToggleTopMargin(int marginTopDp) {
        ViewGroup.LayoutParams currentParams = productDescriptionToggle.getLayoutParams();
        if (currentParams instanceof ConstraintLayout.LayoutParams) {
            ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) currentParams;
            params.topMargin = Math.round(marginTopDp * getResources().getDisplayMetrics().density);
            productDescriptionToggle.setLayoutParams(params);
        }
    }

    private void setupExampleSearch(TextView exampleView) {
        exampleView.setOnClickListener(view -> {
            inputText.setText(exampleView.getText().toString());
            inputText.setSelection(inputText.length());
            search.performClick();
        });
    }

    private void setupIntroExampleSearch(View exampleContainer, TextView exampleLabel) {
        exampleContainer.setOnClickListener(view -> {
            inputText.setText(exampleLabel.getText().toString());
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
        defaults.add(getString(R.string.example_glas));
        return defaults;
    }

    private void updateIntroExampleSearches() {
        List<String> suggestions = getSuccessfulSearchExamples(LanguageManager.isEnglish(requireContext()));
        boolean usesRecentSearches = suggestions.size() >= MIN_SUCCESSFUL_SEARCHES_FOR_RECENT_CHIPS;

        if (!usesRecentSearches) {
            suggestions = getDefaultSuggestions();
        }

        searchIntroExamplesLabel.setText(usesRecentSearches
                ? R.string.search_intro_recent_label
                : R.string.search_intro_examples_label);
        searchIntroSeeAll.setVisibility(usesRecentSearches ? View.VISIBLE : View.GONE);

        TextView[] introViews = {introExampleSearchPizza, introExampleSearchBattery, introExampleSearchCoffeeFilter, introExampleSearchGlass};
        ImageView[] introImages = {introExampleImage1, introExampleImage2, introExampleImage3, introExampleImage4};
        LinearLayout[] introContainers = {introExampleSearchItem1, introExampleSearchItem2, introExampleSearchItem3, introExampleSearchItem4};
        int visibleExamples = Math.min(suggestions.size(), MAX_INTRO_EXAMPLES);
        for (int i = 0; i < introViews.length; i++) {
            if (i < visibleExamples) {
                introViews[i].setText(suggestions.get(i));
                introImages[i].setImageResource(getIntroExampleImageResource(suggestions.get(i)));
                introContainers[i].setVisibility(View.VISIBLE);
            } else {
                introContainers[i].setVisibility(View.GONE);
            }
        }
    }

    private void updateSavedMunicipalityChip() {
        if (savedMunicipalityContainer == null || savedMunicipalityName == null || getContext() == null) {
            return;
        }

        String savedMunicipalityName = SavedMunicipalityManager.getSavedMunicipalityName(requireContext());
        if (savedMunicipalityName == null || savedMunicipalityName.trim().isEmpty()) {
            savedMunicipalityContainer.setVisibility(View.GONE);
            savedMunicipalityContainer.setOnClickListener(null);
            return;
        }

        this.savedMunicipalityName.setText(savedMunicipalityName);
        savedMunicipalityContainer.setVisibility(View.VISIBLE);
        savedMunicipalityContainer.setOnClickListener(view -> {
            Municipality savedMunicipality = findMunicipalityByName(savedMunicipalityName);
            if (savedMunicipality == null) {
                Navigation.findNavController(view).navigate(R.id.fragment_municipalities);
                return;
            }

            Bundle args = new Bundle();
            args.putParcelable("municipality", savedMunicipality);
            Navigation.findNavController(view).navigate(R.id.fragment_municipality_details, args);
        });
    }

    private Municipality findMunicipalityByName(String municipalityName) {
        MunicipalityDB municipalityDB = new MunicipalityDB(getResources());
        for (Municipality municipality : municipalityDB.getMunicipalities()) {
            if (municipality.getMunicipality().equalsIgnoreCase(municipalityName)) {
                return municipality;
            }
        }

        return null;
    }

    private int getIntroExampleImageResource(String productName) {
        if (productName.equalsIgnoreCase(getString(R.string.example_pizzabakke))) {
            return R.drawable.mad_og_drikkekartoner_ikon;
        }
        if (productName.equalsIgnoreCase(getString(R.string.example_batteri))) {
            return R.drawable.batterier;
        }
        if (productName.equalsIgnoreCase(getString(R.string.example_kaffefilter))) {
            return R.drawable.madaffald_ikon;
        }
        if (productName.equalsIgnoreCase(getString(R.string.example_glas))) {
            return R.drawable.glas_ikon;
        }

        boolean useEnglish = LanguageManager.isEnglish(requireContext());
        String sortingKey = trashDB.getFirstSortingKeyForProduct(productName, useEnglish);
        if (useEnglish) {
            sortingKey = trashDB.translateSortingKey(sortingKey);
        }

        return trashDB.getImageResourceForKey(sortingKey);
    }

    private List<String> getSuccessfulSearchExamples(boolean useEnglish) {
        SharedPreferences preferences = requireContext().getSharedPreferences(RECENT_SEARCHES_PREFS, Context.MODE_PRIVATE);
        String storedSearches = preferences.getString(getSuccessfulSearchExamplesKey(useEnglish), "");
        List<String> searches = new ArrayList<>();

        if (!storedSearches.isEmpty()) {
            for (String storedSearch : storedSearches.split("\\n")) {
                if (!storedSearch.trim().isEmpty()) {
                    searches.add(storedSearch);
                }
            }
        }

        return searches;
    }

    private String getSuccessfulSearchExamplesKey(boolean useEnglish) {
        return SUCCESSFUL_SEARCH_EXAMPLES_KEY_PREFIX + (useEnglish ? LanguageManager.ENGLISH : LanguageManager.DANISH);
    }

    private void saveSuccessfulSearchExample(String query, boolean useEnglish) {
        SharedPreferences preferences = requireContext().getSharedPreferences(RECENT_SEARCHES_PREFS, Context.MODE_PRIVATE);
        String key = getSuccessfulSearchExamplesKey(useEnglish);
        List<String> searches = new ArrayList<>();
        String storedSearches = preferences.getString(key, "");

        if (!storedSearches.isEmpty()) {
            for (String storedSearch : storedSearches.split("\\n")) {
                if (!storedSearch.equalsIgnoreCase(query) && !storedSearch.trim().isEmpty()) {
                    searches.add(storedSearch);
                }
            }
        }

        searches.add(0, query);
        if (searches.size() > MAX_INTRO_EXAMPLES) {
            searches = searches.subList(0, MAX_INTRO_EXAMPLES);
        }

        preferences.edit().putString(key, String.join("\n", searches)).apply();
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
            if (score > 0.0) {
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
