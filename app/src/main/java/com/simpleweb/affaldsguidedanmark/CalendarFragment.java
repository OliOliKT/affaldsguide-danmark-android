package com.simpleweb.affaldsguidedanmark;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.ActivityNotFoundException;
import android.provider.CalendarContract;
import android.widget.ImageButton;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.RelativeSizeSpan;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import org.json.JSONObject;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class CalendarFragment extends Fragment {
    private static final String PREFS = "collection_calendar";
    private static final String ADDRESS = "address";
    private static final String ENABLED = "reminders_enabled";
    private static final String TIME = "reminder_time";
    private static final String EXCLUDED = "excluded_labels";

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final ActivityResultLauncher<String> notificationPermission = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(), granted -> {
                if (!isAdded()) return;
                if (granted) {
                    prefs().edit().putBoolean(ENABLED, true).apply();
                    scheduleReminders();
                } else {
                    prefs().edit().putBoolean(ENABLED, false).apply();
                    showError("notificationDenied");
                }
                render();
            });

    private final Handler suggestionHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService suggestionExecutor = Executors.newFixedThreadPool(2);
    private LinearLayout streetSuggestions, numberSuggestions, candidateViews;
    private int suggestionGeneration;
    private boolean applyingSuggestion;
    private String submittedQuery = "";
    private LinearLayout content;
    private final ActivityResultLauncher<Intent> calendarEditor = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> {
                // Calendar apps may return RESULT_CANCELED even after saving. Never infer saved status.
                if (isAdded() && content != null) render();
            });
    private Spinner municipalityInput;
    private EditText postcodeInput, streetInput, numberInput;
    private List<String> municipalities = new ArrayList<>();
    private List<CalendarApi.Address> candidates = new ArrayList<>();
    private List<CalendarApi.Pickup> pickups = new ArrayList<>();
    private CalendarApi.Address address;
    private String formMunicipality = "", formPostcode = "", formStreet = "", formNumber = "";
    private boolean busy, municipalitiesLoading, changingAddress;
    private String errorKey;
    private int requestGeneration;

    @Nullable @Override public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup parent,
                                                   @Nullable Bundle state) {
        ScrollView scroll = new ScrollView(requireContext());
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.page_background));
        content = new LinearLayout(requireContext());
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(20), dp(22), dp(20), dp(32));
        scroll.addView(content);
        address = savedAddress();
        changingAddress = false;
        CalendarWidgetData.Cache cached = CalendarWidgetData.cache(requireContext(), address);
        pickups = cached == null ? new ArrayList<>() : CalendarWidgetData.allUpcoming(cached.events, LocalDate.now());
        render();
        if (address == null) loadMunicipalities();
        else loadPickups();
        return scroll;
    }

    @Override public void onResume() {
        super.onResume();
        if (content != null && address != null && !changingAddress && pickups.isEmpty() && !busy) loadPickups();
    }

    @Override public void onDestroyView() {
        requestGeneration++;
        cancelSuggestions();
        busy = false;
        municipalitiesLoading = false;
        content = null;
        super.onDestroyView();
    }

    @Override public void onDestroy() {
        executor.shutdownNow();
        suggestionExecutor.shutdownNow();
        super.onDestroy();
    }

    private SharedPreferences prefs() { return requireContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE); }
    private String t(String suffix) { return InfoPageText.get(requireContext(), "calendar." + suffix); }
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }

    private CalendarApi.Address savedAddress() {
        try {
            JSONObject value = new JSONObject(prefs().getString(ADDRESS, ""));
            return new CalendarApi.Address(value.getString("municipality"), value.getString("id"), value.getString("label"));
        } catch (Exception ignored) { return null; }
    }

    private void saveAddress(CalendarApi.Address selected) {
        if (busy) return;
        if (CalendarFavouriteAddresses.matches(address, selected)) {
            cancelAddressChange();
            return;
        }
        if (!CalendarWidgetData.selectAddress(requireContext(), selected)) {
            showAddressSaveError();
            return;
        }
        requestGeneration++;
        cancelSuggestions();
        municipalitiesLoading = false;
        AchievementStore.get(requireContext()).recordCalendarAddressSaved();
        address = selected;
        CalendarWidgetProvider.addressChanged(requireContext());
        changingAddress = false;
        candidates.clear();
        pickups.clear();
        errorKey = null;
        clearAddressFormViews();
        render();
        loadPickups();
    }

    private List<CalendarApi.Address> favouriteAddresses() {
        try { return CalendarFavouriteAddresses.read(prefs().getString(CalendarFavouriteAddresses.KEY, "[]")); }
        catch (Exception error) {
            // Keep damaged data intact rather than silently replacing it on read.
            return Collections.emptyList();
        }
    }

    private void toggleFavourite() {
        if (address == null) return;
        try {
            List<CalendarApi.Address> current = CalendarFavouriteAddresses.read(
                    prefs().getString(CalendarFavouriteAddresses.KEY, "[]"));
            List<CalendarApi.Address> updated = CalendarFavouriteAddresses.toggled(current, address);
            if (!prefs().edit().putString(CalendarFavouriteAddresses.KEY,
                    CalendarFavouriteAddresses.encode(updated)).commit()) {
                showAddressSaveError();
                return;
            }
            render();
        } catch (Exception error) { showAddressSaveError(); }
    }

    private void showAddressSaveError() {
        android.widget.Toast.makeText(requireContext(), t("addressSaveError"), android.widget.Toast.LENGTH_LONG).show();
    }

    private void beginAddressChange() {
        requestGeneration++;
        cancelSuggestions();
        busy = false;
        municipalitiesLoading = false;
        changingAddress = true;
        formMunicipality = address.municipality;
        submittedQuery = "";
        candidates.clear();
        errorKey = null;
        clearAddressFormViews();
        render();
        loadMunicipalities();
    }

    private void cancelAddressChange() {
        requestGeneration++;
        cancelSuggestions();
        busy = false;
        municipalitiesLoading = false;
        changingAddress = false;
        candidates.clear();
        errorKey = null;
        clearAddressFormViews();
        render();
        if (address != null && pickups.isEmpty()) loadPickups();
    }

    private void clearAddressFormViews() {
        postcodeInput = null;
        streetInput = null;
        numberInput = null;
        municipalityInput = null;
    }

    private void makeAction(View view, String description, View.OnClickListener action) {
        view.setFocusable(true);
        view.setContentDescription(description);
        android.util.TypedValue ripple = new android.util.TypedValue();
        requireContext().getTheme().resolveAttribute(android.R.attr.selectableItemBackground, ripple, true);
        view.setForeground(ContextCompat.getDrawable(requireContext(), ripple.resourceId));
        view.setOnClickListener(action);
    }

    private void render() {
        if (content == null || !isAdded()) return;
        if ((address == null || changingAddress) && postcodeInput != null) {
            formPostcode = postcodeInput.getText().toString();
            formStreet = streetInput.getText().toString();
            formNumber = numberInput.getText().toString();
            if (municipalityInput != null && municipalityInput.getSelectedItemPosition() > 0) {
                formMunicipality = municipalities.get(municipalityInput.getSelectedItemPosition() - 1);
            }
        }
        content.removeAllViews();
        LinearLayout heading = row();
        TextView title = label(t("title"), 26, true);
        title.setTextColor(ContextCompat.getColor(requireContext(), R.color.green_light));
        title.setGravity(Gravity.CENTER);
        heading.addView(title, new LinearLayout.LayoutParams(0, -2, 1));
        content.addView(heading);
        space(18);
        if (address == null || changingAddress) renderAddressForm();
        else renderSavedAddress();
        if (errorKey != null) {
            TextView error = label(t(errorKey), 14, false);
            error.setTextColor(0xFFB45353);
            error.setPadding(0, dp(12), 0, dp(4));
            content.addView(error);
            if ((address == null || changingAddress) && municipalities.isEmpty()) {
                Button retry = button(t("retry"));
                retry.setOnClickListener(v -> loadMunicipalities());
                content.addView(retry);
            }
        }
        if (address != null || !municipalities.isEmpty()) {
            space(18);
            TextView source = label(t("sourceNote"), 13, false);
            source.setTextColor(ContextCompat.getColor(requireContext(), R.color.grey_black));
            content.addView(source);
        }
    }

    private void renderAddressForm() {
        TextView intro = label(t("intro"), 15, false);
        content.addView(intro);
        space(14);
        List<CalendarApi.Address> favourites = favouriteAddresses();
        if (!favourites.isEmpty()) {
            content.addView(label(t("favouriteAddresses"), 19, true));
            space(8);
            for (CalendarApi.Address favourite : favourites) {
                LinearLayout option = row();
                option.setPadding(dp(14), dp(12), dp(14), dp(12));
                option.setMinimumHeight(dp(56));
                LinearLayout details = new LinearLayout(requireContext());
                details.setOrientation(LinearLayout.VERTICAL);
                details.addView(label(favourite.label, 16, true));
                TextView municipality = label(displayMunicipalityName(favourite.municipality), 13, false);
                municipality.setTextColor(ContextCompat.getColor(requireContext(), R.color.grey_black));
                details.addView(municipality);
                option.addView(details, new LinearLayout.LayoutParams(0, -2, 1));
                TextView arrow = label("›", 22, false);
                arrow.setTextColor(ContextCompat.getColor(requireContext(), R.color.green_light));
                arrow.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
                option.addView(arrow);
                View favouriteCard = card(option);
                makeAction(favouriteCard, favourite.label + ", " + displayMunicipalityName(favourite.municipality),
                        v -> saveAddress(favourite));
                favouriteCard.setEnabled(!busy);
                content.addView(favouriteCard);
                space(8);
            }
            space(8);
            content.addView(label(t("newAddress"), 19, true));
            space(8);
        }
        municipalityInput = new Spinner(requireContext());
        List<String> options = new ArrayList<>();
        options.add(t(municipalitiesLoading ? "loadingMunicipalities" : "chooseMunicipality"));
        for (String municipality : municipalities) options.add(displayMunicipalityName(municipality));
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, options);
        municipalityInput.setAdapter(adapter);
        municipalityInput.setEnabled(!municipalities.isEmpty());
        String saved = (formMunicipality.isEmpty() ? SavedMunicipalityManager.getSavedMunicipalityName(requireContext()) : formMunicipality)
                .replace(" Kommune", "").replace("Københavns", "København");
        for (int i = 0; i < municipalities.size(); i++) {
            if (municipalities.get(i).replace(" Kommune", "").equalsIgnoreCase(saved)) {
                municipalityInput.setSelection(i + 1); break;
            }
        }
        content.addView(card(municipalityInput));
        space(10);
        postcodeInput = input(t("postcode"), InputType.TYPE_CLASS_NUMBER);
        postcodeInput.setText(formPostcode);
        content.addView(card(postcodeInput));
        space(10);
        streetInput = input(t("street"), InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        streetInput.setText(formStreet);
        content.addView(card(streetInput));
        streetSuggestions = new LinearLayout(requireContext());
        streetSuggestions.setOrientation(LinearLayout.VERTICAL);
        content.addView(streetSuggestions);
        space(10);
        numberInput = input(t("houseNumber"), InputType.TYPE_CLASS_TEXT);
        numberInput.setText(formNumber);
        content.addView(card(numberInput));
        numberSuggestions = new LinearLayout(requireContext());
        numberSuggestions.setOrientation(LinearLayout.VERTICAL);
        content.addView(numberSuggestions);
        space(14);
        Button find = button(t("findAddress"));
        find.setBackgroundResource(R.drawable.calendar_find_address_button);
        find.setBackgroundTintList(null);
        find.setTextColor(ContextCompat.getColorStateList(requireContext(), R.color.calendar_find_address_text));
        find.setAllCaps(false);
        find.setTextSize(16);
        find.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        find.setMinHeight(dp(50));
        Runnable updateFindButton = () -> find.setEnabled(!busy && !municipalities.isEmpty()
                && municipalityInput.getSelectedItemPosition() > 0
                && postcodeInput.getText().toString().trim().matches("[0-9]{4}")
                && !streetInput.getText().toString().trim().isEmpty());
        TextWatcher fieldWatcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateFindButton.run();
                if (!applyingSuggestion) {
                    candidates.clear();
                    if (candidateViews != null) candidateViews.setVisibility(View.GONE);
                    scheduleSuggestions();
                }
            }
            @Override public void afterTextChanged(Editable s) { }
        };
        postcodeInput.addTextChangedListener(fieldWatcher);
        streetInput.addTextChangedListener(fieldWatcher);
        numberInput.addTextChangedListener(fieldWatcher);
        municipalityInput.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                updateFindButton.run();
                scheduleSuggestions();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) { updateFindButton.run(); }
        });
        View.OnFocusChangeListener focusListener = (view, focused) -> scheduleSuggestions();
        streetInput.setOnFocusChangeListener(focusListener);
        numberInput.setOnFocusChangeListener(focusListener);
        updateFindButton.run();
        find.setOnClickListener(v -> searchAddresses());
        content.addView(find);
        if (busy) { space(8); content.addView(new ProgressBar(requireContext())); }
        candidateViews = new LinearLayout(requireContext());
        candidateViews.setOrientation(LinearLayout.VERTICAL);
        content.addView(candidateViews);
        if (!candidates.isEmpty()) {
            spaceIn(candidateViews, 18);
            candidateViews.addView(label(t("chooseAddress"), 19, true));
            spaceIn(candidateViews, 8);
            for (CalendarApi.Address candidate : candidates) {
                TextView option = label(candidate.label + "  ›", 16, false);
                option.setPadding(dp(15), dp(15), dp(15), dp(15));
                option.setOnClickListener(v -> saveAddress(candidate));
                candidateViews.addView(card(option));
                spaceIn(candidateViews, 8);
            }
        }
    }

    private void renderSavedAddress() {
        LinearLayout summary = column();
        summary.addView(label(t("yourAddress"), 13, true));
        summary.addView(label(address.label, 19, true));
        summary.addView(label(address.municipality, 14, false));
        LinearLayout addressRow = row();
        addressRow.addView(summary, new LinearLayout.LayoutParams(0, -2, 1));
        TextView arrow = label("›", 26, false);
        arrow.setTextColor(ContextCompat.getColor(requireContext(), R.color.grey_black));
        arrow.setPadding(0, 0, dp(16), 0);
        arrow.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        addressRow.addView(arrow);
        View addressCard = card(addressRow);
        addressCard.setFocusable(true);
        addressCard.setContentDescription(t("changeAddress") + ": " + address.label + ", " + address.municipality);
        android.util.TypedValue ripple = new android.util.TypedValue();
        requireContext().getTheme().resolveAttribute(android.R.attr.selectableItemBackground, ripple, true);
        addressCard.setForeground(ContextCompat.getDrawable(requireContext(), ripple.resourceId));
        addressCard.setOnClickListener(v -> beginAddressChange());
        content.addView(addressCard);
        space(10);
        boolean favourite = CalendarFavouriteAddresses.contains(favouriteAddresses(), address);
        LinearLayout favouriteRow = row();
        favouriteRow.setPadding(dp(16), dp(8), dp(16), dp(8));
        favouriteRow.setMinimumHeight(dp(54));
        ImageView star = new ImageView(requireContext());
        star.setImageResource(favourite ? R.drawable.calendar_star_filled : R.drawable.calendar_star_outline);
        star.setColorFilter(ContextCompat.getColor(requireContext(), R.color.green_light));
        star.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        favouriteRow.addView(star, new LinearLayout.LayoutParams(dp(24), dp(24)));
        TextView favouriteLabel = label(t(favourite ? "removeFavourite" : "addFavourite"), 15, true);
        favouriteLabel.setPadding(dp(10), 0, 0, 0);
        favouriteRow.addView(favouriteLabel, new LinearLayout.LayoutParams(0, -2, 1));
        View favouriteCard = card(favouriteRow);
        makeAction(favouriteCard, favouriteLabel.getText().toString(), v -> toggleFavourite());
        content.addView(favouriteCard);
        space(12);
        renderReminderControls();
        space(22);
        LinearLayout listHeader = row();
        listHeader.addView(label(t("upcoming"), 20, true), new LinearLayout.LayoutParams(0, -2, 1));
        SpannableString refreshText = new SpannableString("↻  " + t("refresh"));
        refreshText.setSpan(new RelativeSizeSpan(1.25f), 0, 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        TextView refresh = label(refreshText.toString(), 15, true);
        refresh.setText(refreshText);
        refresh.setTextColor(ContextCompat.getColor(requireContext(), R.color.green_light));
        refresh.setOnClickListener(v -> { if (!busy) loadPickups(); });
        listHeader.addView(refresh);
        content.addView(listHeader);
        space(10);
        if (busy && pickups.isEmpty()) {
            content.addView(label(t("loading"), 15, false));
            content.addView(new ProgressBar(requireContext()));
        } else if (pickups.isEmpty() && errorKey == null) {
            content.addView(label(t("noPickups"), 15, false));
        } else {
            for (CalendarApi.Pickup pickup : pickups) renderPickup(pickup);
        }
    }

    private void renderReminderControls() {
        LinearLayout box = column();
        Switch toggle = new Switch(requireContext());
        toggle.setText(t("reminders"));
        toggle.setTextSize(16);
        toggle.setChecked(prefs().getBoolean(ENABLED, false));
        toggle.setEnabled(!pickups.isEmpty() && !busy);
        toggle.setOnCheckedChangeListener((button, checked) -> {
            if (checked && Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                button.setChecked(false);
                notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS);
                return;
            }
            prefs().edit().putBoolean(ENABLED, checked).apply();
            if (checked) scheduleReminders(); else CalendarReminderWorker.clear(requireContext());
            render();
        });
        box.addView(toggle);
        if (toggle.isChecked()) {
            spaceIn(box, 8);
            box.addView(label(t("reminderTime"), 14, true));
            Spinner times = new Spinner(requireContext());
            String[] keys = {"eveningBefore", "morningBefore", "pickupMorning"};
            List<String> values = new ArrayList<>();
            for (String key : keys) values.add(t(key));
            times.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, values));
            String selected = prefs().getString(TIME, "eveningBefore");
            for (int i = 0; i < keys.length; i++) if (keys[i].equals(selected)) times.setSelection(i);
            times.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
                public void onItemSelected(android.widget.AdapterView<?> parent, View view, int index, long id) {
                    if (!keys[index].equals(prefs().getString(TIME, "eveningBefore"))) {
                        prefs().edit().putString(TIME, keys[index]).apply(); scheduleReminders();
                    }
                }
                public void onNothingSelected(android.widget.AdapterView<?> parent) { }
            });
            box.addView(times);
            spaceIn(box, 8);
            box.addView(label(t("pickupTypes"), 14, true));
            box.addView(label(t("pickupTypesDetail"), 13, false));
            Set<String> labels = new LinkedHashSet<>();
            for (CalendarApi.Pickup pickup : pickups) labels.add(pickup.label);
            Set<String> excluded = prefs().getStringSet(EXCLUDED, Collections.emptySet());
            for (String value : labels) {
                CheckBox check = new CheckBox(requireContext());
                check.setText(CalendarPickupLabels.display(requireContext(), value));
                check.setChecked(!excluded.contains(value));
                check.setOnCheckedChangeListener((button, checked) -> {
                    Set<String> next = new LinkedHashSet<>(prefs().getStringSet(EXCLUDED, Collections.emptySet()));
                    if (checked) next.remove(value); else next.add(value);
                    prefs().edit().putStringSet(EXCLUDED, next).apply();
                    scheduleReminders();
                });
                box.addView(check);
            }
        }
        content.addView(card(box));
    }

    private void renderPickup(CalendarApi.Pickup pickup) {
        LinearLayout row = row();
        row.setPadding(dp(14), dp(10), dp(14), dp(10));
        List<String> categories = categoriesFor(pickup.kind, pickup.label);
        List<Integer> pictogramResources = new ArrayList<>();
        for (String category : categories) {
            int imageRes = TrashDB.getImageResourceForKey(category, LanguageManager.usesNonDanishContent(requireContext()));
            if (imageRes != 0) pictogramResources.add(imageRes);
        }
        if (!pictogramResources.isEmpty()) {
            LinearLayout pictograms = column();
            pictograms.setPadding(0, 0, 0, 0);
            pictograms.setGravity(Gravity.CENTER);
            int iconSize = pictogramResources.size() == 1 ? 56 : 27;
            for (int i = 0; i < pictogramResources.size(); i += 2) {
                LinearLayout iconRow = row();
                int end = pictogramResources.size() == 1 ? 1 : Math.min(i + 2, pictogramResources.size());
                for (int j = i; j < end; j++) {
                    ImageView image = new ImageView(requireContext());
                    image.setImageResource(pictogramResources.get(j));
                    image.setScaleType(ImageView.ScaleType.FIT_CENTER);
                    GradientDrawable rounded = new GradientDrawable();
                    rounded.setColor(0xFFFFFFFF);
                    rounded.setCornerRadius(dp(7));
                    image.setBackground(rounded);
                    image.setClipToOutline(true);
                    LinearLayout.LayoutParams icon = new LinearLayout.LayoutParams(dp(iconSize), dp(iconSize));
                    if (j > i) icon.setMarginStart(dp(2));
                    iconRow.addView(image, icon);
                }
                if (i > 0) {
                    LinearLayout.LayoutParams iconRowParams = new LinearLayout.LayoutParams(-2, -2);
                    iconRowParams.topMargin = dp(2);
                    pictograms.addView(iconRow, iconRowParams);
                } else {
                    pictograms.addView(iconRow);
                }
            }
            LinearLayout.LayoutParams icons = new LinearLayout.LayoutParams(dp(56), dp(56));
            icons.setMarginEnd(dp(12));
            row.addView(pictograms, icons);
        }
        LinearLayout text = column();
        text.setPadding(0, 0, 0, 0);
        text.addView(label(CalendarPickupLabels.display(requireContext(), pickup.label), 16, true));
        String date = pickup.date;
        try {
            Locale locale = new Locale(LanguageManager.getSavedLanguage(requireContext()));
            date = LocalDate.parse(date).format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(locale));
        } catch (Exception ignored) { }
        TextView dateText = label(date, 14, false);
        dateText.setSingleLine(true);
        androidx.core.widget.TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(
                dateText, 11, 14, 1, android.util.TypedValue.COMPLEX_UNIT_SP);
        text.addView(dateText);
        CalendarApi.Address exportAddress = address;
        if (exportAddress != null && prefs().getStringSet(PickupCalendarDraft.OPENED_KEY,
                Collections.emptySet()).contains(PickupCalendarDraft.key(exportAddress, pickup))) {
            TextView previous = label(t("exportPreviouslyOpened"), 12, false);
            previous.setTextColor(ContextCompat.getColor(requireContext(), R.color.grey_black));
            text.addView(previous);
        }
        row.addView(text, new LinearLayout.LayoutParams(0, -2, 1));
        ImageButton add = new ImageButton(requireContext());
        add.setImageResource(R.drawable.calendar_add_event_icon);
        add.setColorFilter(ContextCompat.getColor(requireContext(), R.color.green_light));
        add.setPadding(dp(10), dp(10), dp(10), dp(10));
        android.util.TypedValue ripple = new android.util.TypedValue();
        requireContext().getTheme().resolveAttribute(android.R.attr.selectableItemBackgroundBorderless, ripple, true);
        add.setBackgroundResource(ripple.resourceId);
        add.setContentDescription(t("exportAdd") + ": " + CalendarPickupLabels.display(requireContext(), pickup.label) + ", " + date);
        add.setOnClickListener(v -> addPickupToCalendar(exportAddress, pickup));
        row.addView(add, new LinearLayout.LayoutParams(dp(44), dp(44)));
        content.addView(card(row));
        space(8);
    }

    private void addPickupToCalendar(CalendarApi.Address selectedAddress, CalendarApi.Pickup pickup) {
        if (selectedAddress == null) return;
        PickupCalendarDraft draft;
        try {
            String name = CalendarPickupLabels.display(requireContext(), pickup.label);
            draft = PickupCalendarDraft.create(selectedAddress, pickup,
                    t("exportTitle").replace("{0}", name), displayMunicipalityName(selectedAddress.municipality),
                    name, t("exportNotUpdated"));
        } catch (IllegalArgumentException | java.time.DateTimeException error) {
            new androidx.appcompat.app.AlertDialog.Builder(requireContext()).setMessage(t("exportDateError"))
                    .setPositiveButton(android.R.string.ok, null).show();
            return;
        }
        if (prefs().getStringSet(PickupCalendarDraft.OPENED_KEY, Collections.emptySet()).contains(draft.key)) {
            new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                    .setTitle(t("exportDuplicateOpenedTitle"))
                    .setMessage(t("exportDuplicateOpenedMessage"))
                    .setNegativeButton(t("cancelAddressChange"), null)
                    .setPositiveButton(t("exportAddAgain"), (dialog, which) -> openCalendarEditor(draft)).show();
        } else openCalendarEditor(draft);
    }

    private void openCalendarEditor(PickupCalendarDraft draft) {
        Intent intent = new Intent(Intent.ACTION_INSERT).setData(CalendarContract.Events.CONTENT_URI)
                .putExtra(CalendarContract.Events.TITLE, draft.title)
                .putExtra(CalendarContract.Events.DESCRIPTION, draft.notes)
                .putExtra(CalendarContract.EXTRA_EVENT_ALL_DAY, true)
                .putExtra(CalendarContract.Events.EVENT_TIMEZONE, "UTC")
                .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, draft.beginMillis)
                .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, draft.endMillis);
        try {
            calendarEditor.launch(intent);
            Set<String> opened = new LinkedHashSet<>(prefs().getStringSet(PickupCalendarDraft.OPENED_KEY, Collections.emptySet()));
            opened.add(draft.key);
            prefs().edit().putStringSet(PickupCalendarDraft.OPENED_KEY, opened).apply();
        } catch (ActivityNotFoundException | SecurityException error) {
            new androidx.appcompat.app.AlertDialog.Builder(requireContext()).setMessage(t("exportNoCalendarApp"))
                    .setPositiveButton(android.R.string.ok, null).show();
        }
    }

    private static List<String> categoriesFor(String kind, String label) {
        if (label.equalsIgnoreCase("Papir/Plast") || label.equalsIgnoreCase("Plast/Papir"))
            return java.util.Arrays.asList("Papir", "Plast");
        switch (kind) {
            case "pap": return Collections.singletonList("Pap");
            case "pappapir": return java.util.Arrays.asList("Pap", "Papir");
            case "papir": return Collections.singletonList("Papir");
            case "plast": return Collections.singletonList("Plast");
            case "plastmadkarton": return java.util.Arrays.asList("Plast", "Mad- og drikkekartoner");
            case "plastmetal": return java.util.Arrays.asList("Plast", "Metal");
            case "plastmetalmdk": return java.util.Arrays.asList("Plast", "Metal", "Mad- og drikkekartoner");
            case "plastmetalpapir": return java.util.Arrays.asList("Plast", "Metal", "Papir");
            case "glas": return Collections.singletonList("Glas");
            case "glasplast": return java.util.Arrays.asList("Glas", "Plast", "Mad- og drikkekartoner");
            case "metal": case "jern": return Collections.singletonList("Metal");
            case "metalglas": return java.util.Arrays.asList("Metal", "Glas");
            case "madaffald": case "bioposer": return Collections.singletonList("Madaffald");
            case "restaffald": case "dagrenovation": return Collections.singletonList("Restaffald");
            case "restaffaldmadaffald": return java.util.Arrays.asList("Restaffald", "Madaffald");
            case "farligtaffald": case "farligtaffaldmiljoboks": return Collections.singletonList("Farligt affald");
            case "haveaffald": case "flis": case "juletrae": return Collections.singletonList("Haveaffald");
            case "tekstil": return Collections.singletonList("Tekstilaffald");
            case "elektronik": return Collections.singletonList("Småt elektronik");
            case "batterier": return Collections.singletonList("Batterier");
            case "storskrald": return Collections.singletonList("Storskrald");
            case "storskraldogfarligtaffald": return java.util.Arrays.asList("Storskrald", "Farligt affald");
            case "storskraldogtekstilaffald": return java.util.Arrays.asList("Storskrald", "Tekstilaffald");
            case "papirmetal": return java.util.Arrays.asList("Papir", "Metal");
            case "papirglas": return java.util.Arrays.asList("Papir", "Pap", "Glas");
            case "papirglasdaaser": return java.util.Arrays.asList("Papir", "Glas", "Metal");
            case "papirglasmetalplast": return java.util.Arrays.asList("Papir", "Glas", "Metal", "Plast");
            case "pappapirglasmetal": return java.util.Arrays.asList("Pap", "Papir", "Glas", "Metal");
            case "pappapirtekstil": return java.util.Arrays.asList("Pap", "Papir", "Tekstilaffald");
            case "pappi": return java.util.Arrays.asList("Pap", "Papir", "Plast");
            case "plastmdkglasmetal": return java.util.Arrays.asList("Plast", "Mad- og drikkekartoner", "Glas", "Metal");
            case "restplast": return java.util.Arrays.asList("Restaffald", "Plast", "Mad- og drikkekartoner");
            default: return Collections.emptyList();
        }
    }

    private void loadMunicipalities() {
        if (municipalitiesLoading || (address != null && !changingAddress)) return;
        municipalitiesLoading = true; errorKey = null; render();
        int generation = ++requestGeneration;
        executor.execute(() -> {
            try {
                List<String> result = CalendarApi.municipalities();
                onUi(generation, () -> { municipalities = result; municipalitiesLoading = false; render(); });
            } catch (Exception error) {
                onUi(generation, () -> { municipalitiesLoading = false; showError(isOffline(error) ? "offline" : "unavailable"); render(); });
            }
        });
    }

    private void searchAddresses() {
        if (busy || municipalityInput == null || municipalityInput.getSelectedItemPosition() < 1) return;
        String postcode = postcodeInput.getText().toString().trim();
        String street = streetInput.getText().toString().trim();
        String number = numberInput.getText().toString().trim();
        if (!postcode.matches("[0-9]{4}") || street.isEmpty()) return;
        String municipality = municipalities.get(municipalityInput.getSelectedItemPosition() - 1);
        formMunicipality = municipality; formPostcode = postcode; formStreet = street; formNumber = number;
        View focused = requireActivity().getCurrentFocus();
        if (focused != null) {
            ((InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE))
                    .hideSoftInputFromWindow(focused.getWindowToken(), 0);
            focused.clearFocus();
        }
        submittedQuery = suggestionQuery();
        cancelSuggestions();
        busy = true; errorKey = null; candidates.clear(); render();
        int generation = ++requestGeneration;
        executor.execute(() -> {
            try {
                List<CalendarApi.Address> result = CalendarApi.search(municipality, postcode, street, number);
                onUi(generation, () -> { candidates = result; busy = false; if (result.isEmpty()) showError("noAddresses"); render(); });
            } catch (Exception error) {
                onUi(generation, () -> { busy = false; showError(isOffline(error) ? "offline" : "unavailable"); render(); });
            }
        });
    }

    private String suggestionQuery() {
        return municipalityInput.getSelectedItemPosition() + "|" + postcodeInput.getText().toString().trim()
                + "|" + streetInput.getText().toString().trim() + "|" + numberInput.getText().toString().trim();
    }

    private void cancelSuggestions() {
        suggestionGeneration++;
        suggestionHandler.removeCallbacksAndMessages(null);
        if (streetSuggestions != null) streetSuggestions.removeAllViews();
        if (numberSuggestions != null) numberSuggestions.removeAllViews();
    }

    private void scheduleSuggestions() {
        cancelSuggestions();
        if (applyingSuggestion || content == null || busy || (address != null && !changingAddress) || municipalityInput == null
                || municipalityInput.getSelectedItemPosition() < 1) return;
        boolean forStreet = streetInput.hasFocus();
        if (!forStreet && !numberInput.hasFocus()) return;
        String postcode = postcodeInput.getText().toString().trim();
        String street = streetInput.getText().toString().trim();
        String number = forStreet ? "" : numberInput.getText().toString().trim();
        String query = suggestionQuery();
        if (!postcode.matches("[0-9]{4}") || street.length() < 3 || (!forStreet && number.isEmpty())
                || query.equals(submittedQuery)) return;
        String municipality = municipalities.get(municipalityInput.getSelectedItemPosition() - 1);
        LinearLayout box = forStreet ? streetSuggestions : numberSuggestions;
        int generation = suggestionGeneration;
        LinearLayout loading = row();
        loading.setPadding(0, dp(8), 0, dp(4));
        ProgressBar spinner = new ProgressBar(requireContext());
        loading.addView(spinner, new LinearLayout.LayoutParams(dp(20), dp(20)));
        TextView loadingText = label(t("loadingSuggestions"), 13, false);
        loadingText.setPadding(dp(8), 0, 0, 0);
        loading.addView(loadingText);
        box.addView(loading);
        suggestionHandler.postDelayed(() -> suggestionExecutor.execute(() -> {
            try {
                List<String> labels = CalendarApi.suggestions(municipality, postcode, street, number);
                List<CalendarAddressSuggestions.Option> options = CalendarAddressSuggestions.options(labels, postcode, forStreet);
                suggestionHandler.post(() -> {
                    if (!isAdded() || content == null || generation != suggestionGeneration) return;
                    box.removeAllViews();
                    if (options.isEmpty()) box.addView(label(t("noSuggestionsYet"), 13, false));
                    else showSuggestionOptions(box, options, 0, forStreet);
                });
            } catch (Exception error) {
                suggestionHandler.post(() -> {
                    if (!isAdded() || content == null || generation != suggestionGeneration) return;
                    box.removeAllViews();
                    box.addView(label(t("suggestionsUnavailable"), 13, false));
                });
            }
        }), 600);
    }

    private void showSuggestionOptions(LinearLayout box, List<CalendarAddressSuggestions.Option> options, int start, boolean forStreet) {
        int end = Math.min(start + 8, options.size());
        for (int i = start; i < end; i++) {
            CalendarAddressSuggestions.Option suggestion = options.get(i);
            TextView option = label(suggestion.label + "  ›", 15, false);
            option.setPadding(dp(14), dp(14), dp(14), dp(14));
            option.setBackgroundResource(R.drawable.result_description_background);
            option.setOnClickListener(view -> {
                applyingSuggestion = true;
                cancelSuggestions();
                if (forStreet) streetInput.setText(suggestion.value);
                else numberInput.setText(suggestion.value);
                submittedQuery = suggestionQuery();
                applyingSuggestion = false;
                // Selecting a suggestion only fills the form; Find address submits it.
            });
            box.addView(option, new LinearLayout.LayoutParams(-1, -2));
        }
        if (end < options.size()) {
            TextView more = label(t("moreSuggestions"), 15, true);
            more.setTextColor(ContextCompat.getColor(requireContext(), R.color.green_light));
            more.setPadding(dp(14), dp(14), dp(14), dp(14));
            more.setOnClickListener(view -> {
                box.removeView(more);
                showSuggestionOptions(box, options, end, forStreet);
            });
            box.addView(more);
        }
    }

    private static String displayMunicipalityName(String name) {
        if (name.equalsIgnoreCase("København")) return "Københavns Kommune";
        if (name.equalsIgnoreCase("Bornholm")) return "Bornholms Regionskommune";
        return name.endsWith("Kommune") ? name : name + " Kommune";
    }

    private void loadPickups() {
        if (busy || address == null || changingAddress) return;
        CalendarApi.Address requested = address;
        long requestedAt = System.currentTimeMillis();
        busy = true; errorKey = null; render();
        int generation = ++requestGeneration;
        executor.execute(() -> {
            try {
                List<CalendarApi.Pickup> result;
                try { result = CalendarApi.pickups(requested); }
                catch (Exception first) {
                    if (!retryable(first)) throw first;
                    Thread.sleep(2000);
                    result = CalendarApi.pickups(requested);
                }
                List<CalendarApi.Pickup> finalResult = result;
                onUi(generation, () -> {
                    boolean saved = CalendarWidgetData.save(requireContext(), requested, finalResult, requestedAt);
                    CalendarWidgetData.Cache latest = CalendarWidgetData.cache(requireContext(), requested);
                    pickups = !saved && latest != null ? CalendarWidgetData.allUpcoming(latest.events, LocalDate.now()) : finalResult;
                    busy = false;
                    if (prefs().getBoolean(ENABLED, false)) scheduleReminders();
                    CalendarWidgetProvider.updateAll(requireContext());
                    AppReviewCoordinator.completedTask(requireContext());
                    render();
                });
            } catch (Exception error) {
                onUi(generation, () -> { busy = false; showError(isOffline(error) ? "offline" : "pickupsUnavailable"); render(); });
            }
        });
    }

    private void scheduleReminders() {
        if (!prefs().getBoolean(ENABLED, false) || address == null || pickups.isEmpty()) return;
        CalendarReminderWorker.schedule(requireContext(), pickups,
                prefs().getStringSet(EXCLUDED, Collections.emptySet()), prefs().getString(TIME, "eveningBefore"));
    }

    private void onUi(int generation, Runnable result) {
        if (getActivity() != null) getActivity().runOnUiThread(() -> {
            if (generation == requestGeneration && isAdded() && content != null) result.run();
        });
    }

    private static boolean retryable(Exception error) {
        return !(error instanceof CalendarApi.HttpError) || ((CalendarApi.HttpError) error).status == 429
                || ((CalendarApi.HttpError) error).status >= 500;
    }
    private static boolean isOffline(Exception error) {
        return error instanceof java.net.UnknownHostException || error instanceof java.net.ConnectException;
    }
    private void showError(String key) { errorKey = key; }

    private LinearLayout row() { LinearLayout value = new LinearLayout(requireContext()); value.setOrientation(LinearLayout.HORIZONTAL); value.setGravity(Gravity.CENTER_VERTICAL); return value; }
    private LinearLayout column() { LinearLayout value = new LinearLayout(requireContext()); value.setOrientation(LinearLayout.VERTICAL); value.setPadding(dp(16), dp(14), dp(16), dp(14)); return value; }
    private TextView label(String value, int size, boolean bold) {
        TextView text = new TextView(requireContext()); text.setText(value); text.setTextSize(size);
        text.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_color));
        if (bold) text.setTypeface(null, Typeface.BOLD);
        return text;
    }
    private EditText input(String hint, int type) {
        EditText field = new EditText(requireContext()); field.setHint(hint); field.setTextSize(16);
        field.setSingleLine(true); field.setInputType(type); field.setBackgroundColor(0x00000000);
        field.setPadding(dp(14), dp(12), dp(14), dp(12)); return field;
    }
    private Button button(String value) { Button result = new Button(requireContext()); result.setText(value); return result; }
    private View card(View child) {
        LinearLayout wrapper = new LinearLayout(requireContext()); wrapper.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable background = new GradientDrawable(); background.setColor(0xFFF7F8F7);
        background.setCornerRadius(dp(12)); background.setStroke(dp(1), 0xFFE5E7EB);
        wrapper.setBackground(background); wrapper.addView(child);
        return wrapper;
    }
    private void space(int height) { spaceIn(content, height); }
    private void spaceIn(LinearLayout target, int height) { View spacer = new View(requireContext()); target.addView(spacer, new LinearLayout.LayoutParams(1, dp(height))); }
}
