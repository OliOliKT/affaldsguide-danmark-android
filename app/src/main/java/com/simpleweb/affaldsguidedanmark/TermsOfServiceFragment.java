package com.simpleweb.affaldsguidedanmark;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.graphics.Typeface;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class TermsOfServiceFragment  extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_terms_of_service, container, false);

        InfoPageText.set(v.findViewById(R.id.termsIntro), "terms.intro");
        LinearLayout sections = v.findViewById(R.id.termsSections);
        String[][] entries = {
                {"terms.sorting.title", "android.terms.sorting.body"},
                {"terms.external.title", "terms.external.body"},
                {"terms.corrections.title", "terms.corrections.body"},
                {"terms.responsibility.title", "terms.responsibility.body"}
        };
        for (String[] entry : entries) {
            TextView title = new TextView(requireContext());
            title.setTextColor(getResources().getColor(R.color.green_light));
            title.setTextSize(22);
            title.setTypeface(null, Typeface.BOLD);
            title.setPadding(dp(16), dp(16), dp(16), 0);
            InfoPageText.set(title, entry[0]);
            sections.addView(title);

            TextView body = new TextView(requireContext());
            body.setTextColor(getResources().getColor(R.color.text_color));
            body.setTextSize(16);
            body.setLineSpacing(dp(8), 1f);
            body.setPadding(dp(16), dp(12), dp(16), dp(12));
            InfoPageText.set(body, entry[1]);
            sections.addView(body);
        }

        return v;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
