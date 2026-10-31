package com.simpleweb.affaldsguidedanmark;

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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LocalSortingRulesFragment extends Fragment {
    private String query = "";
    private String municipality = "";
    private List<RuleRow> rules = new ArrayList<>();
    private final List<RuleRow> visible = new ArrayList<>();
    private boolean loaded;
    private static final class RuleRow {
        String name, categories, searchable;
        int image;
    }
    private RecyclerView list;
    private TextView status;
    private ExecutorService worker;
    private boolean nonDanish;

    @Nullable @Override public View onCreateView(@NonNull LayoutInflater inflater,@Nullable ViewGroup container,@Nullable Bundle state) {
        municipality = getArguments() == null ? SavedMunicipalityManager.getSavedMunicipalityName(requireContext()) : getArguments().getString("municipalityName", "");
        if (municipality == null) municipality = "";
        nonDanish = LanguageManager.usesNonDanishContent(requireContext());
        loaded = false;
        LinearLayout page = MunicipalityScreenViews.page(this, MunicipalityScreenViews.text(requireContext(),"municipalityBox.rulesTitle",municipality));
        TextView intro = MunicipalityScreenViews.label(requireContext(),MunicipalityScreenViews.text(requireContext(),"municipalityBox.localRulesExplanation"),14,false,true);
        intro.setPadding(MunicipalityScreenViews.dp(requireContext(),16),MunicipalityScreenViews.dp(requireContext(),4),MunicipalityScreenViews.dp(requireContext(),16),MunicipalityScreenViews.dp(requireContext(),12)); page.addView(intro);
        android.widget.EditText search = MunicipalityScreenViews.search(requireContext(),MunicipalityScreenViews.text(requireContext(),"municipalityBox.searchRules"),value->{query=value;filter();});
        search.setText(query); page.addView(search);
        status = MunicipalityScreenViews.label(requireContext(),MunicipalityScreenViews.text(requireContext(),"searchview.005"),14,false,true);
        status.setPadding(MunicipalityScreenViews.dp(requireContext(),16),MunicipalityScreenViews.dp(requireContext(),4),MunicipalityScreenViews.dp(requireContext(),16),MunicipalityScreenViews.dp(requireContext(),12)); page.addView(status);
        list = new RecyclerView(requireContext()); list.setLayoutManager(new LinearLayoutManager(requireContext()));
        list.setAdapter(new RulesAdapter()); page.addView(list,new LinearLayout.LayoutParams(-1,0,1));
        return page;
    }
    @Override public void onViewCreated(@NonNull View view,@Nullable Bundle state) {
        worker=Executors.newSingleThreadExecutor();
        android.content.res.Resources resources=getResources();
        worker.execute(()->{
            TrashDB loaded=new TrashDB(resources);
            java.util.Map<String, TrashDB.TrashItem> items = new java.util.HashMap<>();
            if (loaded.trashItems != null) for (TrashDB.TrashItem item : loaded.trashItems) items.put(item.product, item);
            List<RuleRow> entries = new ArrayList<>();
            for (MunicipalitySortingRuleDB.Rule rule : loaded.localRulesFor(municipality)) {
                RuleRow row = new RuleRow();
                TrashDB.TrashItem item = items.get(rule.product);
                row.name = item == null ? rule.product : item.getDisplayProduct(nonDanish);
                List<String> categories = new ArrayList<>();
                String first = "Genbrugsplads";
                if (rule.sorting != null) for (String category : rule.sorting.keySet()) {
                    categories.add(loaded.translateSortingKey(category));
                    if (first.equals("Genbrugsplads")) first = category;
                }
                row.categories = String.join(" · ", categories);
                row.searchable = (row.name + " " + row.categories + " " + (item == null ? "" : String.join(" ", item.getSearchNames(nonDanish)))).toLowerCase(Locale.ROOT);
                row.image = TrashDB.getImageResourceForKey(first, nonDanish);
                entries.add(row);
            }
            java.text.Collator collator = java.text.Collator.getInstance(Locale.getDefault());
            entries.sort((a,b) -> collator.compare(a.name, b.name));
            view.post(() -> { if (getView() != view) return; rules = entries; this.loaded = true; filter(); });
        });
    }
    private void filter() {
        if (list == null || !loaded) return;
        visible.clear();
        String needle = query.trim().toLowerCase(Locale.ROOT);
        for (RuleRow row : rules) if (row.searchable.contains(needle)) visible.add(row);
        status.setText(MunicipalityScreenViews.text(requireContext(), visible.isEmpty() ? "municipalityBox.noRules" : "municipalityBox.ruleCount", String.valueOf(visible.size())));
        list.getAdapter().notifyDataSetChanged();
    }
    private class RulesAdapter extends RecyclerView.Adapter<RuleHolder>{
        @NonNull public RuleHolder onCreateViewHolder(@NonNull ViewGroup parent,int type){return new RuleHolder(MunicipalityScreenViews.card(parent.getContext()));}
        public int getItemCount(){return visible.size();}
        public void onBindViewHolder(@NonNull RuleHolder holder,int position){
            RuleRow row = visible.get(position);
            LinearLayout card = holder.card;
            card.removeAllViews();
            LinearLayout content = new LinearLayout(card.getContext()); content.setGravity(android.view.Gravity.CENTER_VERTICAL);
            android.widget.ImageView image = new android.widget.ImageView(card.getContext()); image.setImageResource(row.image);
            android.graphics.drawable.GradientDrawable rounded = new android.graphics.drawable.GradientDrawable();
            rounded.setColor(android.graphics.Color.WHITE); rounded.setCornerRadius(MunicipalityScreenViews.dp(card.getContext(), 7));
            image.setBackground(rounded); image.setClipToOutline(true);
            content.addView(image, new LinearLayout.LayoutParams(MunicipalityScreenViews.dp(card.getContext(), 42), MunicipalityScreenViews.dp(card.getContext(), 42)));
            LinearLayout labels = new LinearLayout(card.getContext()); labels.setOrientation(LinearLayout.VERTICAL);
            labels.addView(MunicipalityScreenViews.label(card.getContext(), row.name, 17, true, false));
            labels.addView(MunicipalityScreenViews.label(card.getContext(), row.categories, 14, false, true));
            LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(0, -2, 1); labelParams.setMarginStart(MunicipalityScreenViews.dp(card.getContext(), 12));
            content.addView(labels, labelParams);
            content.addView(MunicipalityScreenViews.label(card.getContext(), "›", 24, false, true));
            card.addView(content);
            card.setOnClickListener(v -> { Bundle args = new Bundle(); args.putString("initialQuery", row.name); Navigation.findNavController(v).navigate(R.id.search_trash_fragment, args); });
        }
    }
    private static class RuleHolder extends RecyclerView.ViewHolder{final LinearLayout card;RuleHolder(LinearLayout view){super(view);card=view;}}
    @Override public void onDestroyView(){if(worker!=null)worker.shutdownNow();list=null;status=null;super.onDestroyView();}
}
