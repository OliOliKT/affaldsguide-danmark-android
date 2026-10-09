package com.simpleweb.affaldsguidedanmark;

import android.content.ActivityNotFoundException;
import android.Manifest;
import android.content.pm.PackageManager;
import android.content.Intent;
import android.content.res.Resources;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.graphics.Typeface;
import android.text.util.Linkify;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.HashSet;

public class MunicipalityDetailsFragment extends Fragment {
    private static final String TAG = "MunicipalityDetails";
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    private static final String[] FRACTIONS = {"Restaffald", "Madaffald", "Pap", "Plast", "Glas", "Farligt affald", "Papir", "Metal", "Tekstilaffald", "Mad- og drikkekartoner"};
    private static final String[] FRACTIONS_EN = {"Residual waste", "Food waste", "Cardboard", "Plastic", "Glass", "Hazardous waste", "Paper", "Metal", "Textile waste", "Food and beverage cartons"};
    private LinearLayout recyclingCentersContainer;
    private Municipality currentMunicipality;
    private boolean currentUseEnglish;
    private Location userLocation;
    private TextView findNearestButton;
    private TextView fractionOverviewTitle;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_municipality_details, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Municipality municipality = getArguments() != null ? getArguments().getParcelable("municipality") : null;
        if (municipality == null) {
            Navigation.findNavController(view).navigateUp();
            return;
        }
        AchievementStore.get(requireContext()).recordMunicipalityViewed();

        TextView nameTextView = view.findViewById(R.id.municipalityDetailName);
        TextView addressTextView = view.findViewById(R.id.municipalityDetailAddress);
        TextView emailTextView = view.findViewById(R.id.municipalityDetailEmail);
        TextView websiteTextView = view.findViewById(R.id.municipalityDetailWebsite);
        TextView descriptionTextView = view.findViewById(R.id.municipalityDetailDescription);
        TextView saveMunicipalityButton = view.findViewById(R.id.saveMunicipalityButton);
        LinearLayout extraInfoContainer = view.findViewById(R.id.municipalityExtraInfoContainer);
        ImageButton backButton = view.findViewById(R.id.municipalityBackButton);

        nameTextView.setText(municipality.getMunicipality());
        addressTextView.setText(municipality.getFullAddress());
        emailTextView.setText(municipality.getEmail());
        websiteTextView.setText(municipality.getUrl());
        boolean useEnglish = LanguageManager.usesNonDanishContent(requireContext());
        currentMunicipality = municipality;
        currentUseEnglish = useEnglish;
        descriptionTextView.setText(municipality.getDescription(useEnglish));
        renderExtraInfo(extraInfoContainer, municipality, useEnglish);
        if (getArguments() != null && getArguments().getBoolean("scrollToFractions") && fractionOverviewTitle != null) {
            androidx.core.widget.NestedScrollView scrollView = (androidx.core.widget.NestedScrollView) view;
            scrollView.post(() -> scrollView.smoothScrollTo(0,
                    extraInfoContainer.getTop() + fractionOverviewTitle.getTop()));
        }

        Linkify.addLinks(emailTextView, Linkify.EMAIL_ADDRESSES);
        Linkify.addLinks(websiteTextView, Linkify.WEB_URLS);

        updateSaveMunicipalityButton(saveMunicipalityButton, municipality);
        saveMunicipalityButton.setOnClickListener(v -> {
            if (SavedMunicipalityManager.isSaved(requireContext(), municipality.getMunicipality())) {
                SavedMunicipalityManager.remove(requireContext());
            } else {
                SavedMunicipalityManager.save(requireContext(), municipality.getMunicipality());
            }
            updateSaveMunicipalityButton(saveMunicipalityButton, municipality);
        });

        backButton.setOnClickListener(v -> Navigation.findNavController(view).navigateUp());
    }

    private void updateSaveMunicipalityButton(TextView button, Municipality municipality) {
        boolean isSaved = SavedMunicipalityManager.isSaved(requireContext(), municipality.getMunicipality());
        button.setText(isSaved ? R.string.fjern_gemt_kommune : R.string.gem_kommune);
        button.setTextColor(getResources().getColor(isSaved ? R.color.green_light : R.color.white));
        button.setBackgroundResource(isSaved
                ? R.drawable.remove_saved_municipality_button_background
                : R.drawable.saved_municipality_button_background);
    }

    private void renderExtraInfo(LinearLayout container, Municipality municipality, boolean useEnglish) {
        container.removeAllViews();

        Municipality.Details details = municipality.getDetails();
        String wasteRules = municipality.getWasteRules(useEnglish);
        List<RecyclingCenter> recyclingCenters = getRecyclingCentersForMunicipality(municipality.getMunicipality());
        boolean hasWasteRules = wasteRules != null && !wasteRules.trim().isEmpty();
        boolean hasDetails = details != null;

        if (!hasWasteRules && !hasDetails && recyclingCenters.isEmpty()) {
            container.setVisibility(View.GONE);
            return;
        }

        container.setVisibility(View.VISIBLE);

        if (hasWasteRules) {
            addExpandableTextCard(
                    container,
                    getString(R.string.kommunens_affaldsregler),
                    wasteRules,
                    useEnglish
            );
        }

        if (details != null && details.getWasteFractionCount() != null && details.getWasteFractionTotal() != null) {
            addFractionOverview(container, municipality, details, useEnglish);
        }

        if (details != null) {
            addPracticalWasteInformation(container, details, useEnglish);
        }

        if (details != null && details.getSchemes() != null && !details.getSchemes().isEmpty()) {
            addSubTitle(container, getString(R.string.municipal_waste_service_information), 26);
            for (Municipality.Scheme scheme : details.getSchemes()) {
                addTextCard(container,
                        municipalityText("municipality.label.", scheme.getTitle(false), scheme.getTitle(useEnglish)),
                        municipalityText("municipality.scheme.", scheme.getDescription(false), scheme.getDescription(useEnglish)));
            }
        }

        if (details != null && details.getLinks() != null && !details.getLinks().isEmpty()) {
            addSubTitle(container, getString(R.string.useful_official_links), 26);
            for (Municipality.OfficialLink link : details.getLinks()) {
                addLinkCard(container, municipalityText("municipality.label.", link.getTitle(false), link.getTitle(useEnglish)), link.getUrl(), useEnglish);
            }
        }

        if (details != null) {
            String sourceNote = municipalityText("municipality.source.", details.getSourceNote(false), details.getSourceNote(useEnglish));
            if ((sourceNote != null && !sourceNote.isEmpty()) || (details.getLastChecked() != null && !details.getLastChecked().isEmpty())) {
                StringBuilder sourceBuilder = new StringBuilder();
                if (sourceNote != null && !sourceNote.isEmpty()) {
                    sourceBuilder.append(getString(R.string.source_prefix).trim()).append(' ').append(sourceNote);
                }
                if (details.getLastChecked() != null && !details.getLastChecked().isEmpty()) {
                    if (sourceBuilder.length() > 0) {
                        sourceBuilder.append("\n\n");
                    }
                    sourceBuilder.append(getString(R.string.last_checked_prefix).trim()).append(' ').append(details.getLastChecked());
                }
                addSubTitle(container, getString(R.string.source_and_date), 26);
                addBodyCard(container, sourceBuilder.toString());
            }
        }

        if (!recyclingCenters.isEmpty()) {
            addSubTitle(container, getString(R.string.recycling_centres_count), 26);
            recyclingCentersContainer = container;
            addFindNearestButton(container, useEnglish);
            for (RecyclingCenter recyclingCenter : recyclingCenters) {
                addRecyclingCenterCard(container, recyclingCenter, useEnglish, userLocation);
            }
        }
    }

    private void addFindNearestButton(LinearLayout container, boolean useEnglish) {
        TextView button = createToggleTextView();
        button.setText(getString(R.string.find_naermeste_genbrugsplads));
        button.setOnClickListener(v -> requestUserLocation());
        findNearestButton = button;
        container.addView(button);
    }

    private void requestUserLocation() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE
            );
            return;
        }

        findUserLocation();
    }

    private void findUserLocation() {
        if (recyclingCentersContainer == null) {
            return;
        }

        Toast.makeText(requireContext(), R.string.finder_placering, Toast.LENGTH_SHORT).show();
        LocationManager locationManager = (LocationManager) requireContext().getSystemService(android.content.Context.LOCATION_SERVICE);
        LocationListener listener = new LocationListener() {
            @Override
            public void onLocationChanged(@NonNull Location location) {
                userLocation = location;
                locationManager.removeUpdates(this);
                renderRecyclingCenters();
            }
        };

        try {
            Location lastKnown = null;
            for (String provider : locationManager.getProviders(true)) {
                Location candidate = locationManager.getLastKnownLocation(provider);
                if (candidate != null && (lastKnown == null || candidate.getTime() > lastKnown.getTime())) {
                    lastKnown = candidate;
                }
            }

            if (lastKnown != null) {
                userLocation = lastKnown;
                renderRecyclingCenters();
                return;
            }

            locationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    0L,
                    0f,
                    listener
            );
        } catch (SecurityException | IllegalArgumentException e) {
            Toast.makeText(requireContext(), R.string.placering_ikke_tilgængelig, Toast.LENGTH_LONG).show();
            Log.w(TAG, "Unable to obtain user location", e);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != LOCATION_PERMISSION_REQUEST_CODE) {
            return;
        }

        boolean granted = false;
        for (int result : grantResults) {
            if (result == PackageManager.PERMISSION_GRANTED) {
                granted = true;
                break;
            }
        }

        if (granted) {
            findUserLocation();
        } else {
            Toast.makeText(requireContext(), R.string.placeringstilladelse_påkrævet, Toast.LENGTH_LONG).show();
        }
    }

    private void renderRecyclingCenters() {
        if (recyclingCentersContainer == null || currentMunicipality == null) {
            return;
        }

        List<RecyclingCenter> centers = getRecyclingCentersForMunicipality(currentMunicipality.getMunicipality());
        centers.sort((first, second) -> {
            if (userLocation == null || !first.hasCoordinates()) return first.hasCoordinates() ? 1 : 0;
            if (!second.hasCoordinates()) return -1;
            return Double.compare(distanceInKilometres(first), distanceInKilometres(second));
        });

        int firstCenterIndex = recyclingCentersContainer.indexOfChild(findNearestButton) + 1;
        recyclingCentersContainer.removeViews(
                firstCenterIndex,
                recyclingCentersContainer.getChildCount() - firstCenterIndex
        );
        for (RecyclingCenter center : centers) {
            addRecyclingCenterCard(recyclingCentersContainer, center, currentUseEnglish, userLocation);
        }

        if (userLocation != null && !centers.isEmpty() && centers.get(0).hasCoordinates()) {
            openRecyclingCenterInMaps(centers.get(0));
        }
    }

    private void addExpandableTextCard(LinearLayout container, String title, String rules, boolean useEnglish) {
        LinearLayout card = createCard();
        TextView titleView = createCardTitle(title, 0);
        titleView.setTextSize(19);
        card.addView(titleView);
        TextView rulesTextView = createBody("", 8);
        TextView rulesToggle = createToggleTextView();
        String preview = getFirstSentences(rules, 2);
        boolean shouldCollapse = preview.length() < rules.trim().length();
        rulesTextView.setText(shouldCollapse ? preview : rules);
        card.addView(rulesTextView);

        if (shouldCollapse) {
            rulesToggle.setText(R.string.read_all_local_rules);
            final boolean[] isOpen = {false};
            rulesToggle.setOnClickListener(v -> {
                isOpen[0] = !isOpen[0];
                rulesTextView.setText(isOpen[0] ? rules : preview);
                rulesToggle.setText(isOpen[0] ? R.string.show_less : R.string.read_all_local_rules);
            });
            card.addView(rulesToggle);
        }

        container.addView(card);
    }

    private void addSectionTitle(LinearLayout container, String text) {
        TextView title = new TextView(requireContext());
        title.setText(text);
        title.setTextColor(getResources().getColor(R.color.green_light));
        title.setTextSize(19);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setLineSpacing(dpToPx(2), 1.0f);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, dpToPx(16), 0, 0);
        title.setLayoutParams(params);

        container.addView(title);
    }

    private TextView addSubTitle(LinearLayout container, String text, int topMarginDp) {
        TextView title = new TextView(requireContext());
        title.setText(text);
        title.setTextColor(getResources().getColor(R.color.green_light));
        title.setTextSize(19);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setLineSpacing(dpToPx(2), 1.0f);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, dpToPx(topMarginDp), 0, 0);
        title.setLayoutParams(params);

        container.addView(title);
        return title;
    }

    private void addPracticalWasteInformation(LinearLayout container, Municipality.Details details, boolean useEnglish) {
        String operator = details.getWasteOperator(useEnglish);
        String officialPage = municipalityText("municipality.label.", details.getOfficialWastePageTitle(false), details.getOfficialWastePageTitle(useEnglish));
        String centers = details.getRecyclingCenters(useEnglish);
        if (LanguageManager.isArabic(requireContext()) && hasText(details.getRecyclingCenters(false))) {
            String count = details.getRecyclingCenters(false).replaceAll("[^0-9]", "");
            if (!count.isEmpty()) {
                centers = ArabicText.value(getResources(), "municipality.recyclingCenterCount", "{0} من مراكز إعادة التدوير")
                        .replace("{0}", count);
            }
        }
        String selfServiceTitle = municipalityText("municipality.label.", details.getDigitalSelfServiceTitle(false), details.getDigitalSelfServiceTitle(useEnglish));
        String selfService = municipalityText("municipality.detail.", details.getDigitalSelfService(false), details.getDigitalSelfService(useEnglish));

        if (!hasText(operator) && !hasText(details.getOfficialWastePageUrl())
                && !hasText(centers) && (!hasText(selfServiceTitle) || !hasText(selfService))) {
            return;
        }

        addSectionTitle(container, getString(R.string.practical_waste_info));
        if (hasText(operator)) {
            addPracticalInfoCard(container, getString(R.string.waste_operator), operator, null);
        }
        if (hasText(details.getOfficialWastePageUrl())) {
            addPracticalInfoCard(container, getString(R.string.official_waste_page),
                    hasText(officialPage) ? officialPage : getString(R.string.official_waste_page),
                    details.getOfficialWastePageUrl());
        }
        if (hasText(centers)) {
            addPracticalInfoCard(container, getString(R.string.recycling_centres_count), centers, null);
        }
        if (hasText(selfServiceTitle) && hasText(selfService)) {
            addPracticalInfoCard(container, selfServiceTitle, selfService, details.getDigitalSelfServiceUrl());
        }
    }

    private String municipalityText(String prefix, String danish, String fallback) {
        if (LanguageManager.isArabic(requireContext()) && danish != null) {
            return ArabicText.value(getResources(), prefix + danish, fallback);
        }
        return fallback;
    }

    private void addFractionOverview(LinearLayout container, Municipality municipality, Municipality.Details details, boolean useEnglish) {
        int count = details.getWasteFractionCount();
        int total = details.getWasteFractionTotal();
        fractionOverviewTitle = addSubTitle(container, getString(R.string.waste_fractions_overview, count, total), 26);

        StringBuilder source = new StringBuilder();
        source.append(municipality.getWasteRules()).append(' ').append(municipality.getWasteRules(true));
        if (details.getSchemes() != null) {
            for (Municipality.Scheme scheme : details.getSchemes()) {
                source.append(' ').append(scheme.getTitle(false)).append(' ').append(scheme.getDescription(false));
                source.append(' ').append(scheme.getTitle(true)).append(' ').append(scheme.getDescription(true));
            }
        }
        String normalized = Normalizer.normalize(source.toString().toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "").replace('ø', 'o').replace("æ", "ae");
        String[] words = normalized.split("[^\\p{L}]+");
        Set<Integer> detected = new HashSet<>();
        for (String word : words) {
            if (word.startsWith("restaffald") || word.startsWith("dagrenovation")) detected.add(0);
            if (word.startsWith("madaffald") || word.startsWith("bioaffald")) detected.add(1);
            if (word.equals("pap")) detected.add(2);
            if (word.startsWith("plast")) detected.add(3);
            if (word.startsWith("glas")) detected.add(4);
            if (word.startsWith("miljoboks") || word.startsWith("miljokasse")) detected.add(5);
            if (word.startsWith("papir")) detected.add(6);
            if (word.startsWith("metal")) detected.add(7);
            if (word.startsWith("tekstil")) detected.add(8);
            if (word.startsWith("karton")) detected.add(9);
        }
        if (normalized.contains("farligt affald")) detected.add(5);

        boolean hasUnconfirmed = false;
        for (int i = 0; i < FRACTIONS.length; i += 2) {
            LinearLayout row = new LinearLayout(requireContext());
            row.setOrientation(LinearLayout.HORIZONTAL);
            LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            rowParams.setMargins(0, dpToPx(8), 0, 0);
            row.setLayoutParams(rowParams);
            for (int j = i; j < Math.min(i + 2, FRACTIONS.length); j++) {
                int status = count >= FRACTIONS.length || detected.contains(j) ? 0 : detected.size() == count ? 1 : 2;
                if (status == 2) hasUnconfirmed = true;
                row.addView(createFractionTile(j, status, useEnglish));
            }
            container.addView(row);
        }
        if (hasUnconfirmed) {
            TextView note = createBody(getString(R.string.fraction_unconfirmed_explanation), 8);
            container.addView(note);
        }
    }

    private View createFractionTile(int index, int status, boolean useEnglish) {
        LinearLayout tile = new LinearLayout(requireContext());
        tile.setOrientation(LinearLayout.HORIZONTAL);
        tile.setGravity(android.view.Gravity.CENTER_VERTICAL);
        tile.setBackgroundResource(R.drawable.details_info_background);
        tile.setPadding(dpToPx(10), dpToPx(10), dpToPx(10), dpToPx(10));
        LinearLayout.LayoutParams tileParams = new LinearLayout.LayoutParams(0, dpToPx(76), 1);
        tileParams.setMargins(dpToPx(4), 0, dpToPx(4), 0);
        tile.setLayoutParams(tileParams);

        ImageView image = new ImageView(requireContext());
        image.setImageResource(TrashDB.getImageResourceForKey(FRACTIONS[index], useEnglish));
        image.setScaleType(ImageView.ScaleType.FIT_CENTER);
        image.setAlpha(status == 0 ? 1f : 0.5f);
        tile.addView(image, new LinearLayout.LayoutParams(dpToPx(48), dpToPx(48)));

        LinearLayout labels = new LinearLayout(requireContext());
        labels.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams labelsParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
        labelsParams.setMargins(dpToPx(8), 0, 0, 0);
        tile.addView(labels, labelsParams);
        TextView name = new TextView(requireContext());
        name.setText(LanguageManager.isArabic(requireContext())
                ? ArabicText.value(getResources(), "category." + FRACTIONS[index], FRACTIONS_EN[index])
                : useEnglish ? FRACTIONS_EN[index] : FRACTIONS[index]);
        name.setTextColor(getResources().getColor(R.color.text_color));
        name.setTextSize(13);
        name.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        labels.addView(name);
        TextView statusText = new TextView(requireContext());
        statusText.setText(status == 0 ? R.string.fraction_used : status == 1 ? R.string.fraction_not_used : R.string.fraction_unconfirmed);
        statusText.setTextColor(getResources().getColor(status == 0 ? R.color.green_light : R.color.grey_black));
        statusText.setTextSize(12);
        labels.addView(statusText);
        tile.setContentDescription(name.getText() + ", " + statusText.getText());
        return tile;
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private void addPracticalInfoCard(LinearLayout container, String label, String value, String url) {
        LinearLayout card = createCard();
        card.addView(createCardTitle(label, 0));
        TextView valueView = createBody(value, 5);
        if (hasText(url)) {
            valueView.setTextColor(getResources().getColor(R.color.green_light));
            card.setClickable(true);
            card.setFocusable(true);
            card.setOnClickListener(v -> openUrl(url));
        }
        card.addView(valueView);
        container.addView(card);
    }

    private void addTextCard(LinearLayout container, String title, String body) {
        LinearLayout card = createCard();
        card.addView(createCardTitle(title, 0));
        card.addView(createBody(body, 8));
        container.addView(card);
    }

    private LinearLayout wrapInCard(View contentView) {
        LinearLayout card = createCard();
        card.addView(contentView);
        return card;
    }

    private void addLinkCard(LinearLayout container, String title, String url, boolean useEnglish) {
        LinearLayout card = createCard();
        card.setClickable(true);
        card.setFocusable(true);
        card.setOnClickListener(v -> openUrl(url));

        TextView titleView = createCardTitle(title, 0);
        TextView helperView = createBody(getString(R.string.open_official_page), 5);
        helperView.setTextColor(getResources().getColor(R.color.green_light));

        card.addView(titleView);
        card.addView(helperView);
        container.addView(card);
    }

    private void addRecyclingCenterCard(
            LinearLayout container,
            RecyclingCenter recyclingCenter,
            boolean useEnglish,
            Location userLocation
    ) {
        LinearLayout card = createCard();
        card.setClickable(true);
        card.setFocusable(true);
        card.setOnClickListener(v -> openRecyclingCenterInMaps(recyclingCenter));

        TextView titleView = createCardTitle(recyclingCenter.name, 0);
        TextView addressView = createBody(recyclingCenter.address, 5);
        TextView helperView = createBody(getString(R.string.show_on_map), 8);
        helperView.setTextColor(getResources().getColor(R.color.green_light));
        helperView.setTypeface(Typeface.DEFAULT, Typeface.BOLD);

        card.addView(titleView);
        card.addView(addressView);
        if (userLocation != null && recyclingCenter.hasCoordinates()) {
            String distance = formatDistance(distanceInKilometres(recyclingCenter), useEnglish);
            card.addView(createBody(
                    getString(R.string.afstand_fra_dig, distance),
                    5
            ));
        }
        card.addView(helperView);
        container.addView(card);
    }

    private double distanceInKilometres(RecyclingCenter recyclingCenter) {
        float[] results = new float[1];
        Location.distanceBetween(
                userLocation.getLatitude(),
                userLocation.getLongitude(),
                recyclingCenter.lat,
                recyclingCenter.lng,
                results
        );
        return results[0] / 1000.0;
    }

    private String formatDistance(double distance, boolean useEnglish) {
        String formatted = String.format(
                java.util.Locale.getDefault(),
                distance < 10 ? "%.1f km" : "%.0f km",
                distance
        );
        return formatted;
    }

    private void openUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            return;
        }

        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Log.w(TAG, "No app available to open URL", e);
        }
    }

    private void openRecyclingCenterInMaps(RecyclingCenter recyclingCenter) {
        String label = recyclingCenter.name + ", " + recyclingCenter.address;
        Uri mapUri;

        if (recyclingCenter.hasCoordinates()) {
            mapUri = Uri.parse("geo:" + recyclingCenter.lat + "," + recyclingCenter.lng + "?q="
                    + Uri.encode(recyclingCenter.lat + "," + recyclingCenter.lng + "(" + label + ")"));
        } else {
            mapUri = Uri.parse("geo:0,0?q=" + Uri.encode(label));
        }

        Intent intent = new Intent(Intent.ACTION_VIEW, mapUri);
        try {
            startActivity(intent);
            AchievementStore.get(requireContext()).recordRecyclingCenterMap(
                    recyclingCenter.municipality + ":" + recyclingCenter.name);
        } catch (ActivityNotFoundException e) {
            Log.w(TAG, "No map app available to open recycling centre", e);
        }
    }

    private void addBodyCard(LinearLayout container, String body) {
        LinearLayout card = createCard();
        card.addView(createBody(body, 0));
        container.addView(card);
    }

    private LinearLayout createCard() {
        LinearLayout card = new LinearLayout(requireContext());
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundResource(R.drawable.details_info_background);
        card.setPadding(dpToPx(16), dpToPx(15), dpToPx(16), dpToPx(16));

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, dpToPx(12), 0, 0);
        card.setLayoutParams(params);
        return card;
    }

    private TextView createCardTitle(String text, int topMarginDp) {
        TextView title = new TextView(requireContext());
        title.setText(text);
        title.setTextColor(getResources().getColor(R.color.green_light));
        title.setTextSize(16);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setLineSpacing(dpToPx(2), 1.0f);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, dpToPx(topMarginDp), 0, 0);
        title.setLayoutParams(params);

        return title;
    }

    private TextView createBody(String text, int topMarginDp) {
        TextView body = new TextView(requireContext());
        body.setText(text);
        body.setTextColor(getResources().getColor(R.color.text_color));
        body.setTextSize(15);
        body.setLineSpacing(dpToPx(4), 1.0f);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, dpToPx(topMarginDp), 0, 0);
        body.setLayoutParams(params);

        return body;
    }

    private TextView createToggleTextView() {
        TextView toggle = new TextView(requireContext());
        toggle.setTextColor(getResources().getColor(R.color.green_light));
        toggle.setTextSize(15);
        toggle.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        toggle.setGravity(android.view.Gravity.CENTER);
        toggle.setBackgroundResource(R.drawable.result_description_background);
        toggle.setPadding(dpToPx(14), dpToPx(10), dpToPx(14), dpToPx(10));
        toggle.setMinHeight(dpToPx(44));

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, dpToPx(12), 0, 0);
        toggle.setLayoutParams(params);

        return toggle;
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    private String getFirstSentences(String text, int sentenceCount) {
        String trimmed = text.trim();
        int endIndex = -1;
        int found = 0;

        for (int i = 0; i < trimmed.length(); i++) {
            char current = trimmed.charAt(i);
            if (current == '.' || current == '!' || current == '?') {
                found++;
                endIndex = i;
                if (found >= sentenceCount) {
                    break;
                }
            }
        }

        if (endIndex == -1 || found < sentenceCount) {
            return trimmed;
        }

        return trimmed.substring(0, endIndex + 1);
    }

    private List<RecyclingCenter> getRecyclingCentersForMunicipality(String municipalityName) {
        if (municipalityName == null || municipalityName.trim().isEmpty()) {
            return Collections.emptyList();
        }

        List<RecyclingCenter> allCenters = loadRecyclingCenters(getResources());
        List<RecyclingCenter> matchingCenters = new ArrayList<>();

        for (RecyclingCenter center : allCenters) {
            if (municipalityName.equals(center.municipality)) {
                matchingCenters.add(center);
            }
        }

        matchingCenters.sort(Comparator.comparing(center -> center.name, String.CASE_INSENSITIVE_ORDER));
        return matchingCenters;
    }

    private List<RecyclingCenter> loadRecyclingCenters(Resources resources) {
        try (InputStream inputStream = resources.openRawResource(R.raw.genbrugspladser_data);
             InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
            Gson gson = new Gson();
            Type listType = new TypeToken<List<RecyclingCenter>>() {}.getType();
            return gson.fromJson(reader, listType);
        } catch (Exception e) {
            Log.e(TAG, "Error loading recycling centre JSON file", e);
            return Collections.emptyList();
        }
    }

    private static class RecyclingCenter {
        @SerializedName("Navn")
        String name;

        @SerializedName("Adresse")
        String address;

        @SerializedName("Kommune")
        String municipality;

        @SerializedName("lat")
        Double lat;

        @SerializedName("lng")
        Double lng;

        boolean hasCoordinates() {
            return lat != null && lng != null;
        }
    }
}
