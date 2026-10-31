package com.simpleweb.affaldsguidedanmark;

import android.content.*;
import android.net.Uri;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import android.graphics.Color;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.*;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.*;
import java.util.*;

public final class SortingImpactFragment extends Fragment {
    private LinearLayout content;private ScrollView scroll;private TrashDB db;private SortingLogStore store;
    private boolean weekly,showAll;private int sort;private String fraction="";private YearMonth month=YearMonth.now();
    private LocalDate selectedRhythmDate;
    private String exportText;
    private final ActivityResultLauncher<String> exporter=registerForActivityResult(new ActivityResultContracts.CreateDocument("text/csv"),uri->{
        if(uri==null||!isAdded())return;
        try(java.io.OutputStream out=requireContext().getContentResolver().openOutputStream(uri)){if(out==null)throw new java.io.IOException();out.write(exportText.getBytes(StandardCharsets.UTF_8));}
        catch(Exception error){Toast.makeText(requireContext(),t("exportError"),Toast.LENGTH_LONG).show();}
    });
    @Override public void onCreate(@Nullable Bundle state) {
        super.onCreate(state);
        if (state != null) {
            exportText = state.getString("exportText");
            weekly = state.getBoolean("weekly");
            showAll = state.getBoolean("showAll");
            sort = state.getInt("sort");
            fraction = state.getString("fraction", "");
            month = YearMonth.parse(state.getString("month", YearMonth.now().toString()));
            String selectedDay = state.getString("selectedRhythmDate");
            if (selectedDay != null) selectedRhythmDate = LocalDate.parse(selectedDay);
        }
    }
    @Override public void onSaveInstanceState(@NonNull Bundle state) {
        super.onSaveInstanceState(state);
        state.putString("exportText", exportText);
        state.putBoolean("weekly", weekly);
        state.putBoolean("showAll", showAll);
        state.putInt("sort", sort);
        state.putString("fraction", fraction);
        state.putString("month", month.toString());
        if (selectedRhythmDate != null) state.putString("selectedRhythmDate", selectedRhythmDate.toString());
    }
    @Nullable public View onCreateView(@NonNull LayoutInflater inflater,@Nullable ViewGroup parent,@Nullable Bundle state){
        LinearLayout page=MunicipalityScreenViews.page(this,t("page"));scroll=new ScrollView(requireContext());content=new LinearLayout(requireContext());content.setOrientation(LinearLayout.VERTICAL);content.setPadding(0,0,0,dp(28));scroll.addView(content);page.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        store=new SortingLogStore(requireContext());SortingImpactViews.comparison(requireContext(),store.total(),true);
        content.addView(new ProgressBar(requireContext()));
        android.content.res.Resources resources=getResources();
        java.util.concurrent.ExecutorService loader=java.util.concurrent.Executors.newSingleThreadExecutor();
        loader.execute(()->{TrashDB loaded=new TrashDB(resources);new android.os.Handler(android.os.Looper.getMainLooper()).post(()->{if(isAdded()&&content!=null){db=loaded;render();}});});loader.shutdown();
        return page;
    }
    @Override public void onDestroyView(){content=null;super.onDestroyView();}
    private String t(String key){return InfoPageText.get(requireContext(),"impact."+key);}
    private String history(String key){return InfoPageText.get(requireContext(),"history."+key);}
    private int dp(int n){return MunicipalityScreenViews.dp(requireContext(),n);}
    private String category(String f){return LanguageManager.usesNonDanishContent(requireContext())?db.translateSortingKey(f):f;}
    private String name(SortingLogStore.Entry e){return db.getCurrentProductNameForRecentSearch(e.product,LanguageManager.usesNonDanishContent(requireContext()));}
    private TextView text(String value,int size,boolean bold){return MunicipalityScreenViews.label(requireContext(),value,size,bold,!bold);}
    private void render(){
        if(content==null||db==null)return;int y=scroll.getScrollY();store=new SortingLogStore(requireContext());content.removeAllViews();
        TextView intro=text(t("dashboardIntro"),14,false);intro.setPadding(dp(16),0,dp(16),dp(14));content.addView(intro);
        content.addView(SortingImpactViews.summary(requireContext(),store,false));
        List<SortingLogStore.Entry> entries=store.entries();
        if(entries.isEmpty()){LinearLayout empty=section(t("empty"));empty.addView(text(t("emptyHelp"),15,false));}
        else {rhythm(entries);fractionChart(entries,false);timeChart(entries);fractionChart(entries,true);recent(new ArrayList<>(entries));}
        LinearLayout method=section(t("methodTitle"));method.addView(text(t("method"),14,false));
        String[] keys={"source","materialSource","electronicsSource","foodSource","cartonSource","textileSource","batterySource"};
        String[] urls={"https://www.epa.gov/system/files/documents/2024-01/warm_management_practices_v16_dec.pdf","https://www.epa.gov/system/files/documents/2023-12/warm_containers_packaging_and_non-durable_goods_materials_v16_dec.pdf","https://www.epa.gov/system/files/documents/2023-12/warm_electronics_v16_dec.pdf","https://edit.mst.dk/media/s0rpgnej/handlingsplan-for-cirkulaer-oekonomi.pdf","https://www2.mst.dk/Udgiv/publikationer/2022/01/978-87-7038-377-6.pdf","https://www2.mst.dk/Udgiv/publikationer/2021/05/978-87-7038-306-6.pdf","https://www2.mst.dk/udgiv/publikationer/2013/04/978-87-93026-09-4.pdf"};
        for(int i=0;i<keys.length;i++){String url=urls[i];TextView link=text(t(keys[i])+" ↗",14,false);link.setTextColor(ContextCompat.getColor(requireContext(),R.color.green_light));link.setPadding(0,dp(10),0,dp(10));link.setOnClickListener(v->startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(url))));method.addView(link);}
        Button export=new Button(requireContext());export.setText(t("export"));export.setAllCaps(false);export.setEnabled(!entries.isEmpty());export.setOnClickListener(v->{exportText=csv(entries);exporter.launch("affaldsoverblik-"+LocalDate.now()+".csv");});method.addView(export);
        scroll.post(()->scroll.scrollTo(0,y));
    }
    private LinearLayout section(String title){
        LinearLayout box=MunicipalityScreenViews.card(requireContext());box.setClickable(false);box.setFocusable(false);box.setForeground(null);
        LinearLayout.LayoutParams params=(LinearLayout.LayoutParams)box.getLayoutParams();params.topMargin=dp(22);content.addView(box);
        TextView heading=text(title,20,true);heading.setPadding(0,0,0,dp(12));box.addView(heading);return box;
    }
    private void fractionChart(List<SortingLogStore.Entry> entries,boolean climate){
        LinearLayout box=section(t(climate?"climateByFraction":"byFraction"));Map<String,Double> totals=new LinkedHashMap<>();
        for(SortingLogStore.Entry e:entries)for(Map.Entry<String,Double> factor:e.factors.entrySet())totals.merge(factor.getKey(),(climate?e.weightGrams/1000.0*factor.getValue():e.weightGrams)/(double)e.factors.size(),Double::sum);
        List<String> keys=new ArrayList<>(totals.keySet());keys.removeIf(k->totals.get(k)<=0);keys.sort((a,b)->Double.compare(totals.get(b),totals.get(a)));
        if(keys.isEmpty()){box.addView(text(t("climateByFractionEmpty"),14,false));return;}
        double max=totals.get(keys.get(0));
        for(String key:keys){LinearLayout row=new LinearLayout(requireContext());row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(0,dp(6),0,dp(6));
            com.google.android.material.imageview.ShapeableImageView icon=new com.google.android.material.imageview.ShapeableImageView(requireContext());icon.setImageResource(TrashDB.getImageResourceForKey(key,LanguageManager.usesNonDanishContent(requireContext())));icon.setShapeAppearanceModel(icon.getShapeAppearanceModel().toBuilder().setAllCornerSizes(dp(5)).build());row.addView(icon,new LinearLayout.LayoutParams(dp(28),dp(28)));
            LinearLayout graph=new LinearLayout(requireContext());graph.setOrientation(LinearLayout.VERTICAL);graph.setPadding(dp(9),0,0,0);row.addView(graph,new LinearLayout.LayoutParams(0,-2,1));
            String amount=climate?SortingImpactViews.amount(totals.get(key))+" CO₂e":SortingImpactViews.number(totals.get(key))+" g";
            graph.addView(text(category(key)+" • "+amount,14,true));
            ProgressBar bar=new ProgressBar(requireContext(),null,android.R.attr.progressBarStyleHorizontal);bar.setMax(1000);bar.setProgress((int)Math.round(totals.get(key)/max*1000));bar.setProgressTintList(android.content.res.ColorStateList.valueOf(SortingImpactViews.color(requireContext(),key)));graph.addView(bar,new LinearLayout.LayoutParams(-1,dp(12)));box.addView(row);
        }
        box.addView(text(t(climate?"climateMixedUnits":"weightAxis"),12,false));
    }
    private void timeChart(List<SortingLogStore.Entry> entries){
        LinearLayout box=section(t("monthly"));RadioGroup toggle=new RadioGroup(requireContext());toggle.setOrientation(LinearLayout.HORIZONTAL);
        RadioButton months=new RadioButton(requireContext()),weeks=new RadioButton(requireContext());months.setId(View.generateViewId());weeks.setId(View.generateViewId());months.setText(t("months"));weeks.setText(t("weeks"));months.setTextSize(13);weeks.setTextSize(13);toggle.addView(months);toggle.addView(weeks);toggle.check(weekly?weeks.getId():months.getId());toggle.setOnCheckedChangeListener((g,id)->{weekly=id==weeks.getId();render();});box.addView(toggle);
        LocalDate now=LocalDate.now(),current=weekly?now.with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY)):now.withDayOfMonth(1);
        List<String> labels=new ArrayList<>();List<Double> values=new ArrayList<>();
        for(int offset=5;offset>=0;offset--){LocalDate start=weekly?current.minusWeeks(offset):current.minusMonths(offset),end=weekly?start.plusWeeks(1):start.plusMonths(1);double sum=0;
            for(SortingLogStore.Entry entry:entries){LocalDate d=Instant.ofEpochMilli(entry.date).atZone(ZoneId.systemDefault()).toLocalDate();if(!d.isBefore(start)&&d.isBefore(end))sum+=entry.estimate();}
            labels.add(weekly?InfoPageText.get(requireContext(),"impact.weekNumber").replace("{0}",String.valueOf(start.get(WeekFields.ISO.weekOfWeekBasedYear()))):start.format(DateTimeFormatter.ofPattern("MMM",Locale.getDefault())));values.add(sum);
        }
        box.addView(new SortingImpactViews.TimeChart(requireContext(),labels,values),new LinearLayout.LayoutParams(-1,dp(200)));
    }
    private void rhythm(List<SortingLogStore.Entry> entries) {
        LinearLayout box = section(t("rhythmTitle"));
        box.addView(text(t("rhythmIntro"), 14, false));
        LinearLayout heading = new LinearLayout(requireContext());
        heading.setGravity(Gravity.CENTER_VERTICAL);
        Button previous = new Button(requireContext());
        previous.setText("‹");
        previous.setContentDescription(t("previousMonth"));
        previous.setOnClickListener(v -> { month = month.minusMonths(1); render(); });
        heading.addView(previous, new LinearLayout.LayoutParams(dp(48), dp(48)));
        TextView title = text(month.format(DateTimeFormatter.ofPattern("LLLL yyyy", Locale.getDefault())), 16, true);
        title.setGravity(Gravity.CENTER);
        heading.addView(title, new LinearLayout.LayoutParams(0, -2, 1));
        Button next = new Button(requireContext());
        next.setText("›");
        next.setContentDescription(t("nextMonth"));
        next.setEnabled(month.isBefore(YearMonth.now()));
        next.setOnClickListener(v -> { month = month.plusMonths(1); render(); });
        heading.addView(next, new LinearLayout.LayoutParams(dp(48), dp(48)));
        box.addView(heading);

        Map<LocalDate, Integer> counts = new HashMap<>();
        for (SortingLogStore.Entry entry : entries) {
            LocalDate date = Instant.ofEpochMilli(entry.date).atZone(ZoneId.systemDefault()).toLocalDate();
            counts.merge(date, 1, Integer::sum);
        }
        LinearLayout weekdays = new LinearLayout(requireContext());
        for (java.time.DayOfWeek day : java.time.DayOfWeek.values()) {
            TextView label = text(day.getDisplayName(java.time.format.TextStyle.SHORT, Locale.getDefault()), 11, false);
            label.setGravity(Gravity.CENTER);
            weekdays.addView(label, new LinearLayout.LayoutParams(0, dp(28), 1));
        }
        box.addView(weekdays);

        int blank = month.atDay(1).getDayOfWeek().getValue() - 1;
        TextView detail = text("", 15, true);
        Map<LocalDate, TextView> dayCells = new HashMap<>();
        for (int row = 0; row < (blank + month.lengthOfMonth() + 6) / 7; row++) {
            LinearLayout line = new LinearLayout(requireContext());
            for (int col = 0; col < 7; col++) {
                int day = row * 7 + col - blank + 1;
                TextView cell = new SortingImpactViews.DayCell(requireContext());
                cell.setTextSize(16);
                cell.setTypeface(null, android.graphics.Typeface.BOLD);
                cell.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_color));
                cell.setGravity(Gravity.CENTER);
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(44), 1);
                params.setMargins(dp(2), dp(2), dp(2), dp(2));
                line.addView(cell, params);
                if (day < 1 || day > month.lengthOfMonth()) {
                    cell.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
                    continue;
                }
                LocalDate date = month.atDay(day);
                int count = counts.getOrDefault(date, 0);
                cell.setText(String.valueOf(day));
                cell.setFocusable(true);
                cell.setContentDescription(date.format(DateTimeFormatter.ofLocalizedDate(java.time.format.FormatStyle.LONG))
                        + ": " + count + " " + t("rhythmCount"));
                styleRhythmDay(cell, count, date.equals(selectedRhythmDate));
                dayCells.put(date, cell);
                cell.setOnClickListener(v -> {
                    TextView previousDay = dayCells.get(selectedRhythmDate);
                    if (previousDay != null) styleRhythmDay(previousDay, counts.getOrDefault(selectedRhythmDate, 0), false);
                    selectedRhythmDate = date;
                    styleRhythmDay(cell, count, true);
                    detail.setText(rhythmDayDetail(date, count));
                    detail.announceForAccessibility(detail.getText());
                });
            }
            box.addView(line);
        }
        if (selectedRhythmDate != null && YearMonth.from(selectedRhythmDate).equals(month)) {
            detail.setText(rhythmDayDetail(selectedRhythmDate, counts.getOrDefault(selectedRhythmDate, 0)));
        }
        box.addView(detail);
    }

    private void styleRhythmDay(TextView cell, int count, boolean selected) {
        android.graphics.drawable.GradientDrawable background = new android.graphics.drawable.GradientDrawable();
        background.setCornerRadius(dp(6));
        background.setColor(selected ? 0xffe1efd9 : count > 0 ? 0xffd6e8cf : 0xffeeeeee);
        if (selected) background.setStroke(dp(2), ContextCompat.getColor(requireContext(), R.color.green_light));
        cell.setBackground(background);
        cell.setSelected(selected);
    }

    private String rhythmDayDetail(LocalDate date, int count) {
        return date.format(DateTimeFormatter.ofLocalizedDate(java.time.format.FormatStyle.MEDIUM))
                + " • " + t("rhythmCount") + ": " + count;
    }

    private void recent(List<SortingLogStore.Entry> entries){
        LinearLayout box=section(t("recent"));LinearLayout controls=new LinearLayout(requireContext());
        Spinner sorting=new Spinner(requireContext());String[] sortKeys={"newest","oldest","name","weight","climate"};List<String> options=new ArrayList<>();for(String key:sortKeys)options.add(history(key));sorting.setAdapter(new ArrayAdapter<>(requireContext(),android.R.layout.simple_spinner_dropdown_item,options));sorting.setSelection(sort);sorting.setContentDescription(history("sort"));controls.addView(sorting,new LinearLayout.LayoutParams(0,dp(48),1));
        Set<String> all=new TreeSet<>();for(SortingLogStore.Entry e:entries)all.addAll(e.factors.keySet());List<String> fractions=new ArrayList<>();fractions.add("");fractions.addAll(all);List<String> names=new ArrayList<>();for(String f:fractions)names.add(f.isEmpty()?history("allFractions"):category(f));Spinner filter=new Spinner(requireContext());filter.setAdapter(new ArrayAdapter<>(requireContext(),android.R.layout.simple_spinner_dropdown_item,names));filter.setSelection(Math.max(0,fractions.indexOf(fraction)));filter.setContentDescription(history("fraction"));controls.addView(filter,new LinearLayout.LayoutParams(0,dp(48),1));box.addView(controls);
        sorting.setOnItemSelectedListener(listener(index->{if(index!=sort){sort=index;render();}}));filter.setOnItemSelectedListener(listener(index->{if(!fraction.equals(fractions.get(index))){fraction=fractions.get(index);render();}}));
        entries.removeIf(e->!fraction.isEmpty()&&!e.factors.containsKey(fraction));entries.sort((a,b)->{switch(sort){case 1:return Long.compare(a.date,b.date);case 2:return java.text.Collator.getInstance().compare(name(a),name(b));case 3:return Integer.compare(b.weightGrams,a.weightGrams);case 4:return Double.compare(b.estimate(),a.estimate());default:return Long.compare(b.date,a.date);}});
        if(entries.isEmpty())box.addView(text(history("noMatches"),14,false));
        for(int i=0;i<(showAll?entries.size():Math.min(6,entries.size()));i++){SortingLogStore.Entry entry=entries.get(i);LinearLayout row=new LinearLayout(requireContext());row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(0,dp(8),0,dp(8));ImageView icon=new ImageView(requireContext());icon.setImageResource(TrashDB.getImageResourceForKey(entry.factors.keySet().iterator().next(),LanguageManager.usesNonDanishContent(requireContext())));row.addView(icon,new LinearLayout.LayoutParams(dp(40),dp(40)));
            LinearLayout details=new LinearLayout(requireContext());details.setOrientation(LinearLayout.VERTICAL);details.setPadding(dp(10),0,0,0);details.addView(text(name(entry),15,true));List<String> fs=new ArrayList<>();for(String f:entry.factors.keySet())fs.add(category(f));details.addView(text(String.join(" + ",fs)+" • "+entry.weightGrams+" g",12,false));details.addView(text(Instant.ofEpochMilli(entry.date).atZone(ZoneId.systemDefault()).toLocalDate().format(DateTimeFormatter.ofLocalizedDate(java.time.format.FormatStyle.MEDIUM))+(entry.estimate()>0?" • ≈ "+SortingImpactViews.amount(entry.estimate())+" CO₂e":""),12,false));row.addView(details,new LinearLayout.LayoutParams(0,-2,1));
            TextView menu=text("⋯",28,true);menu.setGravity(Gravity.CENTER);menu.setContentDescription(t("edit")+" / "+t("delete"));row.addView(menu,new LinearLayout.LayoutParams(dp(48),dp(48)));menu.setOnClickListener(v->{PopupMenu popup=new PopupMenu(requireContext(),menu);popup.getMenu().add(t("edit")).setOnMenuItemClickListener(m->{SortingLogDialog.edit(requireContext(),db,entry,this::render);return true;});popup.getMenu().add(t("delete")).setOnMenuItemClickListener(m->{new androidx.appcompat.app.AlertDialog.Builder(requireContext()).setTitle(t("delete")+"?").setMessage(name(entry)).setNegativeButton(t("cancel"),null).setPositiveButton(t("delete"),(d,w)->{try{store.delete(entry.id);render();}catch(Exception ex){Toast.makeText(requireContext(),t("saveError"),Toast.LENGTH_LONG).show();}}).show();return true;});popup.show();});box.addView(row);
        }
        if(entries.size()>6){Button more=new Button(requireContext());more.setText(t(showAll?"showFewerEntries":"showAllEntries"));more.setAllCaps(false);more.setOnClickListener(v->{showAll=!showAll;render();});box.addView(more);}
    }
    private AdapterView.OnItemSelectedListener listener(java.util.function.IntConsumer changed){return new AdapterView.OnItemSelectedListener(){public void onItemSelected(AdapterView<?> p,View v,int pos,long id){changed.accept(pos);}public void onNothingSelected(AdapterView<?> p){}};}
    private String csv(List<SortingLogStore.Entry> entries){List<String> rows=new ArrayList<>();List<String> headers=new ArrayList<>();for(String key:new String[]{"exportDate","exportItem","exportFractions","exportMunicipality","exportWeight","exportImpact","exportVersion"})headers.add(t(key));rows.add(csvRow(headers));
        for(SortingLogStore.Entry e:entries){List<String> fractions=new ArrayList<>();for(String f:e.factors.keySet())fractions.add(category(f));rows.add(csvRow(Arrays.asList(Instant.ofEpochMilli(e.date).toString(),name(e),String.join(" + ",fractions),e.municipality,String.valueOf(e.weightGrams),String.valueOf(e.estimate()),e.factorVersion)));}return "\uFEFF"+String.join("\r\n",rows)+"\r\n";}
    private String csvRow(List<String> cells){List<String> escaped=new ArrayList<>();for(String cell:cells)escaped.add(SortingImpact.csvCell(cell));return String.join(",",escaped);}
}
