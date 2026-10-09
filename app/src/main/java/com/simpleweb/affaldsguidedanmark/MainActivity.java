package com.simpleweb.affaldsguidedanmark;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.text.Editable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextWatcher;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.google.android.gms.ads.MobileAds;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.snackbar.BaseTransientBottomBar;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private NavController navController;
    private Button begynd;
    private DrawerLayout drawerLayout;
    private BottomNavigationView bottomNavigationView;
    private NavigationView navigationView;
    private SharedPreferences sharedPref;
    private PopupWindow greetingMunicipalitySuggestionsPopup;
    private int greetingOnboardingStep = 0;
    private int lastSelectedBottomNavigationItemId = R.id.search_trash_button;
    private boolean isUpdatingBottomNavigationSelection = false;
    private boolean achievementBannerShowing = false;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LanguageManager.wrapContext(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        LanguageManager.applySavedLanguage(this);
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            getWindow().setNavigationBarContrastEnforced(false);
        }

        MobileAds.initialize(this);

        sharedPref = getSharedPreferences("MyAppPreferences", Context.MODE_PRIVATE);
        boolean hasSeenGreetingPage = sharedPref.getBoolean("hasSeenGreetingPage", false);

        if (!hasSeenGreetingPage) {
            setContentView(R.layout.activity_greeting);
            applySystemBarInsets(findViewById(R.id.greeting_layout));
            setUpGreetingLayout();
        } else {
            setContentView(R.layout.activity_main);
            applyMainSystemBarInsets();
            setUpMainLayout();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        AchievementStore store = AchievementStore.get(this);
        store.setUnlockListener(this::showPendingAchievement);
        store.recordAppUse();
        showPendingAchievement();
    }

    @Override
    protected void onPause() {
        AchievementStore.get(this).setUnlockListener(null);
        super.onPause();
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            hideKeyboardWhenTouchingOutsideInput(event);
        }

        return super.dispatchTouchEvent(event);
    }

    private void hideKeyboardWhenTouchingOutsideInput(MotionEvent event) {
        View focusedView = getCurrentFocus();
        if (!(focusedView instanceof EditText)) {
            return;
        }

        Rect focusedViewBounds = new Rect();
        focusedView.getGlobalVisibleRect(focusedViewBounds);
        if (focusedViewBounds.contains((int) event.getRawX(), (int) event.getRawY())) {
            return;
        }

        InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (inputMethodManager != null) {
            inputMethodManager.hideSoftInputFromWindow(focusedView.getWindowToken(), 0);
        }
    }

    private void applySystemBarInsets(View rootView) {
        int initialLeft = rootView.getPaddingLeft();
        int initialTop = rootView.getPaddingTop();
        int initialRight = rootView.getPaddingRight();
        int initialBottom = rootView.getPaddingBottom();

        ViewCompat.setOnApplyWindowInsetsListener(rootView, (view, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(
                    initialLeft + insets.left,
                    initialTop + insets.top,
                    initialRight + insets.right,
                    initialBottom + insets.bottom
            );
            return windowInsets;
        });
        ViewCompat.requestApplyInsets(rootView);
    }

    private void applyMainSystemBarInsets() {
        View navHost = findViewById(R.id.nav_host_fragment);
        View bottomNavigation = findViewById(R.id.bottom_navigation);
        View navigationDrawer = findViewById(R.id.navigation_view);
        View mainContentRoot = findViewById(R.id.main_content_root);

        int navHostInitialLeft = navHost.getPaddingLeft();
        int navHostInitialTop = navHost.getPaddingTop();
        int navHostInitialRight = navHost.getPaddingRight();
        int navHostInitialBottom = navHost.getPaddingBottom();

        int bottomNavInitialLeft = bottomNavigation.getPaddingLeft();
        int bottomNavInitialTop = bottomNavigation.getPaddingTop();
        int bottomNavInitialRight = bottomNavigation.getPaddingRight();
        int bottomNavInitialBottom = bottomNavigation.getPaddingBottom();

        int drawerInitialLeft = navigationDrawer.getPaddingLeft();
        int drawerInitialTop = navigationDrawer.getPaddingTop();
        int drawerInitialRight = navigationDrawer.getPaddingRight();
        int drawerInitialBottom = navigationDrawer.getPaddingBottom();

        ViewCompat.setOnApplyWindowInsetsListener(mainContentRoot, (view, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());

            navHost.setPadding(
                    navHostInitialLeft + insets.left,
                    navHostInitialTop + insets.top,
                    navHostInitialRight + insets.right,
                    navHostInitialBottom
            );

            bottomNavigation.setPadding(
                    bottomNavInitialLeft + insets.left,
                    bottomNavInitialTop,
                    bottomNavInitialRight + insets.right,
                    bottomNavInitialBottom + insets.bottom
            );

            navigationDrawer.setPadding(
                    drawerInitialLeft,
                    drawerInitialTop + insets.top,
                    drawerInitialRight + insets.right,
                    drawerInitialBottom + insets.bottom
            );

            return windowInsets;
        });
        ViewCompat.requestApplyInsets(mainContentRoot);
    }

    private void setUpGreetingLayout() {
        View welcomeStep = findViewById(R.id.onboardingWelcomeStep);
        View languageStep = findViewById(R.id.onboardingLanguageStep);
        View municipalityStep = findViewById(R.id.onboardingMunicipalityStep);
        View onboardingDotOne = findViewById(R.id.onboardingDotOne);
        View onboardingDotTwo = findViewById(R.id.onboardingDotTwo);
        View onboardingDotThree = findViewById(R.id.onboardingDotThree);
        TextView backButton = findViewById(R.id.onboardingBackButton);
        TextView skipButton = findViewById(R.id.onboardingSkipButton);
        RadioGroup languageRadioGroup = findViewById(R.id.languageRadioGroup);
        RadioButton danishLanguageOption = findViewById(R.id.danishLanguageOption);
        RadioButton englishLanguageOption = findViewById(R.id.englishLanguageOption);
        RadioButton arabicLanguageOption = findViewById(R.id.arabicLanguageOption);
        AutoCompleteTextView municipalityInput = findViewById(R.id.greetingMunicipalityInput);
        TextView selectedMunicipalityText = findViewById(R.id.greetingSelectedMunicipality);
        setUpGreetingMunicipalityPicker(municipalityInput, selectedMunicipalityText);

        if (LanguageManager.isArabic(this)) {
            arabicLanguageOption.setChecked(true);
        } else if (LanguageManager.isEnglish(this)) {
            englishLanguageOption.setChecked(true);
        } else {
            danishLanguageOption.setChecked(true);
        }

        languageRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.englishLanguageOption) {
                LanguageManager.saveLanguage(this, LanguageManager.ENGLISH);
                sharedPref.edit().putInt("greetingOnboardingStep", 1).apply();
                recreate();
            } else if (checkedId == R.id.arabicLanguageOption) {
                LanguageManager.saveLanguage(this, LanguageManager.ARABIC);
                sharedPref.edit().putInt("greetingOnboardingStep", 1).apply();
                recreate();
            } else if (checkedId == R.id.danishLanguageOption) {
                LanguageManager.saveLanguage(this, LanguageManager.DANISH);
                sharedPref.edit().putInt("greetingOnboardingStep", 1).apply();
                recreate();
            }
        });

        begynd = findViewById(R.id.begynd);
        greetingOnboardingStep = Math.max(0, Math.min(2, sharedPref.getInt("greetingOnboardingStep", 0)));
        showGreetingOnboardingStep(
                greetingOnboardingStep,
                welcomeStep,
                languageStep,
                municipalityStep,
                onboardingDotOne,
                onboardingDotTwo,
                onboardingDotThree,
                backButton,
                skipButton,
                begynd
        );

        begynd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (greetingOnboardingStep < 2) {
                    greetingOnboardingStep++;
                    sharedPref.edit().putInt("greetingOnboardingStep", greetingOnboardingStep).apply();
                    showGreetingOnboardingStep(
                            greetingOnboardingStep,
                            welcomeStep,
                            languageStep,
                            municipalityStep,
                            onboardingDotOne,
                            onboardingDotTwo,
                            onboardingDotThree,
                            backButton,
                            skipButton,
                            begynd
                    );
                } else {
                    finishGreetingOnboarding(municipalityInput, true);
                }
            }
        });

        backButton.setOnClickListener(view -> {
            if (greetingOnboardingStep == 0) {
                return;
            }

            greetingOnboardingStep--;
            sharedPref.edit().putInt("greetingOnboardingStep", greetingOnboardingStep).apply();
            showGreetingOnboardingStep(
                    greetingOnboardingStep,
                    welcomeStep,
                    languageStep,
                    municipalityStep,
                    onboardingDotOne,
                    onboardingDotTwo,
                    onboardingDotThree,
                    backButton,
                    skipButton,
                    begynd
            );
        });

        skipButton.setOnClickListener(view -> finishGreetingOnboarding(municipalityInput, false));
    }

    private void showGreetingOnboardingStep(
            int step,
            View welcomeStep,
            View languageStep,
            View municipalityStep,
            View onboardingDotOne,
            View onboardingDotTwo,
            View onboardingDotThree,
            TextView backButton,
            TextView skipButton,
            Button continueButton
    ) {
        dismissGreetingMunicipalitySuggestions();

        welcomeStep.setVisibility(step == 0 ? View.VISIBLE : View.GONE);
        languageStep.setVisibility(step == 1 ? View.VISIBLE : View.GONE);
        municipalityStep.setVisibility(step == 2 ? View.VISIBLE : View.GONE);

        onboardingDotOne.setBackgroundResource(step == 0 ? R.drawable.onboarding_dot_active : R.drawable.onboarding_dot_inactive);
        onboardingDotTwo.setBackgroundResource(step == 1 ? R.drawable.onboarding_dot_active : R.drawable.onboarding_dot_inactive);
        onboardingDotThree.setBackgroundResource(step == 2 ? R.drawable.onboarding_dot_active : R.drawable.onboarding_dot_inactive);

        backButton.setVisibility(step == 0 ? View.INVISIBLE : View.VISIBLE);
        skipButton.setVisibility(step == 2 ? View.VISIBLE : View.GONE);
        continueButton.setText(step == 2 ? R.string.begynd_med_det_samme : R.string.onboarding_continue);
    }

    private void finishGreetingOnboarding(AutoCompleteTextView municipalityInput, boolean shouldSaveMunicipality) {
        dismissGreetingMunicipalitySuggestions();
        if (shouldSaveMunicipality) {
            saveGreetingMunicipalityIfSelected(municipalityInput);
        }

        sharedPref.edit()
                .putBoolean("hasSeenGreetingPage", true)
                .remove("greetingOnboardingStep")
                .apply();
        setContentView(R.layout.activity_main);
        applyMainSystemBarInsets();
        setUpMainLayout();
    }

    private void setUpGreetingMunicipalityPicker(AutoCompleteTextView municipalityInput, TextView selectedMunicipalityText) {
        MunicipalityDB municipalityDB = new MunicipalityDB(getResources());
        List<Municipality> municipalities = municipalityDB.getMunicipalities();

        municipalityInput.setAdapter(null);

        String savedMunicipalityName = SavedMunicipalityManager.getSavedMunicipalityName(this);
        if (!savedMunicipalityName.isEmpty()) {
            municipalityInput.setText(savedMunicipalityName, false);
            showSelectedMunicipality(selectedMunicipalityText, savedMunicipalityName);
        }

        municipalityInput.setOnFocusChangeListener((view, hasFocus) -> {
            if (hasFocus) {
                renderGreetingMunicipalitySuggestions(
                        municipalityInput.getText().toString(),
                        municipalities,
                        municipalityInput,
                        selectedMunicipalityText
                );
            } else {
                dismissGreetingMunicipalitySuggestions();
            }
        });
        municipalityInput.setOnClickListener(view -> {
            renderGreetingMunicipalitySuggestions(
                    municipalityInput.getText().toString(),
                    municipalities,
                    municipalityInput,
                    selectedMunicipalityText
            );
        });
        municipalityInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (!isExactMunicipalityName(s.toString(), municipalities)) {
                    selectedMunicipalityText.setVisibility(View.GONE);
                }

                renderGreetingMunicipalitySuggestions(
                        s.toString(),
                        municipalities,
                        municipalityInput,
                        selectedMunicipalityText
                );
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    private void renderGreetingMunicipalitySuggestions(
            String query,
            List<Municipality> municipalities,
            AutoCompleteTextView municipalityInput,
            TextView selectedMunicipalityText
    ) {
        if (!municipalityInput.hasFocus() || query.trim().isEmpty()) {
            dismissGreetingMunicipalitySuggestions();
            return;
        }

        List<Municipality> suggestions = findGreetingMunicipalitySuggestions(query, municipalities);
        if (suggestions.isEmpty()) {
            dismissGreetingMunicipalitySuggestions();
            return;
        }

        LinearLayout suggestionsLayout = new LinearLayout(this);
        suggestionsLayout.setOrientation(LinearLayout.VERTICAL);
        suggestionsLayout.setBackgroundResource(R.drawable.autocomplete_dropdown_background);

        for (Municipality suggestion : suggestions) {
            LinearLayout suggestionView = new LinearLayout(this);
            suggestionView.setOrientation(LinearLayout.VERTICAL);
            suggestionView.setPadding(dpToPx(14), dpToPx(8), dpToPx(14), dpToPx(8));

            TextView municipalityNameView = new TextView(this);
            municipalityNameView.setText(suggestion.getMunicipality());
            municipalityNameView.setTextColor(ContextCompat.getColor(this, R.color.text_color));
            municipalityNameView.setTextSize(14);
            suggestionView.addView(municipalityNameView);

            String alias = findMatchingPostalPlaceLabel(suggestion, query);
            if (alias != null) {
                TextView aliasView = new TextView(this);
                aliasView.setText(alias);
                aliasView.setTextColor(ContextCompat.getColor(this, R.color.grey_black));
                aliasView.setTextSize(12);
                aliasView.setPadding(0, dpToPx(2), 0, 0);
                suggestionView.addView(aliasView);
            }

            suggestionView.setOnClickListener(view -> {
                municipalityInput.setText(suggestion.getMunicipality(), false);
                municipalityInput.setSelection(municipalityInput.length());
                showSelectedMunicipality(selectedMunicipalityText, suggestion.getMunicipality());
                dismissGreetingMunicipalitySuggestions();

                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.hideSoftInputFromWindow(municipalityInput.getWindowToken(), 0);
                }
            });
            suggestionsLayout.addView(suggestionView);
        }

        dismissGreetingMunicipalitySuggestions();

        int popupHeight = (suggestions.size() * dpToPx(54)) + dpToPx(8);
        greetingMunicipalitySuggestionsPopup = new PopupWindow(
                suggestionsLayout,
                municipalityInput.getWidth(),
                LinearLayout.LayoutParams.WRAP_CONTENT,
                false
        );
        greetingMunicipalitySuggestionsPopup.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        greetingMunicipalitySuggestionsPopup.setOutsideTouchable(true);
        greetingMunicipalitySuggestionsPopup.setElevation(dpToPx(4));
        greetingMunicipalitySuggestionsPopup.showAsDropDown(
                municipalityInput,
                0,
                -municipalityInput.getHeight() - popupHeight - dpToPx(4)
        );
    }

    private void dismissGreetingMunicipalitySuggestions() {
        if (greetingMunicipalitySuggestionsPopup != null && greetingMunicipalitySuggestionsPopup.isShowing()) {
            greetingMunicipalitySuggestionsPopup.dismiss();
        }
        greetingMunicipalitySuggestionsPopup = null;
    }

    private List<Municipality> findGreetingMunicipalitySuggestions(String query, List<Municipality> municipalities) {
        String normalizedQuery = normalizeMunicipalityQuery(query);
        List<Municipality> suggestions = new ArrayList<>();

        if (normalizedQuery.isEmpty()) {
            return suggestions;
        }

        for (Municipality municipality : municipalities) {
            if (municipalityMatchesQuery(municipality, normalizedQuery)) {
                suggestions.add(municipality);
                if (suggestions.size() == 4) {
                    break;
                }
            }
        }

        return suggestions;
    }

    private boolean municipalityMatchesQuery(Municipality municipality, String normalizedQuery) {
        if (normalizeMunicipalityQuery(municipality.getMunicipality()).contains(normalizedQuery)) {
            return true;
        }

        if (municipality.getPostalPlaces() == null) {
            return false;
        }

        for (Municipality.PostalPlace postalPlace : municipality.getPostalPlaces()) {
            if (normalizeMunicipalityQuery(postalPlace.getDisplayName()).contains(normalizedQuery)) {
                return true;
            }
        }

        return false;
    }

    private String findMatchingPostalPlaceLabel(Municipality municipality, String query) {
        String normalizedQuery = normalizeMunicipalityQuery(query);
        if (normalizedQuery.isEmpty()
                || normalizeMunicipalityQuery(municipality.getMunicipality()).contains(normalizedQuery)
                || municipality.getPostalPlaces() == null) {
            return null;
        }

        for (Municipality.PostalPlace postalPlace : municipality.getPostalPlaces()) {
            if (normalizeMunicipalityQuery(postalPlace.getDisplayName()).contains(normalizedQuery)) {
                return postalPlace.getDisplayName();
            }
        }

        return null;
    }

    private void showSelectedMunicipality(TextView selectedMunicipalityText, String municipalityName) {
        selectedMunicipalityText.setText(getString(R.string.greeting_municipality_selected, municipalityName));
        selectedMunicipalityText.setVisibility(View.VISIBLE);
    }

    private boolean isExactMunicipalityName(String value, List<Municipality> municipalities) {
        for (Municipality municipality : municipalities) {
            if (municipality.getMunicipality().equalsIgnoreCase(value.trim())) {
                return true;
            }
        }

        return false;
    }

    private void saveGreetingMunicipalityIfSelected(AutoCompleteTextView municipalityInput) {
        String selectedMunicipality = municipalityInput.getText().toString().trim();
        if (selectedMunicipality.isEmpty()) {
            return;
        }

        MunicipalityDB municipalityDB = new MunicipalityDB(getResources());
        for (Municipality municipality : municipalityDB.getMunicipalities()) {
            if (municipality.getMunicipality().equalsIgnoreCase(selectedMunicipality)) {
                SavedMunicipalityManager.save(this, municipality.getMunicipality());
                return;
            }
        }
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    private void setUpMainLayout() {

        navController = Navigation.findNavController(this, R.id.nav_host_fragment);

        drawerLayout = findViewById(R.id.main_layout);
        showPendingAchievement();

        bottomNavigationView = findViewById(R.id.bottom_navigation);

        bottomNavigationView.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                if (isUpdatingBottomNavigationSelection) {
                    return true;
                }

                int itemId = item.getItemId();

                if (itemId == R.id.search_trash_button) {
                    navController.navigate(R.id.action_to_searchTrash);
                } else if (itemId == R.id.trash_types_button) {
                    navController.navigate(R.id.fragment_trash_types);
                } else if (itemId == R.id.municipalities_button) {
                    navController.navigate(R.id.fragment_municipalities);
                } else if (itemId == R.id.navigation_view_button) {
                    if (drawerLayout.isDrawerOpen(GravityCompat.END)) {
                        drawerLayout.closeDrawer(GravityCompat.END);
                    } else {
                        drawerLayout.openDrawer(GravityCompat.END);
                    }
                }
                return true;
            }
        });

        navigationView = findViewById(R.id.navigation_view);
        navigationView.getMenu().findItem(R.id.achievements)
                .setTitle(InfoPageText.get(this, "achievements.page"));

        navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
            updateBottomNavigationSelectionForDestination(destination.getId());
        });

        drawerLayout.addDrawerListener(new DrawerLayout.SimpleDrawerListener() {
            @Override
            public void onDrawerClosed(@NonNull View drawerView) {
                if (navController.getCurrentDestination() != null) {
                    updateBottomNavigationSelectionForDestination(navController.getCurrentDestination().getId());
                } else {
                    setBottomNavigationSelection(lastSelectedBottomNavigationItemId);
                }
            }
        });

        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int itemId = item.getItemId();

                if (itemId == R.id.om) {
                    navController.navigate(R.id.fragment_about);
                    drawerLayout.closeDrawer(GravityCompat.END);
                    return true;
                } else if (itemId == R.id.kontaktinformation) {
                    navController.navigate(R.id.fragment_contact_information);
                    drawerLayout.closeDrawer(GravityCompat.END);
                    return true;
                } else if (itemId == R.id.seneste_soegninger) {
                    navController.navigate(R.id.fragment_recent_searches);
                    drawerLayout.closeDrawer(GravityCompat.END);
                    return true;
                } else if (itemId == R.id.sprog) {
                    navController.navigate(R.id.fragment_language);
                    drawerLayout.closeDrawer(GravityCompat.END);
                    return true;
                } else if (itemId == R.id.achievements) {
                    navController.navigate(R.id.fragment_achievements);
                    drawerLayout.closeDrawer(GravityCompat.END);
                    return true;
                } else if (itemId == R.id.privatlivspolitik) {
                    navController.navigate(R.id.fragment_privacy_policy);
                    drawerLayout.closeDrawer(GravityCompat.END);
                    return true;
                } else if (itemId == R.id.vilkårogbetingelser) {
                    navController.navigate(R.id.fragment_terms_of_service);
                    drawerLayout.closeDrawer(GravityCompat.END);
                    return true;
                }
                return false;
            }
        });
    }

    private void showPendingAchievement() {
        if (achievementBannerShowing || navController == null) return;
        View root = findViewById(R.id.main_content_root);
        if (root == null) return;
        AchievementStore store = AchievementStore.get(this);
        String id = store.takePendingUnlock();
        if (id == null) return;
        achievementBannerShowing = true;
        String title = InfoPageText.get(this, "achievements." + id + ".title");
        String detail = InfoPageText.get(this, "achievements." + id + ".detail");
        String heading = InfoPageText.get(this, "achievements.unlocked") + ": " + title;
        SpannableStringBuilder message = new SpannableStringBuilder(heading + "\n" + detail);
        message.setSpan(new StyleSpan(Typeface.BOLD), 0, heading.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        message.setSpan(new RelativeSizeSpan(1.12f), 0, heading.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        Snackbar banner = Snackbar.make(root, message, 7000);
        banner.getView().setBackgroundResource(R.drawable.achievement_banner_background);
        banner.getView().setElevation(getResources().getDisplayMetrics().density * 8);
        banner.setTextColor(ContextCompat.getColor(this, R.color.white));
        TextView bannerText = banner.getView().findViewById(com.google.android.material.R.id.snackbar_text);
        if (bannerText != null) {
            bannerText.setMaxLines(4);
            Drawable trophy = ContextCompat.getDrawable(this, R.drawable.achievement_trophy);
            bannerText.setCompoundDrawablesRelativeWithIntrinsicBounds(trophy, null, null, null);
            bannerText.setCompoundDrawablePadding(Math.round(getResources().getDisplayMetrics().density * 12));
        }
        View bottomNav = findViewById(R.id.bottom_navigation);
        if (bottomNav != null) banner.setAnchorView(bottomNav);
        banner.getView().setOnClickListener(view -> {
            Bundle args = new Bundle();
            args.putString("highlightAchievement", id);
            navController.navigate(R.id.fragment_achievements, args);
            banner.dismiss();
        });
        banner.addCallback(new BaseTransientBottomBar.BaseCallback<Snackbar>() {
            @Override
            public void onDismissed(Snackbar transientBottomBar, int event) {
                achievementBannerShowing = false;
                showPendingAchievement();
            }
        });
        banner.show();
        Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator != null && vibrator.hasVibrator()) {
            vibrator.vibrate(VibrationEffect.createOneShot(35, VibrationEffect.DEFAULT_AMPLITUDE));
        }
    }

    private void updateBottomNavigationSelectionForDestination(int destinationId) {
        int bottomNavigationItemId = getBottomNavigationItemIdForDestination(destinationId);
        setBottomNavigationSelection(bottomNavigationItemId);

        if (bottomNavigationItemId != R.id.navigation_view_button) {
            lastSelectedBottomNavigationItemId = bottomNavigationItemId;
        }
    }

    private int getBottomNavigationItemIdForDestination(int destinationId) {
        if (destinationId == R.id.fragment_trash_types || destinationId == R.id.fragment_trash_type_details) {
            return R.id.trash_types_button;
        }

        if (destinationId == R.id.fragment_municipalities || destinationId == R.id.fragment_municipality_details) {
            return R.id.municipalities_button;
        }

        if (destinationId == R.id.fragment_recent_searches
                || destinationId == R.id.fragment_language
                || destinationId == R.id.fragment_about
                || destinationId == R.id.fragment_achievements
                || destinationId == R.id.fragment_contact_information
                || destinationId == R.id.fragment_privacy_policy
                || destinationId == R.id.fragment_terms_of_service) {
            return R.id.navigation_view_button;
        }

        return R.id.search_trash_button;
    }

    private void setBottomNavigationSelection(int itemId) {
        if (bottomNavigationView == null || bottomNavigationView.getSelectedItemId() == itemId) {
            return;
        }

        isUpdatingBottomNavigationSelection = true;
        bottomNavigationView.setSelectedItemId(itemId);
        isUpdatingBottomNavigationSelection = false;
    }

    private static String normalizeMunicipalityQuery(String value) {
        return value == null
                ? ""
                : value.toLowerCase(new Locale("da", "DK"))
                .replace("æ", "ae")
                .replace("ø", "oe")
                .replace("å", "aa")
                .replaceAll("[^a-z0-9]+", "");
    }
}
