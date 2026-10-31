package com.simpleweb.affaldsguidedanmark;

import android.app.PendingIntent;
import android.appwidget.*;
import android.content.*;
import android.os.Bundle;
import android.view.View;
import android.widget.RemoteViews;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

public final class CalendarWidgetProvider extends AppWidgetProvider {
    private static final int[] ROWS={R.id.calendarWidgetRow0,R.id.calendarWidgetRow1,R.id.calendarWidgetRow2,R.id.calendarWidgetRow3};
    private static final int[] LABELS={R.id.calendarWidgetLabel0,R.id.calendarWidgetLabel1,R.id.calendarWidgetLabel2,R.id.calendarWidgetLabel3};
    private static final int[] DATES={R.id.calendarWidgetDate0,R.id.calendarWidgetDate1,R.id.calendarWidgetDate2,R.id.calendarWidgetDate3};
    static boolean hasWidgets(Context context){return AppWidgetManager.getInstance(context).getAppWidgetIds(new ComponentName(context,CalendarWidgetProvider.class)).length>0;}
    @Override public void onUpdate(Context context,AppWidgetManager manager,int[] ids){updateAll(context);CalendarWidgetRefreshWorker.schedule(context,true);}
    @Override public void onEnabled(Context context){CalendarWidgetRefreshWorker.schedule(context,true);}
    @Override public void onDisabled(Context context){CalendarWidgetRefreshWorker.cancel(context);}
    @Override public void onAppWidgetOptionsChanged(Context context,AppWidgetManager manager,int id,Bundle options){update(context,manager,id);}
    @Override public void onReceive(Context context,Intent intent){super.onReceive(context,intent);if(Intent.ACTION_DATE_CHANGED.equals(intent.getAction())||Intent.ACTION_TIMEZONE_CHANGED.equals(intent.getAction()))updateAll(context);}
    static void addressChanged(Context context){updateAll(context);CalendarWidgetRefreshWorker.schedule(context,false);}
    static void updateAll(Context context){AppWidgetManager manager=AppWidgetManager.getInstance(context);for(int id:manager.getAppWidgetIds(new ComponentName(context,CalendarWidgetProvider.class)))update(context,manager,id);}
    private static void update(Context context,AppWidgetManager manager,int id){
        Context localized=LanguageManager.wrapContext(context);
        RemoteViews views=new RemoteViews(context.getPackageName(),R.layout.widget_calendar);
        boolean wide=manager.getAppWidgetOptions(id).getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH,160)>=260;
        views.setViewVisibility(R.id.calendarWidgetHeaderIcon,wide?View.VISIBLE:View.GONE);
        views.setTextViewText(R.id.calendarWidgetTitle,localized.getString(R.string.widget_calendar_name));
        views.setTextViewTextSize(R.id.calendarWidgetTitle,android.util.TypedValue.COMPLEX_UNIT_SP,wide?18:15);
        boolean rtl=LanguageManager.isArabic(context);
        views.setInt(R.id.calendarWidgetRoot,"setLayoutDirection",rtl?View.LAYOUT_DIRECTION_RTL:View.LAYOUT_DIRECTION_LTR);
        CalendarApi.Address address=CalendarWidgetData.address(context);
        CalendarWidgetData.Cache cache=CalendarWidgetData.cache(context,address);
        List<CalendarApi.Pickup> events=cache==null?Collections.emptyList():CalendarWidgetData.upcoming(cache.events,LocalDate.now());
        views.setViewVisibility(R.id.calendarWidgetAddress,address==null?View.GONE:View.VISIBLE);
        views.setTextViewText(R.id.calendarWidgetAddress,address==null?"":address.label);
        views.setViewVisibility(R.id.calendarWidgetRows,events.isEmpty()?View.GONE:View.VISIBLE);
        views.setViewVisibility(R.id.calendarWidgetEmpty,events.isEmpty()?View.VISIBLE:View.GONE);
        views.setViewVisibility(R.id.calendarWidgetPin,address==null?View.VISIBLE:View.GONE);
        views.setTextViewText(R.id.calendarWidgetMessage,localized.getString(address==null?R.string.widget_calendar_choose_address:cache==null?R.string.widget_calendar_unavailable:R.string.widget_calendar_empty));
        Locale locale=new Locale(LanguageManager.getSavedLanguage(context));
        DateTimeFormatter dateFormat=DateTimeFormatter.ofPattern(locale.getLanguage().equals("da")?"d. MMM":"d MMM",locale);
        for(int i=0;i<4;i++){
            views.setViewVisibility(ROWS[i],i<events.size()?View.VISIBLE:View.GONE);
            if(i>=events.size())continue;
            CalendarApi.Pickup event=events.get(i);
            String name=CalendarPickupLabels.display(context,event.label),date=LocalDate.parse(event.date).format(dateFormat);
            views.setTextViewText(LABELS[i],name);views.setTextViewText(DATES[i],date);
            views.setTextViewTextSize(LABELS[i],android.util.TypedValue.COMPLEX_UNIT_SP,wide?14:12);
            views.setContentDescription(ROWS[i],name+", "+LocalDate.parse(event.date).format(DateTimeFormatter.ofLocalizedDate(java.time.format.FormatStyle.LONG).withLocale(locale)));
        }
        Intent open=new Intent(context,MainActivity.class).putExtra("open_calendar",true).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP);
        open.setData(android.net.Uri.parse("affaldsguide://widget/calendar/"+id));
        views.setOnClickPendingIntent(R.id.calendarWidgetRoot,PendingIntent.getActivity(context,id,open,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE));
        manager.updateAppWidget(id,views);
    }
}
