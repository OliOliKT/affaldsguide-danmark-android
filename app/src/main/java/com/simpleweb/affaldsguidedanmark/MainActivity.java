package com.simpleweb.affaldsguidedanmark;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
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
            applySystemBarInsets(findViewById(R.id.main_layout));
            setUpMainLayout();
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
        AutoCompleteTextView municipalityInput = findViewById(R.id.greetingMunicipalityInput);
        TextView selectedMunicipalityText = findViewById(R.id.greetingSelectedMunicipality);
        setUpGreetingMunicipalityPicker(municipalityInput, selectedMunicipalityText);

        if (LanguageManager.isEnglish(this)) {
            englishLanguageOption.setChecked(true);
        } else {
            danishLanguageOption.setChecked(true);
        }

        languageRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.englishLanguageOption) {
                LanguageManager.saveLanguage(this, LanguageManager.ENGLISH);
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
        applySystemBarInsets(findViewById(R.id.main_layout));
        setUpMainLayout();
    }

    private void setUpGreetingMunicipalityPicker(AutoCompleteTextView municipalityInput, TextView selectedMunicipalityText) {
        MunicipalityDB municipalityDB = new MunicipalityDB(getResources());
        List<String> municipalityNames = new ArrayList<>();
        for (Municipality municipality : municipalityDB.getMunicipalities()) {
            municipalityNames.add(municipality.getMunicipality());
        }

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
                        municipalityNames,
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
                    municipalityNames,
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
                if (!isExactMunicipalityName(s.toString(), municipalityNames)) {
                    selectedMunicipalityText.setVisibility(View.GONE);
                }

                renderGreetingMunicipalitySuggestions(
                        s.toString(),
                        municipalityNames,
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
            List<String> municipalityNames,
            AutoCompleteTextView municipalityInput,
            TextView selectedMunicipalityText
    ) {
        if (!municipalityInput.hasFocus() || query.trim().isEmpty()) {
            dismissGreetingMunicipalitySuggestions();
            return;
        }

        List<String> suggestions = findGreetingMunicipalitySuggestions(query, municipalityNames);
        if (suggestions.isEmpty()) {
            dismissGreetingMunicipalitySuggestions();
            return;
        }

        LinearLayout suggestionsLayout = new LinearLayout(this);
        suggestionsLayout.setOrientation(LinearLayout.VERTICAL);
        suggestionsLayout.setBackgroundResource(R.drawable.autocomplete_dropdown_background);

        for (String suggestion : suggestions) {
            TextView suggestionView = new TextView(this);
            suggestionView.setText(suggestion);
            suggestionView.setTextColor(ContextCompat.getColor(this, R.color.text_color));
            suggestionView.setTextSize(14);
            suggestionView.setPadding(dpToPx(14), dpToPx(10), dpToPx(14), dpToPx(10));
            suggestionView.setOnClickListener(view -> {
                municipalityInput.setText(suggestion, false);
                municipalityInput.setSelection(municipalityInput.length());
                showSelectedMunicipality(selectedMunicipalityText, suggestion);
                dismissGreetingMunicipalitySuggestions();

                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.hideSoftInputFromWindow(municipalityInput.getWindowToken(), 0);
                }
            });
            suggestionsLayout.addView(suggestionView);
        }

        dismissGreetingMunicipalitySuggestions();

        int popupHeight = (suggestions.size() * dpToPx(40)) + dpToPx(8);
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

    private List<String> findGreetingMunicipalitySuggestions(String query, List<String> municipalityNames) {
        String normalizedQuery = normalizeMunicipalityQuery(query);
        List<String> suggestions = new ArrayList<>();

        if (normalizedQuery.isEmpty()) {
            return suggestions;
        }

        for (String municipalityName : municipalityNames) {
            if (normalizeMunicipalityQuery(municipalityName).contains(normalizedQuery)) {
                suggestions.add(municipalityName);
                if (suggestions.size() == 4) {
                    break;
                }
            }
        }

        return suggestions;
    }

    private void showSelectedMunicipality(TextView selectedMunicipalityText, String municipalityName) {
        selectedMunicipalityText.setText(getString(R.string.greeting_municipality_selected, municipalityName));
        selectedMunicipalityText.setVisibility(View.VISIBLE);
    }

    private boolean isExactMunicipalityName(String value, List<String> municipalityNames) {
        for (String municipalityName : municipalityNames) {
            if (municipalityName.equalsIgnoreCase(value.trim())) {
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

        bottomNavigationView = findViewById(R.id.bottom_navigation);

        bottomNavigationView.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int itemId = item.getItemId();

                switch (itemId) {
                    case R.id.search_trash_button:
                        navController.navigate(R.id.action_to_searchTrash);
                        break;
                    case R.id.trash_types_button:
                        navController.navigate(R.id.fragment_trash_types);
                        break;
                    case R.id.municipalities_button:
                        navController.navigate(R.id.fragment_municipalities);
                        break;
                    case R.id.navigation_view_button:
                        if (drawerLayout.isDrawerOpen(GravityCompat.END)) {
                            drawerLayout.closeDrawer(GravityCompat.END);
                        } else {
                            drawerLayout.openDrawer(GravityCompat.END);
                        }
                        break;
                }
                return true;
            }
        });

        navigationView = findViewById(R.id.navigation_view);

        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int itemId = item.getItemId();

                switch (itemId) {
                    case R.id.om:
                        navController.navigate(R.id.fragment_about);
                        drawerLayout.closeDrawer(GravityCompat.END);
                        return true;
                    case R.id.kontaktinformation:
                        navController.navigate(R.id.fragment_contact_information);
                        drawerLayout.closeDrawer(GravityCompat.END);
                        return true;
                    case R.id.spørgsmålogsvar:
                        navController.navigate(R.id.fragment_qa);
                        drawerLayout.closeDrawer(GravityCompat.END);
                        return true;
                    case R.id.seneste_soegninger:
                        navController.navigate(R.id.fragment_recent_searches);
                        drawerLayout.closeDrawer(GravityCompat.END);
                        return true;
                    case R.id.sprog:
                        navController.navigate(R.id.fragment_language);
                        drawerLayout.closeDrawer(GravityCompat.END);
                        return true;
                    case R.id.privatlivspolitik:
                        navController.navigate(R.id.fragment_privacy_policy);
                        drawerLayout.closeDrawer(GravityCompat.END);
                        return true;
                    case R.id.vilkårogbetingelser:
                        navController.navigate(R.id.fragment_terms_of_service);
                        drawerLayout.closeDrawer(GravityCompat.END);
                        return true;
                }
                return false;
            }
        });
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
