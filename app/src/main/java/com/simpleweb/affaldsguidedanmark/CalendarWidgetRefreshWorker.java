package com.simpleweb.affaldsguidedanmark;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.work.*;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

/** Best-effort refresh while a calendar widget is installed; Android controls execution time. */
public final class CalendarWidgetRefreshWorker extends Worker {
    private static final String PERIODIC="calendar-widget-periodic",NOW="calendar-widget-refresh";
    public CalendarWidgetRefreshWorker(@NonNull Context context,@NonNull WorkerParameters params){super(context,params);}
    static void schedule(Context context,boolean immediate){
        if(!CalendarWidgetProvider.hasWidgets(context)||CalendarWidgetData.address(context)==null){cancel(context);return;}
        Constraints constraints=new Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build();
        WorkManager work=WorkManager.getInstance(context);
        work.enqueueUniquePeriodicWork(PERIODIC,ExistingPeriodicWorkPolicy.KEEP,new PeriodicWorkRequest.Builder(CalendarWidgetRefreshWorker.class,1,TimeUnit.HOURS).setConstraints(constraints).build());
        if(immediate)work.enqueueUniqueWork(NOW,ExistingWorkPolicy.KEEP,new OneTimeWorkRequest.Builder(CalendarWidgetRefreshWorker.class).setConstraints(constraints).build());
    }
    static void cancel(Context context){WorkManager work=WorkManager.getInstance(context);work.cancelUniqueWork(PERIODIC);work.cancelUniqueWork(NOW);}
    @NonNull @Override public Result doWork(){
        Context context=getApplicationContext();
        if(!CalendarWidgetProvider.hasWidgets(context))return Result.success();
        CalendarApi.Address address=CalendarWidgetData.address(context);
        if(address==null){CalendarWidgetProvider.updateAll(context);return Result.success();}
        long started=System.currentTimeMillis();
        try{
            List<CalendarApi.Pickup> events=CalendarApi.pickups(address);
            synchronized (CalendarWidgetData.class) {
                if(CalendarWidgetData.save(context,address,events,started)){
                    if(CalendarWidgetData.prefs(context).getBoolean("reminders_enabled",false))CalendarReminderWorker.schedule(context,events,
                            CalendarWidgetData.prefs(context).getStringSet("excluded_labels",Collections.emptySet()),CalendarWidgetData.prefs(context).getString("reminder_time","eveningBefore"));
                }
            }
            CalendarWidgetProvider.updateAll(context);return Result.success();
        }catch(Exception error){CalendarWidgetProvider.updateAll(context);return getRunAttemptCount()<2?Result.retry():Result.success();}
    }
}
