package com.simpleweb.affaldsguidedanmark;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;

public class AchievementsFragment extends Fragment {
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup parent,
                             @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_achievements, parent, false);
        TextView pageTitle = root.findViewById(R.id.achievementsTitle);
        pageTitle.setText(InfoPageText.get(requireContext(), "achievements.page"));
        LinearLayout container = root.findViewById(R.id.achievementsContainer);
        AchievementStore store = AchievementStore.get(requireContext());
        String highlighted = getArguments() == null ? null : getArguments().getString("highlightAchievement");
        View highlightedCard = null;
        for (AchievementStore.Achievement achievement : AchievementStore.visibleAchievements()) {
            View card = createCard(store, achievement, achievement.id.equals(highlighted));
            container.addView(card);
            if (achievement.id.equals(highlighted)) highlightedCard = card;
        }
        if (highlightedCard != null) {
            View destination = highlightedCard;
            NestedScrollView scroll = root.findViewById(R.id.achievementsScroll);
            scroll.post(() -> scroll.smoothScrollTo(0, destination.getTop()));
        }
        return root;
    }

    private View createCard(AchievementStore store, AchievementStore.Achievement achievement,
                            boolean highlighted) {
        LinearLayout card = new LinearLayout(requireContext());
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.TOP);
        card.setPadding(dp(14), dp(14), dp(14), dp(14));
        card.setBackgroundResource(R.drawable.result_description_background);
        if (highlighted) card.setBackgroundTintList(ColorStateList.valueOf(0xffe7f4e4));
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        cardParams.setMargins(0, 0, 0, dp(10));
        card.setLayoutParams(cardParams);

        ImageView icon = new ImageView(requireContext());
        icon.setImageResource(iconFor(achievement.id));
        icon.setColorFilter(ContextCompat.getColor(requireContext(),
                store.isUnlocked(achievement) ? R.color.green_light : R.color.grey_black));
        icon.setContentDescription(null);
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(dp(32), dp(32));
        iconParams.setMargins(0, dp(3), dp(12), 0);
        card.addView(icon, iconParams);

        LinearLayout content = new LinearLayout(requireContext());
        content.setOrientation(LinearLayout.VERTICAL);
        card.addView(content, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        TextView title = new TextView(requireContext());
        title.setText(InfoPageText.get(requireContext(), "achievements." + achievement.id + ".title"));
        title.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_color));
        title.setTextSize(17);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        content.addView(title);

        TextView detail = new TextView(requireContext());
        detail.setText(InfoPageText.get(requireContext(), "achievements." + achievement.id + ".detail"));
        detail.setTextColor(ContextCompat.getColor(requireContext(), R.color.grey_black));
        detail.setTextSize(14);
        detail.setPadding(0, dp(3), 0, dp(8));
        content.addView(detail);

        ProgressBar progress = new ProgressBar(requireContext(), null, android.R.attr.progressBarStyleHorizontal);
        progress.setMax(achievement.target);
        progress.setProgress(store.count(achievement));
        progress.setProgressTintList(ColorStateList.valueOf(
                ContextCompat.getColor(requireContext(), R.color.green_light)));
        content.addView(progress, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(7)));

        TextView count = new TextView(requireContext());
        count.setText(store.count(achievement) + "/" + achievement.target);
        count.setTextColor(ContextCompat.getColor(requireContext(), R.color.grey_black));
        count.setTextSize(12);
        count.setPadding(0, dp(5), 0, 0);
        content.addView(count);

        if (store.isUnlocked(achievement)) {
            TextView check = new TextView(requireContext());
            check.setText("✓");
            check.setTextSize(22);
            check.setTextColor(ContextCompat.getColor(requireContext(), R.color.green_light));
            check.setContentDescription(InfoPageText.get(requireContext(), "achievements.unlocked"));
            LinearLayout.LayoutParams checkParams = new LinearLayout.LayoutParams(dp(30), dp(32));
            checkParams.setMargins(dp(10), 0, 0, 0);
            card.addView(check, checkParams);
        }
        return card;
    }

    private int iconFor(String id) {
        if (id.equals("firstAnswer")) return android.R.drawable.checkbox_on_background;
        if (id.equals("fiveCategories") || id.equals("allFractions")) return android.R.drawable.ic_menu_sort_by_size;
        if (id.equals("localExpert") || id.equals("localRulesExpert")) return android.R.drawable.ic_menu_myplaces;
        if (id.equals("recyclingCenter") || id.equals("neighborhoodRegular")) return android.R.drawable.ic_menu_mapmode;
        if (id.equals("helpfulEye") || id.equals("trueWasteExpert")) return android.R.drawable.ic_menu_edit;
        if (id.equals("tenDayStreak") || id.equals("thirtyDayStreak")) return android.R.drawable.ic_menu_today;
        return android.R.drawable.ic_menu_search;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
