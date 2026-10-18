package com.simpleweb.affaldsguidedanmark;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
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

    private LinearLayout content;
    private Spinner municipalityInput;
    private EditText postcodeInput, streetInput, numberInput;
    private List<String> municipalities = new ArrayList<>();
    private List<CalendarApi.Address> candidates = new ArrayList<>();
    private List<CalendarApi.Pickup> pickups = new ArrayList<>();
    private CalendarApi.Address address;
    private String formMunicipality = "", formPostcode = "", formStreet = "", formNumber = "";
    private boolean busy, municipalitiesLoading;
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
        render();
        if (address == null) loadMunicipalities();
        else loadPickups();
        return scroll;
    }

    @Override public void onResume() {
        super.onResume();
        if (content != null && address != null && pickups.isEmpty() && !busy) loadPickups();
    }

    @Override public void onDestroyView() {
        requestGeneration++;
        content = null;
        super.onDestroyView();
    }

    @Override public void onDestroy() {
        executor.shutdownNow();
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
        try {
            JSONObject value = new JSONObject().put("municipality", selected.municipality)
                    .put("id", selected.id).put("label", selected.label);
            prefs().edit().putString(ADDRESS, value.toString()).apply();
        } catch (Exception ignored) { return; }
        AchievementStore.get(requireContext()).recordCalendarAddressSaved();
        address = selected;
        candidates.clear();
        pickups.clear();
        errorKey = null;
        render();
        loadPickups();
    }

    private void render() {
        if (content == null || !isAdded()) return;
        if (address == null && postcodeInput != null) {
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
        heading.addView(title, new LinearLayout.LayoutParams(0, -2, 1));
        TextView help = label("ⓘ", 25, false);
        help.setTextColor(ContextCompat.getColor(requireContext(), R.color.green_light));
        help.setContentDescription(t("helpTitle"));
        help.setOnClickListener(v -> new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle(t("helpTitle")).setMessage(t("helpMessage")).setPositiveButton(android.R.string.ok, null).show());
        heading.addView(help);
        content.addView(heading);
        space(18);
        if (address == null) renderAddressForm();
        else renderSavedAddress();
        if (errorKey != null) {
            TextView error = label(t(errorKey), 14, false);
            error.setTextColor(0xFFB45353);
            error.setPadding(0, dp(12), 0, dp(4));
            content.addView(error);
            if (address == null && municipalities.isEmpty()) {
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
        space(10);
        numberInput = input(t("houseNumber"), InputType.TYPE_CLASS_TEXT);
        numberInput.setText(formNumber);
        content.addView(card(numberInput));
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
                && !streetInput.getText().toString().trim().isEmpty()
                && !numberInput.getText().toString().trim().isEmpty());
        TextWatcher fieldWatcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { updateFindButton.run(); }
            @Override public void afterTextChanged(Editable s) { }
        };
        postcodeInput.addTextChangedListener(fieldWatcher);
        streetInput.addTextChangedListener(fieldWatcher);
        numberInput.addTextChangedListener(fieldWatcher);
        municipalityInput.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                updateFindButton.run();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) { updateFindButton.run(); }
        });
        updateFindButton.run();
        find.setOnClickListener(v -> searchAddresses());
        content.addView(find);
        if (busy) { space(8); content.addView(new ProgressBar(requireContext())); }
        if (!candidates.isEmpty()) {
            space(18);
            content.addView(label(t("chooseAddress"), 19, true));
            space(8);
            for (CalendarApi.Address candidate : candidates) {
                TextView option = label(candidate.label + "  ›", 16, false);
                option.setPadding(dp(15), dp(15), dp(15), dp(15));
                option.setOnClickListener(v -> saveAddress(candidate));
                content.addView(card(option));
                space(8);
            }
        }
    }

    private void renderSavedAddress() {
        LinearLayout summary = column();
        summary.addView(label(t("yourAddress"), 13, true));
        summary.addView(label(address.label, 19, true));
        summary.addView(label(address.municipality, 14, false));
        content.addView(card(summary));
        space(10);
        TextView change = label(t("changeAddress") + "  ›", 16, true);
        change.setPadding(dp(16), dp(15), dp(16), dp(15));
        change.setOnClickListener(v -> {
            CalendarReminderWorker.clear(requireContext());
            prefs().edit().remove(ADDRESS).putBoolean(ENABLED, false).remove(EXCLUDED).apply();
            address = null; pickups.clear(); errorKey = null; busy = false;
            postcodeInput = null; streetInput = null; numberInput = null; municipalityInput = null;
            render(); loadMunicipalities();
        });
        content.addView(card(change));
        space(12);
        renderReminderControls();
        space(22);
        LinearLayout listHeader = row();
        listHeader.addView(label(t("upcoming"), 20, true), new LinearLayout.LayoutParams(0, -2, 1));
        TextView refresh = label("↻  " + t("refresh"), 15, true);
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
                check.setText(value);
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
        text.addView(label(pickup.label, 16, true));
        String date = pickup.date;
        try {
            Locale locale = new Locale(LanguageManager.getSavedLanguage(requireContext()));
            date = LocalDate.parse(date).format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(locale));
        } catch (Exception ignored) { }
        text.addView(label(date, 14, false));
        row.addView(text);
        content.addView(card(row));
        space(8);
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
        if (municipalitiesLoading || address != null) return;
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
        if (!postcode.matches("[0-9]{4}") || street.isEmpty() || number.isEmpty()) return;
        String municipality = municipalities.get(municipalityInput.getSelectedItemPosition() - 1);
        formMunicipality = municipality; formPostcode = postcode; formStreet = street; formNumber = number;
        View focused = requireActivity().getCurrentFocus();
        if (focused != null) {
            ((InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE))
                    .hideSoftInputFromWindow(focused.getWindowToken(), 0);
            focused.clearFocus();
        }
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

    private static String displayMunicipalityName(String name) {
        if (name.equalsIgnoreCase("København")) return "Københavns Kommune";
        if (name.equalsIgnoreCase("Bornholm")) return "Bornholms Regionskommune";
        return name.endsWith("Kommune") ? name : name + " Kommune";
    }

    private void loadPickups() {
        if (busy || address == null) return;
        CalendarApi.Address requested = address;
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
                    pickups = finalResult; busy = false;
                    if (prefs().getBoolean(ENABLED, false)) scheduleReminders();
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
