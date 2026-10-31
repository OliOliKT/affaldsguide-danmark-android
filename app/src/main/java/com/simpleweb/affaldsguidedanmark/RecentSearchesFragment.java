package com.simpleweb.affaldsguidedanmark;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.*;
import android.widget.*;
import androidx.annotation.*;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.text.Collator;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RecentSearchesFragment extends Fragment {
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());
    private List<RecentSearchHistory.Row> history = new ArrayList<>(), visible = new ArrayList<>();
    private List<String> fractions = new ArrayList<>();
    private View root, controls, loading;
    private EditText input;
    private ImageButton clear;
    private TextView sortButton, filterButton, reset, empty;
    private RecyclerView list;
    private TrashDB db;
    private int sort;
    private String query = "", fraction = "";
    private boolean loaded;
    private final HistoryAdapter adapter = new HistoryAdapter();

    @Override public void onCreate(@Nullable Bundle state) {
        super.onCreate(state);
        if (state != null) {
            query = state.getString("query", "");
            fraction = state.getString("fraction", "");
            sort = state.getInt("sort", 0);
        }
    }

    @Override public void onSaveInstanceState(@NonNull Bundle state) {
        super.onSaveInstanceState(state);
        state.putString("query", query);
        state.putString("fraction", fraction);
        state.putInt("sort", sort);
    }

    @Override public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup parent, @Nullable Bundle state) {
        return inflater.inflate(R.layout.fragment_recent_searches, parent, false);
    }

    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle state) {
        super.onViewCreated(view, state);
        root = view; loaded = false;
        visible = new ArrayList<>();
        controls = view.findViewById(R.id.recentSearchControls);
        loading = view.findViewById(R.id.recentSearchLoading);
        empty = view.findViewById(R.id.emptyRecentSearchesText);
        input = view.findViewById(R.id.recentSearchInput);
        clear = view.findViewById(R.id.recentSearchClear);
        sortButton = view.findViewById(R.id.recentSearchSort);
        filterButton = view.findViewById(R.id.recentSearchFilter);
        reset = view.findViewById(R.id.recentSearchReset);
        list = view.findViewById(R.id.recentSearchesList);
        list.setLayoutManager(new LinearLayoutManager(requireContext()));
        list.setAdapter(adapter);
        input.setHint(t("search")); input.setText(query);
        clear.setContentDescription(t("clearSearch"));
        reset.setText(t("reset"));
        input.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) { query = s.toString(); update(); }
            public void afterTextChanged(Editable value) {}
        });
        clear.setOnClickListener(v -> input.setText(""));
        reset.setOnClickListener(v -> { sort = 0; fraction = ""; input.setText(""); update(); });
        sortButton.setOnClickListener(v -> showSort());
        filterButton.setOnClickListener(v -> showFilter());

        String stored = requireContext().getSharedPreferences("RecentSearches", Context.MODE_PRIVATE).getString("queries", "");
        String municipality = SavedMunicipalityManager.getSavedMunicipalityName(requireContext());
        boolean translated = LanguageManager.usesNonDanishContent(requireContext());
        android.content.res.Resources resources = getResources();
        worker.execute(() -> {
            TrashDB database = new TrashDB(resources);
            List<RecentSearchHistory.Row> rows = new ArrayList<>();
            Set<String> seen = new HashSet<>();
            for (String saved : stored.split("\n")) {
                String name = database.getCurrentProductNameForRecentSearch(saved.trim(), translated);
                if (name == null) continue;
                TrashDB.TrashItem item = TrashDB.resolveItem(database.trashItems, name, translated);
                if (item == null || !seen.add(item.product)) continue;
                MunicipalitySortingRuleDB.Rule local = database.getLocalRule(item.product, false, municipality);
                Map<String,String> sorting = local == null ? item.sorting : local.sorting;
                Set<String> categories = new LinkedHashSet<>();
                if (sorting != null) for (String key : sorting.keySet()) categories.add(database.toDanishSortingKey(key));
                rows.add(new RecentSearchHistory.Row(item.product, name, item.getSearchNames(translated), categories));
            }
            main.post(() -> {
                if (!isAdded() || root != view) return;
                db = database; history = rows; loaded = true;
                Set<String> all = new HashSet<>();
                for (RecentSearchHistory.Row row : history) all.addAll(row.fractions);
                fractions = new ArrayList<>(all);
                Collator collator = Collator.getInstance(Locale.getDefault());
                fractions.sort((a,b) -> collator.compare(category(a), category(b)));
                if (!fraction.isEmpty() && !all.contains(fraction)) fraction = "";
                loading.setVisibility(View.GONE);
                update();
            });
        });
    }

    private String t(String suffix) { return InfoPageText.get(requireContext(), "history." + suffix); }
    private String category(String value) { return LanguageManager.usesNonDanishContent(requireContext()) ? db.translateSortingKey(value) : value; }
    private int dp(int value) { return MunicipalityScreenViews.dp(requireContext(), value); }

    private void update() {
        if (!loaded || root == null) return;
        controls.setVisibility(history.isEmpty() ? View.GONE : View.VISIBLE);
        clear.setVisibility(query.isEmpty() ? View.GONE : View.VISIBLE);
        sortButton.setText("↕  " + t(sort == 1 ? "oldest" : sort == 2 ? "name" : "newest") + "  ▾");
        filterButton.setText((fraction.isEmpty() ? t("allFractions") : category(fraction)) + "  ▾");
        sortButton.setContentDescription(t("sort") + ": " + sortButton.getText());
        filterButton.setContentDescription(t("fraction") + ": " + filterButton.getText());
        reset.setVisibility(!query.isEmpty() || !fraction.isEmpty() || sort != 0 ? View.VISIBLE : View.GONE);
        visible = RecentSearchHistory.visible(history, query, fraction, sort, Locale.getDefault());
        adapter.notifyDataSetChanged();
        list.setVisibility(visible.isEmpty() ? View.GONE : View.VISIBLE);
        empty.setText(history.isEmpty() ? getString(R.string.seneste_soegninger_tom) : t("noMatches"));
        empty.setVisibility(visible.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void showSort() {
        PopupMenu menu = new PopupMenu(requireContext(), sortButton);
        String[] keys = {"newest", "oldest", "name"};
        for (int i = 0; i < keys.length; i++) {
            final int selection = i;
            menu.getMenu().add(0, i, i, t(keys[i])).setCheckable(true).setChecked(sort == i)
                    .setOnMenuItemClickListener(item -> { sort = selection; update(); return true; });
        }
        menu.getMenu().setGroupCheckable(0, true, true);
        menu.show();
    }

    private void showFilter() {
        PopupMenu menu = new PopupMenu(requireContext(), filterButton);
        menu.getMenu().add(0, 0, 0, t("allFractions")).setCheckable(true).setChecked(fraction.isEmpty())
                .setOnMenuItemClickListener(item -> { fraction = ""; update(); return true; });
        for (int i = 0; i < fractions.size(); i++) {
            String selection = fractions.get(i);
            menu.getMenu().add(0, i + 1, i + 1, category(selection)).setCheckable(true).setChecked(fraction.equals(selection))
                    .setOnMenuItemClickListener(item -> { fraction = selection; update(); return true; });
        }
        menu.getMenu().setGroupCheckable(0, true, true);
        menu.show();
    }

    @Override public void onDestroyView() {
        list.setAdapter(null);
        root = null;
        super.onDestroyView();
    }
    @Override public void onDestroy() { worker.shutdownNow(); main.removeCallbacksAndMessages(null); super.onDestroy(); }

    private final class HistoryAdapter extends RecyclerView.Adapter<HistoryHolder> {
        @NonNull public HistoryHolder onCreateViewHolder(@NonNull ViewGroup parent, int type) {
            LinearLayout row = new LinearLayout(parent.getContext());
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setBackgroundResource(R.drawable.list_item_background);
            row.setPadding(dp(14), dp(14), dp(14), dp(14));
            row.setMinimumHeight(dp(52)); row.setFocusable(true);
            android.util.TypedValue ripple = new android.util.TypedValue();
            requireContext().getTheme().resolveAttribute(android.R.attr.selectableItemBackground, ripple, true);
            row.setForeground(ContextCompat.getDrawable(requireContext(), ripple.resourceId));
            ImageView clock = new ImageView(requireContext());
            clock.setImageResource(android.R.drawable.ic_menu_recent_history);
            clock.setColorFilter(ContextCompat.getColor(requireContext(), R.color.green_light));
            clock.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
            row.addView(clock, new LinearLayout.LayoutParams(dp(22), dp(22)));
            TextView title = MunicipalityScreenViews.label(requireContext(), "", 17, false, false);
            LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(0, -2, 1);
            titleParams.setMarginStart(dp(12)); row.addView(title, titleParams);
            ImageView arrow = new ImageView(requireContext()); arrow.setImageResource(R.drawable.arrow_right);
            arrow.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
            row.addView(arrow, new LinearLayout.LayoutParams(dp(16), dp(16)));
            RecyclerView.LayoutParams params = new RecyclerView.LayoutParams(-1, -2); params.bottomMargin = dp(10); row.setLayoutParams(params);
            return new HistoryHolder(row, title);
        }
        public void onBindViewHolder(@NonNull HistoryHolder holder, int position) {
            RecentSearchHistory.Row row = visible.get(position); holder.title.setText(row.name);
            holder.itemView.setOnClickListener(view -> {
                Bundle args = new Bundle(); args.putString("initialQuery", row.name);
                Navigation.findNavController(view).navigate(R.id.action_to_searchTrash, args);
            });
        }
        public int getItemCount() { return visible.size(); }
    }
    private static final class HistoryHolder extends RecyclerView.ViewHolder {
        final TextView title;
        HistoryHolder(View view, TextView title) { super(view); this.title = title; }
    }
}
