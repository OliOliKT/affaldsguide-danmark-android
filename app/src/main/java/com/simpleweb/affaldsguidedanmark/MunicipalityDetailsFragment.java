package com.simpleweb.affaldsguidedanmark;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.graphics.Typeface;
import android.text.util.Linkify;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import java.util.List;

public class MunicipalityDetailsFragment extends Fragment {
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

        TextView nameTextView = view.findViewById(R.id.municipalityDetailName);
        TextView addressTextView = view.findViewById(R.id.municipalityDetailAddress);
        TextView emailTextView = view.findViewById(R.id.municipalityDetailEmail);
        TextView websiteTextView = view.findViewById(R.id.municipalityDetailWebsite);
        TextView descriptionTextView = view.findViewById(R.id.municipalityDetailDescription);
        LinearLayout extraInfoContainer = view.findViewById(R.id.municipalityExtraInfoContainer);
        ImageButton backButton = view.findViewById(R.id.municipalityBackButton);

        nameTextView.setText(municipality.getMunicipality());
        addressTextView.setText(municipality.getFullAddress());
        emailTextView.setText(municipality.getEmail());
        websiteTextView.setText(municipality.getUrl());
        boolean useEnglish = LanguageManager.isEnglish(requireContext());
        descriptionTextView.setText(municipality.getDescription(useEnglish));
        renderExtraInfo(extraInfoContainer, municipality.getDetails(), municipality.getWasteRules(useEnglish), useEnglish);

        Linkify.addLinks(emailTextView, Linkify.EMAIL_ADDRESSES);
        Linkify.addLinks(websiteTextView, Linkify.WEB_URLS);

        backButton.setOnClickListener(v -> Navigation.findNavController(view).navigateUp());
    }

    private void renderExtraInfo(LinearLayout container, Municipality.Details details, String wasteRules, boolean useEnglish) {
        container.removeAllViews();

        boolean hasWasteRules = wasteRules != null && !wasteRules.trim().isEmpty();
        boolean hasDetails = details != null;

        if (!hasWasteRules && !hasDetails) {
            container.setVisibility(View.GONE);
            return;
        }

        container.setVisibility(View.VISIBLE);
        addSectionTitle(container, useEnglish ? "Practical waste information" : "Praktisk affaldsinfo");

        if (hasWasteRules) {
            addExpandableTextCard(
                    container,
                    useEnglish ? "Municipality waste rules" : "Kommunens affaldsregler",
                    wasteRules,
                    useEnglish
            );
        }

        if (details == null) {
            return;
        }

        if (details.getQuickFacts() != null && !details.getQuickFacts().isEmpty()) {
            addSubTitle(container, useEnglish ? "Quick facts" : "Hurtige fakta", 24);
            addFactGrid(container, details.getQuickFacts(), useEnglish);
        }

        if (details.getSchemes() != null && !details.getSchemes().isEmpty()) {
            addSubTitle(container, useEnglish ? "Waste services" : "Affaldsordninger", 26);
            for (Municipality.Scheme scheme : details.getSchemes()) {
                addTextCard(container, scheme.getTitle(useEnglish), scheme.getDescription(useEnglish));
            }
        }

        if (details.getLinks() != null && !details.getLinks().isEmpty()) {
            addSubTitle(container, useEnglish ? "Useful official links" : "Nyttige officielle links", 26);
            for (Municipality.OfficialLink link : details.getLinks()) {
                addLinkCard(container, link.getTitle(useEnglish), link.getUrl(), useEnglish);
            }
        }

        String sourceNote = details.getSourceNote(useEnglish);
        if ((sourceNote != null && !sourceNote.isEmpty()) || (details.getLastChecked() != null && !details.getLastChecked().isEmpty())) {
            StringBuilder sourceBuilder = new StringBuilder();
            if (sourceNote != null && !sourceNote.isEmpty()) {
                sourceBuilder.append(useEnglish ? "Source: " : "Kilde: ").append(sourceNote);
            }
            if (details.getLastChecked() != null && !details.getLastChecked().isEmpty()) {
                if (sourceBuilder.length() > 0) {
                    sourceBuilder.append("\n\n");
                }
                sourceBuilder.append(useEnglish ? "Last checked: " : "Sidst tjekket: ").append(details.getLastChecked());
            }
            addSubTitle(container, useEnglish ? "Source and date" : "Kilde og dato", 26);
            addBodyCard(container, sourceBuilder.toString());
        }
    }

    private void addExpandableTextCard(LinearLayout container, String title, String rules, boolean useEnglish) {
        LinearLayout card = createCard();
        card.addView(createCardTitle(title, 0));
        TextView rulesTextView = createBody("", 8);
        TextView rulesToggle = createToggleTextView();
        String preview = getFirstSentences(rules, 2);
        boolean shouldCollapse = preview.length() < rules.trim().length();
        rulesTextView.setText(shouldCollapse ? preview : rules);
        card.addView(rulesTextView);

        if (shouldCollapse) {
            rulesToggle.setText(useEnglish ? "Read all local rules" : "Læs alle lokale regler");
            final boolean[] isOpen = {false};
            rulesToggle.setOnClickListener(v -> {
                isOpen[0] = !isOpen[0];
                rulesTextView.setText(isOpen[0] ? rules : preview);
                rulesToggle.setText(isOpen[0]
                        ? (useEnglish ? "Show less" : "Vis mindre")
                        : (useEnglish ? "Read all local rules" : "Læs alle lokale regler"));
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

    private void addSubTitle(LinearLayout container, String text, int topMarginDp) {
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

        container.addView(title);
    }

    private void addFactGrid(LinearLayout container, List<Municipality.QuickFact> facts, boolean useEnglish) {
        LinearLayout currentRow = null;

        for (int i = 0; i < facts.size(); i++) {
            if (i % 2 == 0) {
                currentRow = new LinearLayout(requireContext());
                currentRow.setOrientation(LinearLayout.HORIZONTAL);
                LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );
                rowParams.setMargins(0, dpToPx(10), 0, 0);
                currentRow.setLayoutParams(rowParams);
                container.addView(currentRow);
            }

            LinearLayout factCard = createCompactFactCard(
                    facts.get(i).getLabel(useEnglish),
                    facts.get(i).getValue(useEnglish)
            );
            LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    1
            );
            boolean isLeft = i % 2 == 0;
            cardParams.setMargins(isLeft ? 0 : dpToPx(6), 0, isLeft ? dpToPx(6) : 0, 0);
            factCard.setLayoutParams(cardParams);
            currentRow.addView(factCard);
        }

        if (facts.size() % 2 == 1 && currentRow != null) {
            View spacer = new View(requireContext());
            LinearLayout.LayoutParams spacerParams = new LinearLayout.LayoutParams(
                    0,
                    1,
                    1
            );
            spacerParams.setMargins(dpToPx(6), 0, 0, 0);
            spacer.setLayoutParams(spacerParams);
            currentRow.addView(spacer);
        }
    }

    private LinearLayout createCompactFactCard(String label, String value) {
        LinearLayout card = createCard();
        card.setPadding(dpToPx(12), dpToPx(12), dpToPx(12), dpToPx(12));

        TextView labelView = createCardTitle(label, 0);
        labelView.setTextSize(14);
        TextView valueView = createBody(value, 4);
        valueView.setTextSize(14);
        card.addView(labelView);
        card.addView(valueView);
        return card;
    }

    private void addFactCard(LinearLayout container, String label, String value) {
        LinearLayout card = createCard();
        TextView labelView = createCardTitle(label, 0);
        TextView valueView = createBody(value, 5);
        card.addView(labelView);
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
        TextView helperView = createBody(useEnglish ? "Open official page" : "Åbn officiel side", 5);
        helperView.setTextColor(getResources().getColor(R.color.green_light));

        card.addView(titleView);
        card.addView(helperView);
        container.addView(card);
    }

    private void openUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            return;
        }

        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        startActivity(intent);
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
}
