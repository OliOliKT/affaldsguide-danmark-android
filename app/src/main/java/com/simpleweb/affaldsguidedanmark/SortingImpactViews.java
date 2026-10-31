package com.simpleweb.affaldsguidedanmark;

import android.content.Context;
import android.graphics.*;
import android.view.View;
import android.widget.*;
import androidx.core.content.ContextCompat;
import java.text.NumberFormat;
import java.util.*;

final class SortingImpactViews {
    static String number(double value){NumberFormat f=NumberFormat.getNumberInstance();f.setMaximumFractionDigits(2);return f.format(value);}
    static String amount(double kg){return kg<1?number(kg*1000)+" g":number(kg)+" kg";}
    static String comparison(Context context,double kg,boolean chooseNew){
        if(kg<=0)return InfoPageText.get(context,"impact.comparison.empty");
        String[] keys={"car","flight","phone","light","television","petrol","kettle","oven"};
        double[] factors={.393/1.609344,.12,.001,.0004,.005,8.887/3.785411784,.006,2.0/60*.05};
        android.content.SharedPreferences prefs=context.getSharedPreferences("sorting_impact_v1",0);
        String previous=prefs.getString("comparison","");List<Integer> candidates=new ArrayList<>();
        for(int i=0;i<keys.length;i++){double n=kg/factors[i];if(n>=(i==1?100:1)&&n<=10000&&(!chooseNew||!keys[i].equals(previous)))candidates.add(i);}
        int index=0;
        if(!chooseNew){for(int i=0;i<keys.length;i++)if(keys[i].equals(previous))index=i;}
        else {if(!candidates.isEmpty())index=candidates.get(new Random().nextInt(candidates.size()));prefs.edit().putString("comparison",keys[index]).apply();}
        return InfoPageText.get(context,"impact.comparison."+keys[index]).replace("{0}",number(kg/factors[index]));
    }
    static LinearLayout summary(Context context,SortingLogStore store,boolean search){
        LinearLayout card=MunicipalityScreenViews.card(context);
        double kg=store.total();boolean empty=store.entries().isEmpty();
        if (search && empty) return emptySearchSummary(context, card);
        TextView title=MunicipalityScreenViews.label(context,InfoPageText.get(context,"impact."+(search?(empty?"searchEmptyTitle":"searchEstimate"):"estimate")),18,true,false);card.addView(title);
        if(!empty||!search){TextView value=MunicipalityScreenViews.label(context,"≈ "+amount(kg)+" CO₂e",30,true,false);value.setTextColor(ContextCompat.getColor(context,R.color.green_light));value.setPadding(0,8,0,8);card.addView(value);}
        card.addView(MunicipalityScreenViews.label(context,search&&empty?InfoPageText.get(context,"impact.searchEmptyPrompt"):comparison(context,kg,false),14,false,true));
        return card;
    }
    private static LinearLayout emptySearchSummary(Context context, LinearLayout card) {
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);

        FrameLayout badge = new FrameLayout(context);
        android.graphics.drawable.GradientDrawable badgeBackground = new android.graphics.drawable.GradientDrawable();
        badgeBackground.setShape(android.graphics.drawable.GradientDrawable.OVAL);
        badgeBackground.setColor(0xffedf4e9);
        badge.setBackground(badgeBackground);
        ImageView icon = new ImageView(context);
        icon.setImageResource(R.drawable.impact_summary_leaf);
        icon.setColorFilter(ContextCompat.getColor(context, R.color.green_light));
        icon.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        FrameLayout.LayoutParams iconParams = new FrameLayout.LayoutParams(dp(context, 24), dp(context, 24), android.view.Gravity.CENTER);
        badge.addView(icon, iconParams);
        row.addView(badge, new LinearLayout.LayoutParams(dp(context, 44), dp(context, 44)));

        LinearLayout copy = new LinearLayout(context);
        copy.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams copyParams = new LinearLayout.LayoutParams(0, -2, 1);
        copyParams.setMarginStart(dp(context, 12));
        copyParams.setMarginEnd(dp(context, 8));
        row.addView(copy, copyParams);

        TextView title = MunicipalityScreenViews.label(context,
                InfoPageText.get(context, "impact.page"), 16, true, false);
        copy.addView(title);
        TextView prompt = MunicipalityScreenViews.label(context,
                InfoPageText.get(context, "impact.searchEmptyPromptCompact"), 13, false, true);
        prompt.setPadding(0, dp(context, 4), 0, 0);
        prompt.setLineSpacing(dp(context, 2), 1);
        copy.addView(prompt);
        TextView link = MunicipalityScreenViews.label(context,
                InfoPageText.get(context, "impact.openOverview"), 13, true, false);
        link.setTextColor(ContextCompat.getColor(context, R.color.green_light));
        link.setPadding(0, dp(context, 7), 0, 0);
        copy.addView(link);

        ImageView arrow = new ImageView(context);
        arrow.setImageResource(R.drawable.arrow_right);
        arrow.setColorFilter(ContextCompat.getColor(context, R.color.grey_black));
        arrow.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        row.addView(arrow, new LinearLayout.LayoutParams(dp(context, 16), dp(context, 16)));
        card.addView(row);
        return card;
    }

    private static int dp(Context context, int value) { return MunicipalityScreenViews.dp(context, value); }

    static int color(Context context,String fraction){
        // Use the pictogram's dominant opaque colour, excluding its white glyph.
        Bitmap bitmap=BitmapFactory.decodeResource(context.getResources(),TrashDB.getImageResourceForKey(fraction,false));
        if(bitmap==null)return ContextCompat.getColor(context,R.color.green_light);
        Map<Integer,Integer> counts=new HashMap<>();int best=0,max=0;
        for(int y=0;y<bitmap.getHeight();y+=Math.max(1,bitmap.getHeight()/20))for(int x=0;x<bitmap.getWidth();x+=Math.max(1,bitmap.getWidth()/20)){
            int c=bitmap.getPixel(x,y);if(Color.alpha(c)<200||(Color.red(c)>235&&Color.green(c)>235&&Color.blue(c)>235))continue;
            int n=counts.getOrDefault(c,0)+1;counts.put(c,n);if(n>max){max=n;best=c;}
        }
        bitmap.recycle();return max>0?best:ContextCompat.getColor(context,R.color.green_light);
    }
    static final class DayCell extends androidx.appcompat.widget.AppCompatTextView {
        DayCell(Context context) { super(context); }
        @Override protected void onMeasure(int widthSpec, int heightSpec) {
            super.onMeasure(widthSpec, heightSpec);
            setMeasuredDimension(getMeasuredWidth(), getMeasuredWidth());
        }
    }
    @android.annotation.SuppressLint("ViewConstructor") // Only constructed programmatically with chart data.
    static final class TimeChart extends View {
        private final List<String> labels;private final List<Double> values;private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
        TimeChart(Context c,List<String> labels,List<Double> values){super(c);this.labels=labels;this.values=values;StringBuilder description=new StringBuilder(InfoPageText.get(c,"impact.climateAxis"));for(int i=0;i<labels.size();i++)description.append(". ").append(labels.get(i)).append(": ").append(number(values.get(i))).append(" kg CO₂e");setContentDescription(description.toString());setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);}
        protected void onDraw(Canvas canvas){super.onDraw(canvas);float density=getResources().getDisplayMetrics().density;float scaled=getResources().getDisplayMetrics().scaledDensity;
            float left=70*density,right=getWidth()-8*density,bottom=getHeight()-32*density,top=26*density;double max=Collections.max(values);if(max<=0)max=1;
            String axisTitle=InfoPageText.get(getContext(),"impact.climateAxis");
            paint.setTypeface(Typeface.DEFAULT);paint.setTextSize(12*scaled);paint.setTextAlign(Paint.Align.CENTER);paint.setColor(0xff666666);
            float axisLength=bottom-top;
            if(paint.measureText(axisTitle)>axisLength)paint.setTextSize(paint.getTextSize()*axisLength/paint.measureText(axisTitle));
            canvas.save();
            canvas.rotate(-90,12*density,(top+bottom)/2);
            canvas.drawText(axisTitle,12*density,(top+bottom)/2-(paint.ascent()+paint.descent())/2,paint);
            canvas.restore();
            paint.setTypeface(Typeface.DEFAULT);paint.setTextSize(10*scaled);paint.setTextAlign(Paint.Align.RIGHT);
            for(int i=0;i<=3;i++){float y=bottom-(bottom-top)*i/3;paint.setColor(0xffdddddd);canvas.drawLine(left,y,right,y,paint);paint.setColor(0xff666666);canvas.drawText(number(max*i/3),left-5*density,y+3*density,paint);}
            float step=(right-left)/values.size();
            for(int i=0;i<values.size();i++){float x=left+step*i+step*.16f,h=(float)(values.get(i)/max*(bottom-top));paint.setColor(ContextCompat.getColor(getContext(),R.color.green_light));canvas.drawRoundRect(x,bottom-h,x+step*.68f,bottom,3*density,3*density,paint);
                paint.setTextAlign(Paint.Align.CENTER);paint.setTypeface(Typeface.DEFAULT_BOLD);paint.setColor(h>24*density?Color.WHITE:0xff666666);canvas.drawText(number(values.get(i)),x+step*.34f,h>24*density?bottom-h/2+4*density:bottom-h-5*density,paint);
                paint.setTypeface(Typeface.DEFAULT);paint.setColor(0xff666666);canvas.drawText(labels.get(i),x+step*.34f,bottom+18*density,paint);
            }
        }
    }
}
