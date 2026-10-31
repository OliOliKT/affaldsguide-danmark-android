package com.simpleweb.affaldsguidedanmark;

import android.content.Context;
import android.graphics.Typeface;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.text.*;
import android.view.*;
import android.widget.*;
import androidx.core.content.ContextCompat;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import java.util.*;

/** Shared weight picker for new registrations and editing existing ones. */
final class SortingLogDialog {
    private final Context context;
    private final TrashDB db;
    private final SortingImpact impact;
    private final SortingLogStore store;
    private final BottomSheetDialog dialog;
    private final LinearLayout body;
    private final TextView estimate;
    private final Button save;
    private final List<String> selected = new ArrayList<>();
    private final List<Button> destinationButtons = new ArrayList<>(), weightButtons = new ArrayList<>();
    private Integer grams;
    private TextView splitNotice;
    private final TrashDB.TrashItem item;
    private final SortingLogStore.Entry editing;
    private final String municipality;
    private final Runnable onSaved;

    static void show(Context context, TrashDB db, TrashDB.TrashItem item, Collection<String> fractions, String municipality, Runnable onSaved) {
        new SortingLogDialog(context, db, item, fractions, municipality, null, onSaved).dialog.show();
    }
    static void edit(Context context, TrashDB db, SortingLogStore.Entry entry, Runnable onSaved) {
        new SortingLogDialog(context, db, null, entry.factors.keySet(), entry.municipality, entry, onSaved).dialog.show();
    }
    private SortingLogDialog(Context context, TrashDB db, TrashDB.TrashItem item, Collection<String> fractions,
                             String municipality, SortingLogStore.Entry editing, Runnable onSaved) {
        this.context=context; this.db=db; this.item=item; this.municipality=municipality; this.editing=editing; this.onSaved=onSaved;
        impact=new SortingImpact(context.getResources()); store=new SortingLogStore(context);
        dialog=new BottomSheetDialog(context); ScrollView scroll=new ScrollView(context); body=new LinearLayout(context);
        body.setOrientation(LinearLayout.VERTICAL); body.setPadding(dp(20),dp(20),dp(20),dp(28)); scroll.addView(body); dialog.setContentView(scroll);
        add(t(editing==null?"logTitle":"edit"),22,true); add(t("saveIntro"),14,false);
        add(db.getCurrentProductNameForRecentSearch(editing==null?item.product:editing.product, LanguageManager.usesNonDanishContent(context)),19,true);
        List<String> choices=impact.choices(fractions);
        if (editing!=null) selected.addAll(editing.factors.keySet());
        else if(choices.size()==1) selected.add(choices.get(0));
        if(editing==null && choices.size()>1) {
            add(t("chooseDestination"),17,true);
            for(String choice:choices) {
                Button option=choiceButton(category(choice)); destinationButtons.add(option); body.addView(option);
                option.setOnClickListener(v->{ selected.clear(); selected.add(choice); selectDestination(option); if (splitNotice != null) splitNotice.setVisibility(View.GONE); update(); });
            }
            if(choices.size()==2||choices.size()==3) {
                Button all=choiceButton(t(choices.size()==2?"bothDestinations":"allThreeDestinations")); destinationButtons.add(all); body.addView(all);
                all.setOnClickListener(v->{selected.clear();selected.addAll(choices);selectDestination(all);splitNotice.setVisibility(View.VISIBLE);update();});
                splitNotice = add(t("weightSplit"),13,false);
                splitNotice.setVisibility(View.GONE);
            }
        }
        add(t("chooseWeight"),17,true);
        int[] weights={10,50,100,250,500,1000};
        for(int row=0;row<2;row++) {
            LinearLayout line=new LinearLayout(context);
            for(int col=0;col<3;col++) {
                int weight=weights[row*3+col]; Button button=choiceButton(weight==1000?"1 kg":weight+" g");weightButtons.add(button);
                LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(0,dp(54),1);params.setMargins(dp(3),dp(3),dp(3),dp(3));line.addView(button,params);
                button.setOnClickListener(v->{ grams=weight; for(Button other:weightButtons) tint(other,other==button); customBox.setVisibility(View.GONE);update(); });
            }
            body.addView(line);
        }
        Button other=choiceButton(t("other"));body.addView(other);
        customBox=new LinearLayout(context);customBox.setVisibility(View.GONE);body.addView(customBox);
        EditText custom=new EditText(context);custom.setHint(t("customWeight"));custom.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL);
        custom.setSingleLine();customBox.addView(custom,new LinearLayout.LayoutParams(0,dp(54),1));
        Spinner unit=new Spinner(context);unit.setAdapter(new ArrayAdapter<>(context,android.R.layout.simple_spinner_dropdown_item,new String[]{"g","kg"}));customBox.addView(unit);
        Runnable customChanged=()->{ if(customBox.getVisibility()==View.VISIBLE){grams=SortingImpact.grams(custom.getText().toString(),unit.getSelectedItemPosition()==1);update();} };
        custom.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int a,int c,int f){} public void onTextChanged(CharSequence s,int a,int b,int c){customChanged.run();}public void afterTextChanged(Editable e){}});
        unit.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){public void onItemSelected(AdapterView<?> p,View v,int pos,long id){customChanged.run();}public void onNothingSelected(AdapterView<?> p){}});
        other.setOnClickListener(v->{for(Button button:weightButtons)tint(button,false);customBox.setVisibility(View.VISIBLE);customChanged.run();custom.requestFocus();});
        estimate=add("",14,false);
        save=choiceButton(t(editing==null?"save":"saveWeight"));save.setMinHeight(dp(52));save.setTextColor(ContextCompat.getColor(context,R.color.white));
        save.setBackgroundResource(R.drawable.calendar_find_address_button);save.setBackgroundTintList(null);save.setTextColor(ContextCompat.getColorStateList(context,R.color.calendar_find_address_text));body.addView(save);
        save.setOnClickListener(v->{
            if(grams==null||selected.isEmpty())return;
            try {
                if(editing!=null)store.edit(editing.id,grams);
                else {LinkedHashMap<String,Double> factors=new LinkedHashMap<>();for(String fraction:selected)factors.put(fraction,impact.factor(fraction,item.type,item.product));store.add(item.product,municipality,grams,factors);}
                save.performHapticFeedback(android.os.Build.VERSION.SDK_INT >= 30 ? HapticFeedbackConstants.CONFIRM : HapticFeedbackConstants.KEYBOARD_TAP);
                playSound();Toast.makeText(context,t("savedConfirmation"),Toast.LENGTH_SHORT).show();dialog.dismiss();onSaved.run();if(editing==null)AppReviewCoordinator.completedTask(context);
            }catch(Exception error){Toast.makeText(context,t("saveError"),Toast.LENGTH_LONG).show();}
        });
        if(editing!=null){grams=editing.weightGrams;boolean preset=false;for(int i=0;i<weights.length;i++){boolean match=weights[i]==grams;tint(weightButtons.get(i),match);preset|=match;}if(!preset){customBox.setVisibility(View.VISIBLE);custom.setText(String.valueOf(grams));}}
        dialog.setOnShowListener(ignored -> {
            View sheet = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (sheet != null) com.google.android.material.bottomsheet.BottomSheetBehavior.from(sheet)
                    .setState(com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED);
        });
        if (dialog.getWindow() != null) dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        update();
    }
    private LinearLayout customBox;
    private void selectDestination(Button selectedButton){for(Button button:destinationButtons)tint(button,button==selectedButton);}
    private void tint(Button button,boolean selected){button.setBackgroundTintList(android.content.res.ColorStateList.valueOf(selected?0xffdcebd7:0xffeeeeee));button.setTextColor(ContextCompat.getColor(context,R.color.text_color));}
    private void update(){
        if(save==null)return;
        save.setEnabled(grams!=null&&!selected.isEmpty());
        LinkedHashMap<String,Double> factors=new LinkedHashMap<>();for(String fraction:selected)factors.put(fraction,editing==null?impact.factor(fraction,item.type,item.product):editing.factors.get(fraction));
        double kg=grams==null?0:SortingImpact.estimate(grams,factors.values());
        if(selected.size()==1&&factors.values().iterator().next()==0){String f=selected.get(0);estimate.setText(t(f.equals("Restaffald")?"residualNoEstimate":f.equals("Farligt affald")?"hazardousNoEstimate":f.equals("Genbrugsplads")?"recyclingCenterNoEstimate":"materialNoEstimate"));}
        else estimate.setText(grams==null||selected.isEmpty()?"":kg>0?SortingImpactViews.amount(kg)+" CO₂e":t("noEstimate"));
    }
    private void playSound(){
        AudioManager audio=(AudioManager)context.getSystemService(Context.AUDIO_SERVICE);if(audio==null||audio.getRingerMode()!=AudioManager.RINGER_MODE_NORMAL)return;
        MediaPlayer player=MediaPlayer.create(context,R.raw.sorting_save);if(player==null)return;player.setVolume(.3f,.3f);player.setOnCompletionListener(MediaPlayer::release);player.setOnErrorListener((p,w,e)->{p.release();return true;});player.start();
    }
    private String category(String value){return LanguageManager.usesNonDanishContent(context)?db.translateSortingKey(value):value;}
    private String t(String key){return InfoPageText.get(context,"impact."+key);}
    private int dp(int n){return MunicipalityScreenViews.dp(context,n);}
    private TextView add(String text,int size,boolean bold){TextView v=MunicipalityScreenViews.label(context,text,size,bold,!bold);v.setPadding(0,dp(6),0,dp(8));body.addView(v);return v;}
    private Button choiceButton(String text){Button b=new androidx.appcompat.widget.AppCompatButton(context);b.setText(text);b.setTextSize(15);b.setAllCaps(false);b.setMinHeight(dp(52));android.graphics.drawable.GradientDrawable bg=new android.graphics.drawable.GradientDrawable();bg.setColor(0xffeeeeee);bg.setCornerRadius(dp(12));b.setBackground(bg);LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(-1,dp(54));params.setMargins(0,dp(4),0,dp(4));b.setLayoutParams(params);tint(b,false);return b;}
}
