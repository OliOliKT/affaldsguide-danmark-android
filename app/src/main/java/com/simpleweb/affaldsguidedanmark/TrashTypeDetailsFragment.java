package com.simpleweb.affaldsguidedanmark;

import android.os.Bundle;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.text.HtmlCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TrashTypeDetailsFragment extends Fragment {

    private static final String ARG_TRASH_TYPE = "trashType";
    private static final String ARG_SELECTED_TRASH_GROUP = "selectedTrashGroup";

    private RecyclerView listTrash;
    private TrashAdapter trashAdapter;
    public static TrashTypeDetailsFragment newInstance(TrashType trashType) {
        TrashTypeDetailsFragment fragment = new TrashTypeDetailsFragment();
        Bundle args = new Bundle();
        args.putParcelable(ARG_TRASH_TYPE, trashType);
        args.putString(ARG_SELECTED_TRASH_GROUP, trashType.getNavn());
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_trash_type_details, container, false);

        TextView nameTextView = view.findViewById(R.id.detail_name);
        TextView descriptionTextView = view.findViewById(R.id.affaldsfraktionBeskrivelse);
        LinearLayout extendedDescriptionContainer = view.findViewById(R.id.affaldsfraktionUdvidetBeskrivelseContainer);
        ImageView imageView = view.findViewById(R.id.affaldsfraktionBillede);
        TextView pros_description = view.findViewById(R.id.pros_description);
        TextView cons_description = view.findViewById(R.id.cons_description);
        TextView DBAffaldTitel = view.findViewById(R.id.all_items_title);
        LinearLayout trashCategoriesLayout = view.findViewById(R.id.trashCategoriesLayout);
        LinearLayout enhancementContainer = view.findViewById(R.id.fractionEnhancementContainer);
        LinearLayout practicalContainer = view.findViewById(R.id.fractionPracticalContainer);
        LinearLayout faqContainer = view.findViewById(R.id.fractionFaqContainer);

        listTrash = view.findViewById(R.id.listTrash);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
        listTrash.setLayoutManager(layoutManager);
        trashAdapter = new TrashAdapter();
        listTrash.setAdapter(trashAdapter);

        TrashType trashType = getArguments().getParcelable(ARG_TRASH_TYPE);

        if (trashType != null) {
            nameTextView.setText(trashType.getNavn());
            descriptionTextView.setText(trashType.getBeskrivelse());
            imageView.setImageResource(trashType.getImageResId());

            boolean useEnglish = LanguageManager.isEnglish(requireContext());
            FractionEnhancement enhancement = FractionEnhancement.get(getResources(), trashType.getDanishNavn(), useEnglish);
            renderEnhancement(enhancementContainer, practicalContainer, faqContainer, enhancement);
            descriptionTextView.setVisibility(enhancement != null ? View.GONE : View.VISIBLE);
            trashCategoriesLayout.setVisibility(enhancement != null ? View.GONE : View.VISIBLE);

            if (trashType.getUdvidetBeskrivelse() != null && !trashType.getUdvidetBeskrivelse().isEmpty()) {
                renderExtendedDescription(extendedDescriptionContainer, trashType.getUdvidetBeskrivelse());
            } else {
                extendedDescriptionContainer.setVisibility(View.GONE);
            }

            List<String> prosList = trashType.getPros();
            if (prosList != null && !prosList.isEmpty()) {
                pros_description.setText(formatList(prosList));
            } else {
                pros_description.setText("");
            }

            List<String> consList = trashType.getCons();
            if (consList != null && !consList.isEmpty()) {
                cons_description.setText(formatList(consList));
            } else {
                cons_description.setText("");
            }

        }

        ImageButton backButton = view.findViewById(R.id.backButton);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Navigation.findNavController(view).navigateUp();
            }
        });


        String selectedTrashGroup = getArguments().getString(ARG_SELECTED_TRASH_GROUP);

        TrashDB trashDB = new TrashDB(getResources());
        boolean useEnglish = LanguageManager.isEnglish(requireContext());
        String sortingLookupName = trashType != null ? trashType.getDanishNavn() : selectedTrashGroup;
        List<String> trashProducts = trashDB.getProductNamesForCategory(sortingLookupName, useEnglish);
        trashAdapter.setTrashNames(trashProducts);
        trashAdapter.notifyDataSetChanged();

        if (useEnglish) {
            DBAffaldTitel.setText("Waste items sorted as " + (trashType != null ? trashType.getNavn() : selectedTrashGroup) + ":");
        } else {
            DBAffaldTitel.setText("Alle genstande som sorteres i " + selectedTrashGroup + ":");
        }


        return view;
    }

    private void renderEnhancement(LinearLayout enhancementContainer, LinearLayout practicalContainer, LinearLayout faqContainer, FractionEnhancement enhancement) {
        enhancementContainer.removeAllViews();
        practicalContainer.removeAllViews();
        faqContainer.removeAllViews();

        if (enhancement == null) {
            enhancementContainer.setVisibility(View.GONE);
            practicalContainer.setVisibility(View.GONE);
            faqContainer.setVisibility(View.GONE);
            return;
        }

        enhancementContainer.setVisibility(View.VISIBLE);
        addSectionCard(enhancementContainer, enhancement.summaryTitle, enhancement.summary);

        practicalContainer.setVisibility(View.VISIBLE);
        addListCard(practicalContainer, enhancement.mistakesTitle, enhancement.getMistakes());
        addRuleCard(practicalContainer, enhancement);
        addSectionCard(practicalContainer, enhancement.municipalityTitle,
                enhancement.municipalityNote + " " + enhancement.municipalityLinkText + ".");

        if (!enhancement.getFaqs().isEmpty()) {
            faqContainer.setVisibility(View.VISIBLE);
            addFaqCard(faqContainer, enhancement);
        } else {
            faqContainer.setVisibility(View.GONE);
        }
    }

    private void addSectionCard(LinearLayout container, String title, String body) {
        LinearLayout card = createCard();
        boolean hasTitle = title != null && !title.trim().isEmpty();
        if (hasTitle) {
            card.addView(createTitle(title));
        }
        card.addView(createBody(body, hasTitle ? 8 : 0));
        container.addView(card);
    }

    private void addListCard(LinearLayout container, String title, List<String> items) {
        LinearLayout card = createCard();
        card.addView(createTitle(title));

        for (String item : items) {
            card.addView(createBulletBody(item, 8));
        }

        container.addView(card);
    }

    private void renderExtendedDescription(LinearLayout container, String description) {
        container.removeAllViews();

        List<DescriptionSection> sections = parseDescriptionSections(description);
        if (sections.isEmpty()) {
            container.setVisibility(View.GONE);
            return;
        }

        container.setVisibility(View.VISIBLE);
        for (DescriptionSection section : sections) {
            addSectionCard(container, section.title, section.body);
        }
    }

    private void addRuleCard(LinearLayout container, FractionEnhancement enhancement) {
        LinearLayout card = createCard();
        card.addView(createTitle(enhancement.cleanDirtyTitle));
        card.addView(createSubtitle(enhancement.cleanRuleTitle, 12));
        card.addView(createBody(enhancement.cleanRule, 4));
        card.addView(createSubtitle(enhancement.dirtyRuleTitle, 14));
        card.addView(createBody(enhancement.dirtyRule, 4));
        container.addView(card);
    }

    private void addFaqCard(LinearLayout container, FractionEnhancement enhancement) {
        LinearLayout card = createCard();
        card.addView(createTitle(enhancement.faqTitle));

        for (FractionEnhancement.Faq faq : enhancement.getFaqs()) {
            card.addView(createSubtitle(faq.question, 14));
            card.addView(createBody(faq.answer, 4));
        }

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
        params.setMargins(0, 0, 0, dpToPx(12));
        card.setLayoutParams(params);

        return card;
    }

    private TextView createTitle(String text) {
        TextView title = new TextView(requireContext());
        title.setText(text);
        title.setTextColor(getResources().getColor(R.color.green_light));
        title.setTextSize(19);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setLineSpacing(dpToPx(2), 1.0f);
        return title;
    }

    private TextView createSubtitle(String text, int topMarginDp) {
        TextView subtitle = new TextView(requireContext());
        subtitle.setText(text);
        subtitle.setTextColor(getResources().getColor(R.color.text_color));
        subtitle.setTextSize(16);
        subtitle.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        subtitle.setLineSpacing(dpToPx(2), 1.0f);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, dpToPx(topMarginDp), 0, 0);
        subtitle.setLayoutParams(params);

        return subtitle;
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

    private LinearLayout createBulletBody(String text, int topMarginDp) {
        LinearLayout row = new LinearLayout(requireContext());
        row.setOrientation(LinearLayout.HORIZONTAL);

        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        rowParams.setMargins(0, dpToPx(topMarginDp), 0, 0);
        row.setLayoutParams(rowParams);

        TextView bullet = new TextView(requireContext());
        bullet.setText("•");
        bullet.setTextColor(getResources().getColor(R.color.green_light));
        bullet.setTextSize(15);
        bullet.setTypeface(Typeface.DEFAULT, Typeface.BOLD);

        LinearLayout.LayoutParams bulletParams = new LinearLayout.LayoutParams(
                dpToPx(14),
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        bullet.setLayoutParams(bulletParams);
        row.addView(bullet);

        TextView body = new TextView(requireContext());
        body.setText(text);
        body.setTextColor(getResources().getColor(R.color.text_color));
        body.setTextSize(15);
        body.setLineSpacing(dpToPx(4), 1.0f);

        LinearLayout.LayoutParams bodyParams = new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1
        );
        body.setLayoutParams(bodyParams);
        row.addView(body);

        return row;
    }

    private List<DescriptionSection> parseDescriptionSections(String description) {
        List<DescriptionSection> sections = new ArrayList<>();
        Pattern pattern = Pattern.compile("(?is)<h2[^>]*>(.*?)</h2>\\s*<p[^>]*>(.*?)</p>");
        Matcher matcher = pattern.matcher(description);

        int firstMatchStart = -1;
        while (matcher.find()) {
            if (firstMatchStart == -1) {
                firstMatchStart = matcher.start();
                addLeadingParagraphs(sections, description.substring(0, firstMatchStart));
            }

            String title = toPlainText(matcher.group(1));
            String body = toPlainText(matcher.group(2));
            if (!body.isEmpty()) {
                sections.add(new DescriptionSection(title, body));
            }
        }

        if (sections.isEmpty()) {
            addLeadingParagraphs(sections, description);
        }

        return sections;
    }

    private void addLeadingParagraphs(List<DescriptionSection> sections, String html) {
        Pattern paragraphPattern = Pattern.compile("(?is)<p[^>]*>(.*?)</p>");
        Matcher paragraphMatcher = paragraphPattern.matcher(html);

        while (paragraphMatcher.find()) {
            String body = toPlainText(paragraphMatcher.group(1));
            if (!body.isEmpty()) {
                sections.add(new DescriptionSection(null, body));
            }
        }
    }

    private String toPlainText(String html) {
        if (html == null) {
            return "";
        }

        return HtmlCompat.fromHtml(html, HtmlCompat.FROM_HTML_MODE_COMPACT)
                .toString()
                .replace('\u00A0', ' ')
                .trim();
    }

    private String formatList(List<String> items) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < items.size(); i++) {
            builder.append("• ").append(items.get(i));
            if (i < items.size() - 1) {
                builder.append("\n\n");
            }
        }
        return builder.toString();
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    private static class DescriptionSection {
        final String title;
        final String body;

        DescriptionSection(String title, String body) {
            this.title = title;
            this.body = body;
        }
    }

}
