package com.simpleweb.affaldsguidedanmark;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Rect;
import android.graphics.Typeface;
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
import android.widget.Button;
import android.widget.EditText;
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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.snackbar.BaseTransientBottomBar;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    private NavController navController;
    private Button begynd;
    private DrawerLayout drawerLayout;
    private BottomNavigationView bottomNavigationView;
    private NavigationView navigationView;
    private SharedPreferences sharedPref;
    private int greetingOnboardingStep = 1;
    private String greetingSelectedMunicipalityName = "";
    private int lastSelectedBottomNavigationItemId = R.id.search_trash_button;
    private boolean isUpdatingBottomNavigationSelection = false;
    private boolean achievementBannerShowing = false;
    private boolean reviewReady;
    private final android.os.Handler reviewHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private final Runnable reviewRequest = () -> {
        if (isReviewSurfaceReady()) AppReviewCoordinator.get(this).request(this, this::isReviewSurfaceReady);
    };

    void completedReviewTask() {
        reviewReady = true;
        scheduleReviewRequest();
    }

    private void scheduleReviewRequest() {
        reviewHandler.removeCallbacks(reviewRequest);
        if (reviewReady && AppReviewCoordinator.get(this).eligible(this)) reviewHandler.postDelayed(reviewRequest, 3000);
    }

    private boolean isReviewSurfaceReady() {
        if (!reviewReady || !getLifecycle().getCurrentState().isAtLeast(androidx.lifecycle.Lifecycle.State.RESUMED)
                || isFinishing() || isDestroyed() || !hasWindowFocus() || achievementBannerShowing
                || navController == null || navController.getCurrentDestination() == null
                || drawerLayout == null || drawerLayout.isDrawerOpen(GravityCompat.END)) return false;
        androidx.core.view.WindowInsetsCompat insets = ViewCompat.getRootWindowInsets(getWindow().getDecorView());
        if (insets != null && insets.isVisible(WindowInsetsCompat.Type.ime())) return false;
        int destination = navController.getCurrentDestination().getId();
        return destination == R.id.search_trash_fragment || destination == R.id.fragment_calendar
                || destination == R.id.fragment_sorting_impact;
    }

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
            openCalendarFromIntent(getIntent());
            openWidgetShortcutFromIntent(getIntent());
        }
    }

    @Override protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        openCalendarFromIntent(intent);
        openWidgetShortcutFromIntent(intent);
    }

    private void openCalendarFromIntent(Intent intent) {
        if (intent == null || !intent.getBooleanExtra("open_calendar", false) || navController == null) return;
        intent.removeExtra("open_calendar");
        navController.navigate(R.id.fragment_calendar);
    }

    private void openWidgetShortcutFromIntent(Intent intent) {
        if (intent == null || navController == null) return;
        String shortcut = intent.getStringExtra(PhotoSearchWidgetProvider.EXTRA_SHORTCUT);
        if (shortcut == null) return;
        intent.removeExtra(PhotoSearchWidgetProvider.EXTRA_SHORTCUT);
        Bundle args = new Bundle();
        args.putBoolean("openWidgetCamera", PhotoSearchWidgetProvider.SHORTCUT_CAMERA.equals(shortcut));
        args.putBoolean("focusWidgetSearch", PhotoSearchWidgetProvider.SHORTCUT_SEARCH.equals(shortcut));
        navController.navigate(R.id.action_to_searchTrash, args);
    }

    @Override
    protected void onResume() {
        super.onResume();
        AchievementStore store = AchievementStore.get(this);
        store.setUnlockListener(this::showPendingAchievement);
        store.recordAppUse();
        AppReviewCoordinator.get(this).recordActiveDay();
        showPendingAchievement();
    }

    @Override
    protected void onPause() {
        AchievementStore.get(this).setUnlockListener(null);
        reviewReady = false;
        reviewHandler.removeCallbacks(reviewRequest);
        super.onPause();
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            reviewReady = false;
            reviewHandler.removeCallbacks(reviewRequest);
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
        View languageStep = findViewById(R.id.onboardingLanguageStep);
        View municipalityStep = findViewById(R.id.onboardingMunicipalityStep);
        findViewById(R.id.onboardingLocationStep).setVisibility(View.GONE);
        View onboardingDotOne = findViewById(R.id.onboardingDotOne);
        View onboardingDotTwo = findViewById(R.id.onboardingDotTwo);
        findViewById(R.id.onboardingDotThree).setVisibility(View.GONE);
        TextView backButton = findViewById(R.id.onboardingBackButton);
        TextView skipButton = findViewById(R.id.onboardingSkipButton);
        View selectedMunicipalityRow = findViewById(R.id.greetingSelectedMunicipality);
        TextView selectedMunicipalityText = findViewById(R.id.greetingSelectedMunicipalityName);
        TextView municipalityTitle = findViewById(R.id.greetingMunicipalityTitle);
        TextView municipalitySubtitle = findViewById(R.id.greetingMunicipalityText);
        EditText municipalityInput = findViewById(R.id.greetingMunicipalityInput);
        RecyclerView municipalityList = findViewById(R.id.greetingMunicipalityList);
        List<Municipality> municipalities = new MunicipalityDB(getResources()).getMunicipalities();
        greetingSelectedMunicipalityName = SavedMunicipalityManager.getSavedMunicipalityName(this);

        municipalityList.setLayoutManager(new LinearLayoutManager(this));
        OnboardingMunicipalityAdapter municipalityAdapter = new OnboardingMunicipalityAdapter(
                municipalities,
                greetingSelectedMunicipalityName,
                municipality -> {
                    greetingSelectedMunicipalityName = municipality.getMunicipality();
                    sharedPref.edit().putString("greetingSelectedMunicipality", greetingSelectedMunicipalityName).apply();
                    showSelectedMunicipality(selectedMunicipalityRow, selectedMunicipalityText, greetingSelectedMunicipalityName);
                    begynd.setEnabled(true);
                    begynd.setAlpha(1f);
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    if (imm != null) {
                        imm.hideSoftInputFromWindow(municipalityInput.getWindowToken(), 0);
                    }
                    municipalityInput.clearFocus();
                }
        );
        municipalityList.setAdapter(municipalityAdapter);
        String pendingMunicipality = sharedPref.getString("greetingSelectedMunicipality", "");
        if (!pendingMunicipality.isEmpty()) {
            greetingSelectedMunicipalityName = pendingMunicipality;
            municipalityAdapter.setSelectedMunicipalityName(pendingMunicipality);
        }
        if (!greetingSelectedMunicipalityName.isEmpty()) {
            showSelectedMunicipality(selectedMunicipalityRow, selectedMunicipalityText, greetingSelectedMunicipalityName);
        }
        municipalityInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                municipalityAdapter.filter(s.toString());
            }
            @Override public void afterTextChanged(Editable s) { }
        });
        municipalityInput.setOnFocusChangeListener((view, hasFocus) -> {
            municipalityTitle.setVisibility(hasFocus ? View.GONE : View.VISIBLE);
            municipalitySubtitle.setVisibility(hasFocus ? View.GONE : View.VISIBLE);
        });

        findViewById(R.id.danishLanguageOption).setOnClickListener(view -> selectGreetingLanguage(LanguageManager.DANISH));
        findViewById(R.id.englishLanguageOption).setOnClickListener(view -> selectGreetingLanguage(LanguageManager.ENGLISH));
        findViewById(R.id.arabicLanguageOption).setOnClickListener(view -> selectGreetingLanguage(LanguageManager.ARABIC));

        begynd = findViewById(R.id.begynd);
        begynd.setEnabled(!greetingSelectedMunicipalityName.isEmpty());
        begynd.setAlpha(greetingSelectedMunicipalityName.isEmpty() ? 0.5f : 1f);
        greetingOnboardingStep = Math.max(1, Math.min(2, sharedPref.getInt("greetingOnboardingStep", 1)));
        showGreetingOnboardingStep(
                greetingOnboardingStep,
                languageStep,
                municipalityStep,
                onboardingDotOne,
                onboardingDotTwo,
                backButton,
                skipButton,
                begynd
        );

        begynd.setOnClickListener(view -> finishGreetingOnboarding());

        View.OnClickListener goBack = view -> {
            if (greetingOnboardingStep == 1) {
                return;
            }

            greetingOnboardingStep--;
            sharedPref.edit().putInt("greetingOnboardingStep", greetingOnboardingStep).apply();
            showGreetingOnboardingStep(
                    greetingOnboardingStep,
                    languageStep,
                    municipalityStep,
                    onboardingDotOne,
                    onboardingDotTwo,
                    backButton,
                    skipButton,
                    begynd
            );
        };
        backButton.setOnClickListener(goBack);

        skipButton.setOnClickListener(view -> {
            if (greetingOnboardingStep == 1) {
                greetingOnboardingStep = 2;
                sharedPref.edit().putInt("greetingOnboardingStep", 2).apply();
                showGreetingOnboardingStep(2, languageStep, municipalityStep,
                        onboardingDotOne, onboardingDotTwo, backButton, skipButton, begynd);
            } else if (greetingOnboardingStep == 2) {
                greetingSelectedMunicipalityName = "";
                sharedPref.edit().remove("greetingSelectedMunicipality").apply();
                finishGreetingOnboarding();
            } else {
                finishGreetingOnboarding();
            }
        });
    }

    private void selectGreetingLanguage(String language) {
        LanguageManager.saveLanguage(this, language);
        sharedPref.edit().putInt("greetingOnboardingStep", 2).apply();
        recreate();
    }

    private void showGreetingOnboardingStep(
            int step,
            View languageStep,
            View municipalityStep,
            View onboardingDotOne,
            View onboardingDotTwo,
            TextView backButton,
            TextView skipButton,
            Button continueButton
    ) {
        if (step == 1) {
            View focusedView = getCurrentFocus();
            if (focusedView instanceof EditText) {
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.hideSoftInputFromWindow(focusedView.getWindowToken(), 0);
                }
                focusedView.clearFocus();
            }
        }
        languageStep.setVisibility(step == 1 ? View.VISIBLE : View.GONE);
        municipalityStep.setVisibility(step == 2 ? View.VISIBLE : View.GONE);

        onboardingDotOne.setBackgroundResource(step == 1 ? R.drawable.onboarding_dot_active : R.drawable.onboarding_dot_inactive);
        onboardingDotTwo.setBackgroundResource(step == 2 ? R.drawable.onboarding_dot_active : R.drawable.onboarding_dot_inactive);

        findViewById(R.id.onboardingButtonRow).setVisibility(View.VISIBLE);
        backButton.setVisibility(step == 1 ? View.INVISIBLE : View.VISIBLE);
        skipButton.setVisibility(View.VISIBLE);
        continueButton.setVisibility(step == 2 ? View.VISIBLE : View.GONE);
        continueButton.setEnabled(!greetingSelectedMunicipalityName.isEmpty());
        continueButton.setAlpha(continueButton.isEnabled() ? 1f : 0.5f);
        continueButton.setText(R.string.onboarding_continue);
    }

    private void finishGreetingOnboarding() {
        if (!greetingSelectedMunicipalityName.isEmpty()) {
            SavedMunicipalityManager.save(this, greetingSelectedMunicipalityName);
        }

        sharedPref.edit()
                .putBoolean("hasSeenGreetingPage", true)
                .remove("greetingOnboardingStep")
                .remove("greetingSelectedMunicipality")
                .remove("greetingLocationRequested")
                .apply();
        setContentView(R.layout.activity_main);
        applyMainSystemBarInsets();
        setUpMainLayout();
        openWidgetShortcutFromIntent(getIntent());
    }

    private void showSelectedMunicipality(View selectedMunicipalityRow, TextView selectedMunicipalityText, String municipalityName) {
        selectedMunicipalityText.setText(municipalityName);
        selectedMunicipalityRow.setContentDescription(getString(R.string.greeting_municipality_selected, municipalityName));
        selectedMunicipalityRow.setVisibility(View.VISIBLE);
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
                } else if (itemId == R.id.calendar_button) {
                    navController.navigate(R.id.fragment_calendar);
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
        navigationView.getMenu().findItem(R.id.rate_app).setTitle(InfoPageText.get(this, "more.rateApp"));
        navigationView.getMenu().findItem(R.id.sorting_impact).setTitle(InfoPageText.get(this, "impact.page"));
        navigationView.getMenu().findItem(R.id.achievements)
                .setTitle(InfoPageText.get(this, "achievements.page"));

        navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
            reviewReady = false;
            reviewHandler.removeCallbacks(reviewRequest);
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

                if (itemId == R.id.rate_app) {
                    reviewReady = false;
                    reviewHandler.removeCallbacks(reviewRequest);
                    drawerLayout.closeDrawer(GravityCompat.END);
                    AppReviewCoordinator.get(MainActivity.this).openStore(MainActivity.this);
                    return true;
                } else if (itemId == R.id.sorting_impact) {
                    navController.navigate(R.id.fragment_sorting_impact);
                    drawerLayout.closeDrawer(GravityCompat.END);
                    return true;
                } else if (itemId == R.id.om) {
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
        AchievementStore.Unlock unlock = store.takePendingUnlock();
        if (unlock == null) return;
        String id = unlock.achievement.id;
        achievementBannerShowing = true;
        String title = InfoPageText.get(this, "achievements." + id + ".title");
        String detail = AchievementStore.detail(this, unlock.achievement,
                unlock.achievement.thresholds[unlock.level - 1]);
        if (unlock.achievement.thresholds.length > 1) title += " " + new String(new char[unlock.level]).replace("\0", "★");
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
                scheduleReviewRequest();
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

        if (destinationId == R.id.fragment_calendar) {
            return R.id.calendar_button;
        }

        if (destinationId == R.id.fragment_sorting_impact
                || destinationId == R.id.fragment_recent_searches
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

}
