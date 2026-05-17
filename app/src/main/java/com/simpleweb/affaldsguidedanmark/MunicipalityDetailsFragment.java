package com.simpleweb.affaldsguidedanmark;

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
        TextView rulesTextView = view.findViewById(R.id.municipalityDetailWasteRules);
        LinearLayout extraInfoContainer = view.findViewById(R.id.municipalityExtraInfoContainer);
        ImageButton backButton = view.findViewById(R.id.municipalityBackButton);

        nameTextView.setText(municipality.getMunicipality());
        addressTextView.setText(municipality.getFullAddress());
        emailTextView.setText(municipality.getEmail());
        websiteTextView.setText(municipality.getUrl());
        boolean useEnglish = LanguageManager.isEnglish(requireContext());
        descriptionTextView.setText(municipality.getDescription(useEnglish));
        rulesTextView.setText(municipality.getWasteRules(useEnglish));
        renderExtraInfo(extraInfoContainer, municipality.getDetails(), useEnglish);

        Linkify.addLinks(emailTextView, Linkify.EMAIL_ADDRESSES);
        Linkify.addLinks(websiteTextView, Linkify.WEB_URLS);

        backButton.setOnClickListener(v -> Navigation.findNavController(view).navigateUp());
    }

    private void renderExtraInfo(LinearLayout container, Municipality.Details details, boolean useEnglish) {
        container.removeAllViews();

        if (details == null) {
            container.setVisibility(View.GONE);
            return;
        }

        container.setVisibility(View.VISIBLE);
        addSectionTitle(container, useEnglish ? "Practical waste information" : "Praktisk affaldsinfo");

        if (details.getQuickFacts() != null && !details.getQuickFacts().isEmpty()) {
            addSubTitle(container, useEnglish ? "Quick facts" : "Hurtige fakta", 12);
            for (Municipality.QuickFact fact : details.getQuickFacts()) {
                addFactCard(container, fact.getLabel(useEnglish), fact.getValue(useEnglish));
            }
        }

        if (details.getSchemes() != null && !details.getSchemes().isEmpty()) {
            for (Municipality.Scheme scheme : details.getSchemes()) {
                addTextCard(container, scheme.getTitle(useEnglish), scheme.getDescription(useEnglish));
            }
        }

        if (details.getLinks() != null && !details.getLinks().isEmpty()) {
            addSubTitle(container, useEnglish ? "Official links" : "Officielle links", 12);
            for (Municipality.OfficialLink link : details.getLinks()) {
                TextView linkView = createBody(link.getTitle(useEnglish) + "\n" + link.getUrl(), 8);
                linkView.setTextColor(getResources().getColor(R.color.green_light));
                linkView.setAutoLinkMask(Linkify.WEB_URLS);
                linkView.setLinksClickable(true);
                container.addView(wrapInCard(linkView));
                Linkify.addLinks(linkView, Linkify.WEB_URLS);
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
            addTextCard(container, useEnglish ? "Source and date" : "Kilde og dato", sourceBuilder.toString());
        }
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

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }
}
