package com.simpleweb.affaldsguidedanmark;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import java.util.ArrayList;
import java.util.List;

public class RecentSearchesFragment extends Fragment {
    private static final String RECENT_SEARCHES_PREFS = "RecentSearches";
    private static final String RECENT_SEARCHES_KEY = "queries";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_recent_searches, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        NativeAdHelper.loadNativeAd(requireContext(), view);

        LinearLayout recentSearchesList = view.findViewById(R.id.recentSearchesList);
        TextView emptyRecentSearchesText = view.findViewById(R.id.emptyRecentSearchesText);
        List<String> recentSearches = getRecentSearches();

        if (recentSearches.isEmpty()) {
            emptyRecentSearchesText.setVisibility(View.VISIBLE);
            recentSearchesList.setVisibility(View.GONE);
            return;
        }

        emptyRecentSearchesText.setVisibility(View.GONE);
        recentSearchesList.setVisibility(View.VISIBLE);

        for (String search : recentSearches) {
            TextView searchView = createRecentSearchView(search);
            searchView.setOnClickListener(itemView -> {
                Bundle args = new Bundle();
                args.putString("initialQuery", search);
                Navigation.findNavController(view).navigate(R.id.search_trash_fragment, args);
            });
            recentSearchesList.addView(searchView);
        }
    }

    private TextView createRecentSearchView(String search) {
        TextView textView = new TextView(requireContext());
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        layoutParams.setMargins(0, 0, 0, dpToPx(10));
        textView.setLayoutParams(layoutParams);
        textView.setBackgroundResource(R.drawable.list_item_background);
        textView.setClickable(true);
        textView.setFocusable(true);
        textView.setPadding(dpToPx(16), dpToPx(14), dpToPx(16), dpToPx(14));
        textView.setText(search);
        textView.setTextColor(getResources().getColor(R.color.text_color));
        textView.setTextSize(17);
        return textView;
    }

    private List<String> getRecentSearches() {
        SharedPreferences preferences = requireContext().getSharedPreferences(RECENT_SEARCHES_PREFS, Context.MODE_PRIVATE);
        String storedSearches = preferences.getString(RECENT_SEARCHES_KEY, "");
        List<String> searches = new ArrayList<>();

        if (storedSearches.isEmpty()) {
            return searches;
        }

        for (String search : storedSearches.split("\\n")) {
            if (!search.trim().isEmpty()) {
                searches.add(search);
            }
        }

        return searches;
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }
}
