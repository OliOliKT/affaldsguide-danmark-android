package com.simpleweb.affaldsguidedanmark;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.content.Intent;
import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.graphics.Typeface;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.view.Gravity;
import android.text.Editable;
import android.text.InputFilter;
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
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.MultiAutoCompleteTextView;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.navigation.Navigation;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.File;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

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
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 143;
    private static final String[] CORRECTION_CATEGORIES = {
            "Batterier", "Farligt affald", "Genbrug", "Genbrugsplads", "Glas",
            "Mad- og drikkekartoner", "Madaffald", "Metal", "Pant", "Pap", "Papir", "Plast",
            "Restaffald", "Småt elektronik", "Tekstilaffald"
    };
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
    private View selectedMunicipalityContent;
    private View chooseMunicipalityContent;
    private View municipalityCardHeader;
    private View municipalityNearestRow;
    private View municipalityFractionsRow;
    private View municipalityOperatorRow;
    private TextView municipalityNearestValue;
    private TextView municipalityFractionsValue;
    private TextView municipalityOperatorValue;
    private Municipality selectedMunicipality;
    private RecyclingCenter nearestRecyclingCenter;
    private boolean showingSearchStart = true;
    private Uri pendingCameraUri;
    private final ActivityResultLauncher<PickVisualMediaRequest> pickPhoto =
            registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), this::openPhotoSearch);
    private final ActivityResultLauncher<Uri> takePhoto =
            registerForActivityResult(new ActivityResultContracts.TakePicture(), success -> {
                if (Boolean.TRUE.equals(success)) openPhotoSearch(pendingCameraUri);
            });
    private TextView productDescriptionToggle;
    private TextView productDescriptionText;
    private TextView localRuleNote;
    private TextView localRuleSource;
    private LinearLayout fallbackSortingCard;
    private TextView fallbackSortingToggle;
    private LinearLayout fallbackSortingContent;
    private TextView suggestCorrectionButton;
    private String resultMunicipalityName;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View v = inflater.inflate(R.layout.fragment_search_trash, container, false);

        Resources resources = requireContext().getResources();
        trashDB = new TrashDB(resources);

        search = v.findViewById(R.id.search_button);
        TextView takePhotoButton = v.findViewById(R.id.takePhotoButton);
        TextView choosePhotoButton = v.findViewById(R.id.choosePhotoButton);
        takePhotoButton.setText(InfoPageText.get(requireContext(), "searchview.017"));
        choosePhotoButton.setText(InfoPageText.get(requireContext(), "searchview.018"));
        choosePhotoButton.setOnClickListener(view -> pickPhoto.launch(
                new PickVisualMediaRequest.Builder()
                        .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                        .build()));
        takePhotoButton.setOnClickListener(view -> {
            try {
                File directory = new File(requireContext().getCacheDir(), "photo_search");
                if (!directory.exists() && !directory.mkdirs()) throw new java.io.IOException("Cannot create camera directory");
                File image = File.createTempFile("capture_", ".jpg", directory);
                pendingCameraUri = FileProvider.getUriForFile(requireContext(),
                        requireContext().getPackageName() + ".fileprovider", image);
                takePhoto.launch(pendingCameraUri);
            } catch (Exception error) {
                Log.e(TAG, "Could not open camera", error);
                Toast.makeText(requireContext(), InfoPageText.get(requireContext(), "photosearchview.008"), Toast.LENGTH_LONG).show();
            }
        });
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
        selectedMunicipalityContent = v.findViewById(R.id.selectedMunicipalityContent);
        chooseMunicipalityContent = v.findViewById(R.id.chooseMunicipalityContent);
        municipalityCardHeader = v.findViewById(R.id.municipalityCardHeader);
        municipalityNearestRow = v.findViewById(R.id.municipalityNearestRow);
        municipalityFractionsRow = v.findViewById(R.id.municipalityFractionsRow);
        municipalityOperatorRow = v.findViewById(R.id.municipalityOperatorRow);
        municipalityNearestValue = v.findViewById(R.id.municipalityNearestValue);
        municipalityFractionsValue = v.findViewById(R.id.municipalityFractionsValue);
        municipalityOperatorValue = v.findViewById(R.id.municipalityOperatorValue);
        productDescriptionToggle = v.findViewById(R.id.productDescriptionToggle);
        productDescriptionText = v.findViewById(R.id.productDescriptionText);
        localRuleNote = v.findViewById(R.id.localRuleNote);
        localRuleSource = v.findViewById(R.id.localRuleSource);
        fallbackSortingCard = v.findViewById(R.id.fallbackSortingCard);
        fallbackSortingToggle = v.findViewById(R.id.fallbackSortingToggle);
        fallbackSortingContent = v.findViewById(R.id.fallbackSortingContent);
        suggestCorrectionButton = v.findViewById(R.id.suggestCorrectionButton);

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
                clearTextAndImages();
                resultText.setText("");
                showStartContent(true);
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
                showStartContent(what.isEmpty());
                boolean useEnglish = LanguageManager.usesNonDanishContent(requireContext());
                String municipalityName = SavedMunicipalityManager.getSavedMunicipalityName(requireContext());
                inputText.setText(what);
                inputText.setSelection(inputText.length());

                trashDB.searchProductJson(what, useEnglish, municipalityName, new TrashDB.OnSearchCompleteListener() {
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
                            showStartContent(true);
                            insertTrashImage.setVisibility(ImageView.GONE);
                            resultText.setText("");
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
                            MunicipalitySortingRuleDB.Rule localRule = trashDB.getLocalRule(what, useEnglish, municipalityName);
                            AchievementStore.get(requireContext()).recordItem(
                                    trashDB.getDanishProductName(what, useEnglish),
                                    trashDB.getFirstSortingKeyForProduct(what, useEnglish),
                                    localRule != null);
                            if (localRule != null) {
                                boolean isDropOff = false;
                                for (String category : sorteringMap.keySet()) {
                                    if (category.equals("Vask") || category.equals("Sink")
                                            || category.equals("Politistation") || category.equals("Police station")
                                            || category.equals("Apotek") || category.equals("Pharmacy")) {
                                        isDropOff = true;
                                    }
                                }
                                if (sorteringMap.size() == 1) {
                                    isDropOff |= sorteringMap.containsKey("Genbrugsplads")
                                            || sorteringMap.containsKey("Recycling centre");
                                }
                                String displayName = localResultItemName(what);
                                SpannableString title = new SpannableString(getString(isDropOff
                                        ? R.string.local_disposal_title : R.string.local_result_title,
                                        municipalityName, displayName));
                                int nameStart = title.toString().lastIndexOf(displayName);
                                if (nameStart >= 0 && !displayName.isEmpty()) {
                                    title.setSpan(new ForegroundColorSpan(ContextCompat.getColor(
                                                    requireContext(), R.color.green_light)),
                                            nameStart, nameStart + displayName.length(),
                                            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                                }
                                resultText.setText(title);
                            } else {
                                trashDB.colorProductName(resultText, what, getActivity(), isSpecialText, useEnglish);
                            }
                            resultMunicipalityName = municipalityName;
                            setupProductDescription(what, useEnglish, municipalityName, localRule);
                            showFallbackSorting(sorteringMap, municipalityName, useEnglish);
                            showLocalRuleDetails(localRule, useEnglish);
                            suggestCorrectionButton.setText(InfoPageText.get(requireContext(), "searchview.033"));
                            suggestCorrectionButton.setVisibility(View.VISIBLE);
                            suggestCorrectionButton.setOnClickListener(view ->
                                    showCorrectionForm(what, sorteringMap, municipalityName, useEnglish));
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
                                            && !guidance.equals("Whole item")
                                            && !guidance.equals(ArabicText.lookup(getResources(), "guidance.Hele genstand"));
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
        String currentMunicipality = SavedMunicipalityManager.getSavedMunicipalityName(requireContext());
        if (resultMunicipalityName != null && !resultMunicipalityName.equals(currentMunicipality)
                && inputText != null && inputText.length() > 0 && search != null) {
            search.performClick();
        }
    }

    private void fetchProduktList() {
        if (trashDB.trashItems != null) {
            synchronized (cachedProduktList) {
                cachedProduktList.clear();

                for (TrashDB.TrashItem item : trashDB.trashItems) {
                    String productName = item.getDisplayProduct(LanguageManager.usesNonDanishContent(requireContext()));
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
        resultMunicipalityName = null;
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
        localRuleNote.setVisibility(View.GONE);
        localRuleSource.setVisibility(View.GONE);
        localRuleSource.setOnClickListener(null);
        fallbackSortingCard.setVisibility(View.GONE);
        fallbackSortingContent.setVisibility(View.GONE);
        fallbackSortingContent.removeAllViews();
        suggestCorrectionButton.setVisibility(View.GONE);
        suggestCorrectionButton.setOnClickListener(null);

    }

    private void setInsertTrashImageForLanguage() {
        boolean useEnglish = LanguageManager.usesNonDanishContent(requireContext());
        insertTrashImage.setImageResource(useEnglish
                ? R.drawable.indtast_affald_billede_en
                : R.drawable.indtast_affald_billede);
    }

    private void setupProductDescription(String productName, boolean useEnglish,
                                         String municipalityName, MunicipalitySortingRuleDB.Rule localRule) {
        String description = trashDB.getProductDescription(productName, useEnglish);
        if (localRule != null) {
            List<String> categories = new ArrayList<>();
            for (String category : localRule.sortingFor(useEnglish).keySet()) {
                categories.add(useEnglish ? trashDB.translateSortingKey(category) : category);
            }
            Collections.sort(categories, String.CASE_INSENSITIVE_ORDER);
            if (!categories.isEmpty()) {
                String separator = LanguageManager.isArabic(requireContext()) ? " و" : useEnglish ? " and " : " og ";
                String categoryList = categories.size() == 1
                        ? categories.get(0)
                        : String.join(", ", categories.subList(0, categories.size() - 1))
                        + separator + categories.get(categories.size() - 1);
                description = getString(R.string.local_sorting_description, productName, categoryList, municipalityName);
            }
        }
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

    private void showLocalRuleDetails(MunicipalitySortingRuleDB.Rule rule, boolean useEnglish) {
        if (rule == null) return;
        String note = rule.noteFor(useEnglish);
        if (note != null && !note.trim().isEmpty()) {
            localRuleNote.setText(note.trim());
            localRuleNote.setVisibility(View.VISIBLE);
        }

        String source = rule.sourceURL;
        if (source == null || source.trim().isEmpty()) return;
        Uri uri = Uri.parse(source.trim());
        if (!("https".equalsIgnoreCase(uri.getScheme()) || "http".equalsIgnoreCase(uri.getScheme()))
                || uri.getHost() == null) return;
        String label = getString(R.string.local_rule_source);
        if (rule.lastChecked != null && !rule.lastChecked.trim().isEmpty()) {
            String checkedDate = rule.lastChecked;
            try {
                checkedDate = LocalDate.parse(checkedDate).format(
                        DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG)
                                .withLocale(useEnglish ? Locale.ENGLISH : new Locale("da", "DK")));
            } catch (RuntimeException ignored) {
                // Keep the source date as supplied if it is not ISO formatted.
            }
            label += "\n" + getString(R.string.local_rule_checked, checkedDate);
        }
        localRuleSource.setText(label);
        localRuleSource.setVisibility(View.VISIBLE);
        localRuleSource.setOnClickListener(view -> startActivity(new Intent(Intent.ACTION_VIEW, uri)));
    }

    private void showFallbackSorting(Map<String, String> sorting, String municipalityName, boolean useEnglish) {
        Set<String> categories = new LinkedHashSet<>();
        for (String key : sorting.keySet()) {
            String category = trashDB.toDanishSortingKey(key);
            if (!category.equals("Politistation") && !category.equals("Vask")
                    && !category.equals("Genbrugsplads") && !category.equals("Apotek")) {
                categories.add(category);
            }
        }
        if (categories.isEmpty()) return;

        fallbackSortingCard.setVisibility(View.VISIBLE);
        fallbackSortingToggle.setText(InfoPageText.get(requireContext(), "searchview.035"));
        fallbackSortingToggle.setOnClickListener(view -> fallbackSortingContent.setVisibility(
                fallbackSortingContent.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE));

        TextView introduction = fallbackText();
        String introKey = municipalityName == null || municipalityName.trim().isEmpty()
                ? "search.fallbackAdvice.general" : "search.fallbackAdvice.withMunicipality";
        String intro = InfoPageText.get(requireContext(), introKey);
        introduction.setText(intro.replace("{0}", municipalityName == null ? "" : municipalityName));
        fallbackSortingContent.addView(introduction);

        for (String category : categories) {
            TextView categoryName = fallbackText();
            categoryName.setTypeface(null, Typeface.BOLD);
            categoryName.setText(useEnglish ? trashDB.translateSortingKey(category) : category);
            categoryName.setPadding(0, dp(14), 0, dp(4));
            fallbackSortingContent.addView(categoryName);

            String alternative = category.equals("Madaffald") ? "Restaffald"
                    : category.equals("Restaffald") ? null : "Genbrugsplads";
            if (alternative != null) {
                LinearLayout pictograms = new LinearLayout(requireContext());
                pictograms.setOrientation(LinearLayout.HORIZONTAL);
                pictograms.setGravity(Gravity.CENTER_VERTICAL);
                pictograms.addView(fallbackPictogram(category, useEnglish));
                TextView arrow = fallbackText();
                arrow.setText(LanguageManager.isArabic(requireContext()) ? "←" : "→");
                arrow.setTextSize(18);
                arrow.setPadding(dp(8), 0, dp(8), 0);
                pictograms.addView(arrow);
                pictograms.addView(fallbackPictogram(alternative, useEnglish));
                fallbackSortingContent.addView(pictograms);
            }

            String adviceKey;
            switch (category) {
                case "Madaffald": adviceKey = "searchview.036"; break;
                case "Farligt affald": adviceKey = "searchview.037"; break;
                case "Tekstilaffald": adviceKey = "searchview.038"; break;
                case "Restaffald": adviceKey = "searchview.039"; break;
                case "Småt elektronik": adviceKey = "searchview.040"; break;
                case "Batterier": adviceKey = "searchview.041"; break;
                default: adviceKey = "searchview.043"; break;
            }
            String advice = InfoPageText.get(requireContext(), adviceKey);
            TextView adviceView = fallbackText();
            int colon = advice.indexOf(':');
            if (colon >= 0) {
                SpannableString styled = new SpannableString(advice);
                styled.setSpan(new StyleSpan(Typeface.BOLD), 0, colon + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                adviceView.setText(styled);
            } else {
                adviceView.setText(advice);
            }
            fallbackSortingContent.addView(adviceView);
        }
    }

    private TextView fallbackText() {
        TextView view = new TextView(requireContext());
        view.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_color));
        view.setTextSize(14);
        view.setLineSpacing(dp(3), 1f);
        return view;
    }

    private ImageView fallbackPictogram(String category, boolean useEnglish) {
        ImageView image = new ImageView(requireContext());
        image.setImageResource(TrashDB.getImageResourceForKey(category, useEnglish));
        image.setContentDescription(useEnglish ? trashDB.translateSortingKey(category) : category);
        image.setScaleType(ImageView.ScaleType.FIT_CENTER);
        image.setLayoutParams(new LinearLayout.LayoutParams(dp(40), dp(40)));
        return image;
    }

    private int dp(int size) {
        return Math.round(size * getResources().getDisplayMetrics().density);
    }

    private void showCorrectionForm(String productName, Map<String, String> sorting,
                                    String municipalityName, boolean useEnglish) {
        ScrollView scroll = new ScrollView(requireContext());
        LinearLayout fields = new LinearLayout(requireContext());
        fields.setOrientation(LinearLayout.VERTICAL);
        fields.setPadding(dp(20), dp(14), dp(20), dp(14));
        scroll.addView(fields);

        TextView explanation = correctionText(InfoPageText.get(requireContext(), "searchview.047"), false);
        fields.addView(explanation);
        fields.addView(correctionText(InfoPageText.get(requireContext(), "searchview.048") + ": " + productName, true));
        if (municipalityName != null && !municipalityName.trim().isEmpty()) {
            fields.addView(correctionText(InfoPageText.get(requireContext(), "searchview.049") + ": " + municipalityName, false));
        }

        fields.addView(correctionText(InfoPageText.get(requireContext(), "searchview.050"), true));
        StringBuilder currentSorting = new StringBuilder();
        for (Map.Entry<String, String> entry : sorting.entrySet()) {
            String danishCategory = trashDB.toDanishSortingKey(entry.getKey());
            if (currentSorting.length() > 0) currentSorting.append('\n');
            currentSorting.append(useEnglish ? trashDB.translateSortingKey(danishCategory) : danishCategory);
            if (entry.getValue() != null && !entry.getValue().trim().isEmpty()) {
                currentSorting.append(": ").append(entry.getValue().trim());
            }
        }
        fields.addView(correctionText(currentSorting.toString(), false));

        fields.addView(correctionText(InfoPageText.get(requireContext(), "searchview.051"), true));
        List<String> categoryLabels = new ArrayList<>();
        categoryLabels.add(InfoPageText.get(requireContext(), "searchview.053"));
        for (String category : CORRECTION_CATEGORIES) {
            categoryLabels.add(useEnglish ? trashDB.translateSortingKey(category) : category);
        }
        Spinner categories = new Spinner(requireContext());
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, categoryLabels);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        categories.setAdapter(adapter);
        categories.setMinimumHeight(dp(48));
        fields.addView(categories);

        fields.addView(correctionText(InfoPageText.get(requireContext(), "searchview.054"), true));
        EditText comment = new EditText(requireContext());
        comment.setHint(InfoPageText.get(requireContext(), "searchview.055"));
        comment.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        comment.setImeOptions(EditorInfo.IME_ACTION_DONE);
        comment.setMinLines(3);
        comment.setMaxLines(6);
        comment.setFilters(new InputFilter[]{new InputFilter.LengthFilter(1000)});
        comment.setOnEditorActionListener((textView, actionId, event) -> {
            if (actionId != EditorInfo.IME_ACTION_DONE
                    && (event == null || event.getKeyCode() != KeyEvent.KEYCODE_ENTER
                    || event.getAction() != KeyEvent.ACTION_DOWN)) return false;
            InputMethodManager keyboard = (InputMethodManager) requireContext()
                    .getSystemService(Context.INPUT_METHOD_SERVICE);
            if (keyboard != null) keyboard.hideSoftInputFromWindow(comment.getWindowToken(), 0);
            comment.clearFocus();
            return true;
        });
        fields.addView(comment);

        TextView error = correctionText(InfoPageText.get(requireContext(), "searchview.057"), false);
        error.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_red_dark));
        error.setVisibility(View.GONE);
        fields.addView(error);

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle(InfoPageText.get(requireContext(), "searchview.058"))
                .setView(scroll)
                .setNegativeButton(InfoPageText.get(requireContext(), "searchview.059"), null)
                .setPositiveButton(InfoPageText.get(requireContext(), "searchview.060"), null)
                .create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(button -> {
            int selectedIndex = categories.getSelectedItemPosition();
            if (selectedIndex <= 0) {
                Toast.makeText(requireContext(), InfoPageText.get(requireContext(), "searchview.052"), Toast.LENGTH_SHORT).show();
                return;
            }
            String category = CORRECTION_CATEGORIES[selectedIndex - 1];
            String categoryLabel = useEnglish ? trashDB.translateSortingKey(category) : category;
            String danishName = trashDB.getDanishProductName(productName, useEnglish);
            String locale = LanguageManager.getSavedLanguage(requireContext());
            String trimmedComment = comment.getText().toString().trim();
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setEnabled(false);
            dialog.setCancelable(false);
            error.setVisibility(View.GONE);
            new Thread(() -> {
                boolean sent = SortingCorrectionReporter.submit(productName, danishName,
                        currentSorting.toString(), category, categoryLabel, trimmedComment, locale);
                button.post(() -> {
                    if (!isAdded()) return;
                    if (sent) {
                        AchievementStore.get(requireContext()).recordCorrection();
                        dialog.dismiss();
                        new AlertDialog.Builder(requireContext())
                                .setTitle(InfoPageText.get(requireContext(), "searchview.044"))
                                .setMessage(InfoPageText.get(requireContext(), "searchview.045"))
                                .setPositiveButton(InfoPageText.get(requireContext(), "searchview.046"), null)
                                .show();
                    } else {
                        error.setVisibility(View.VISIBLE);
                        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setEnabled(true);
                        dialog.setCancelable(true);
                    }
                });
            }).start();
        }));
        dialog.show();
    }

    private TextView correctionText(String value, boolean heading) {
        TextView text = new TextView(requireContext());
        text.setText(value);
        text.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_color));
        text.setTextSize(heading ? 16 : 14);
        text.setLineSpacing(dp(3), 1f);
        text.setPadding(0, heading ? dp(14) : dp(6), 0, dp(3));
        if (heading) text.setTypeface(null, Typeface.BOLD);
        return text;
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
        List<String> suggestions = getSuccessfulSearchExamples(LanguageManager.usesNonDanishContent(requireContext()));
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

        String name = SavedMunicipalityManager.getSavedMunicipalityName(requireContext());
        selectedMunicipality = name == null ? null : findMunicipalityByName(name);
        boolean hasMunicipality = selectedMunicipality != null;
        selectedMunicipalityContent.setVisibility(hasMunicipality ? View.VISIBLE : View.GONE);
        chooseMunicipalityContent.setVisibility(hasMunicipality ? View.GONE : View.VISIBLE);
        savedMunicipalityContainer.setVisibility(showingSearchStart ? View.VISIBLE : View.GONE);
        if (!hasMunicipality) {
            nearestRecyclingCenter = null;
            savedMunicipalityContainer.findViewById(R.id.chooseMunicipalityButton)
                    .setOnClickListener(view -> Navigation.findNavController(view).navigate(R.id.fragment_municipalities));
            return;
        }

        savedMunicipalityName.setText(selectedMunicipality.getMunicipality());
        municipalityCardHeader.setOnClickListener(view -> openMunicipalityDetails(view, false));
        Municipality.Details details = selectedMunicipality.getDetails();
        Integer count = details == null ? null : details.getWasteFractionCount();
        Integer total = details == null ? null : details.getWasteFractionTotal();
        municipalityFractionsRow.setVisibility(count != null && total != null ? View.VISIBLE : View.GONE);
        if (count != null && total != null) {
            municipalityFractionsValue.setText(getString(R.string.uses_fractions, count, total));
        }
        municipalityFractionsRow.setOnClickListener(view -> openMunicipalityDetails(view, true));

        String operator = details == null ? null : details.getWasteOperator(LanguageManager.usesNonDanishContent(requireContext()));
        municipalityOperatorRow.setVisibility(operator == null || operator.trim().isEmpty() ? View.GONE : View.VISIBLE);
        municipalityOperatorValue.setText(operator);
        municipalityOperatorRow.setOnClickListener(view -> {
            String url = details == null ? null : details.getWasteOperatorUrl();
            if (url == null || !(url.startsWith("https://") || url.startsWith("http://"))) {
                openMunicipalityDetails(view, false);
                return;
            }
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        });

        municipalityNearestRow.setOnClickListener(view -> {
            if (nearestRecyclingCenter != null) {
                openRecyclingCenterInMaps(nearestRecyclingCenter);
            } else {
                requestNearestRecyclingCenter();
            }
        });
        updateNearestRecyclingCenterFromLastLocation();
    }

    private void showStartContent(boolean show) {
        showingSearchStart = show;
        if (savedMunicipalityContainer != null) savedMunicipalityContainer.setVisibility(show ? View.VISIBLE : View.GONE);
        if (searchIntroContainer != null) searchIntroContainer.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    private static String localResultItemName(String name) {
        if (name == null || name.isEmpty()) return "";
        int uppercaseCount = 0;
        for (int offset = 0; offset < name.length(); ) {
            int codePoint = name.codePointAt(offset);
            if (Character.isUpperCase(codePoint) && ++uppercaseCount > 1) return name;
            offset += Character.charCount(codePoint);
        }
        int firstCodePointEnd = Character.charCount(name.codePointAt(0));
        return name.substring(0, firstCodePointEnd).toLowerCase(Locale.ROOT)
                + name.substring(firstCodePointEnd);
    }

    private void openPhotoSearch(Uri uri) {
        if (uri == null || !isAdded() || getView() == null) return;
        Bundle args = new Bundle();
        args.putString("photoUri", uri.toString());
        Navigation.findNavController(getView()).navigate(R.id.fragment_photo_search, args);
    }

    private void openMunicipalityDetails(View view, boolean scrollToFractions) {
        if (selectedMunicipality == null) return;
        Bundle args = new Bundle();
        args.putParcelable("municipality", selectedMunicipality);
        args.putBoolean("scrollToFractions", scrollToFractions);
        Navigation.findNavController(view).navigate(R.id.fragment_municipality_details, args);
    }

    private void updateNearestRecyclingCenterFromLastLocation() {
        nearestRecyclingCenter = null;
        municipalityNearestValue.setText(R.string.location_to_find_centre);
        if (selectedMunicipality == null || ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return;
        LocationManager manager = (LocationManager) requireContext().getSystemService(Context.LOCATION_SERVICE);
        Location latest = null;
        try {
            for (String provider : manager.getProviders(true)) {
                Location candidate = manager.getLastKnownLocation(provider);
                if (candidate != null && (latest == null || candidate.getTime() > latest.getTime())) latest = candidate;
            }
        } catch (SecurityException error) {
            Log.w(TAG, "Location permission changed", error);
        }
        if (latest != null) showNearestRecyclingCenter(latest);
    }

    private void requestNearestRecyclingCenter() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
            return;
        }
        LocationManager manager = (LocationManager) requireContext().getSystemService(Context.LOCATION_SERVICE);
        updateNearestRecyclingCenterFromLastLocation();
        if (nearestRecyclingCenter != null) return;
        municipalityNearestValue.setText(R.string.finding_nearest_centre);
        try {
            String provider = manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
                    ? LocationManager.NETWORK_PROVIDER : LocationManager.GPS_PROVIDER;
            if (!manager.isProviderEnabled(provider)) {
                municipalityNearestValue.setText(R.string.nearest_centre_unavailable);
                return;
            }
            manager.requestSingleUpdate(provider, new LocationListener() {
                @Override public void onLocationChanged(@NonNull Location location) {
                    if (isAdded()) showNearestRecyclingCenter(location);
                }
            }, null);
        } catch (SecurityException error) {
            municipalityNearestValue.setText(R.string.location_to_find_centre);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE && grantResults.length > 0) {
            for (int result : grantResults) {
                if (result == PackageManager.PERMISSION_GRANTED) {
                    requestNearestRecyclingCenter();
                    return;
                }
            }
            municipalityNearestValue.setText(R.string.location_to_find_centre);
        }
    }

    private void showNearestRecyclingCenter(Location location) {
        if (selectedMunicipality == null) return;
        float closestDistance = Float.MAX_VALUE;
        RecyclingCenter closest = null;
        try (InputStream stream = getResources().openRawResource(R.raw.genbrugspladser_data);
             InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            Type type = new TypeToken<List<RecyclingCenter>>() {}.getType();
            List<RecyclingCenter> centers = new Gson().fromJson(reader, type);
            for (RecyclingCenter center : centers) {
                if (!selectedMunicipality.getMunicipality().equals(center.municipality)
                        || center.latitude == null || center.longitude == null) continue;
                float[] result = new float[1];
                Location.distanceBetween(location.getLatitude(), location.getLongitude(), center.latitude, center.longitude, result);
                if (result[0] < closestDistance) {
                    closestDistance = result[0];
                    closest = center;
                }
            }
        } catch (Exception error) {
            Log.w(TAG, "Could not read recycling centers", error);
        }
        nearestRecyclingCenter = closest;
        municipalityNearestValue.setText(closest == null ? getString(R.string.nearest_centre_unavailable)
                : closest.name + " · " + String.format(Locale.getDefault(), "%.1f km", closestDistance / 1000f));
    }

    private void openRecyclingCenterInMaps(RecyclingCenter center) {
        Uri destination = Uri.parse("geo:" + center.latitude + "," + center.longitude + "?q="
                + Uri.encode(center.latitude + "," + center.longitude + "(" + center.name + ")"));
        startActivity(Intent.createChooser(new Intent(Intent.ACTION_VIEW, destination), getString(R.string.show_on_map)));
        AchievementStore.get(requireContext()).recordRecyclingCenterMap(
                center.municipality + ":" + center.name);
    }

    private static class RecyclingCenter {
        @SerializedName("Navn") String name;
        @SerializedName("Kommune") String municipality;
        @SerializedName("lat") Double latitude;
        @SerializedName("lng") Double longitude;
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
        boolean useEnglish = LanguageManager.usesNonDanishContent(requireContext());
        if (productName.equalsIgnoreCase(getString(R.string.example_pizzabakke))) {
            return trashDB.getImageResourceForKey("Mad- og drikkekartoner", useEnglish);
        }
        if (productName.equalsIgnoreCase(getString(R.string.example_batteri))) {
            return trashDB.getImageResourceForKey("Batterier", useEnglish);
        }
        if (productName.equalsIgnoreCase(getString(R.string.example_kaffefilter))) {
            return trashDB.getImageResourceForKey("Madaffald", useEnglish);
        }
        if (productName.equalsIgnoreCase(getString(R.string.example_glas))) {
            return trashDB.getImageResourceForKey("Glas", useEnglish);
        }

        String sortingKey = trashDB.getFirstSortingKeyForProduct(productName, useEnglish);
        if (useEnglish) {
            sortingKey = trashDB.translateSortingKey(sortingKey);
        }

        return trashDB.getImageResourceForKey(sortingKey, useEnglish);
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
        return SUCCESSFUL_SEARCH_EXAMPLES_KEY_PREFIX + LanguageManager.getSavedLanguage(requireContext());
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
