package com.simpleweb.affaldsguidedanmark;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

/** Durable, best-effort local reminders. Android may defer delivery to save battery. */
public final class CalendarReminderWorker extends Worker {
    private static final String TAG = "calendar-reminders";
    private static final String CHANNEL = "waste_collections";

    public CalendarReminderWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull @Override public Result doWork() {
        Context context = getApplicationContext();
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) return Result.success();
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null) return Result.success();
        manager.createNotificationChannel(new NotificationChannel(CHANNEL,
                InfoPageText.get(context, "calendar.title"), NotificationManager.IMPORTANCE_DEFAULT));
        String titleKey = getInputData().getBoolean("today", false) ? "calendar.reminderTitleToday" : "calendar.reminderTitle";
        String body = InfoPageText.get(context, "calendar.reminderBody")
                .replace("{0}", CalendarPickupLabels.display(context, getInputData().getString("labels") == null ? "" : getInputData().getString("labels")));
        Intent openCalendar = new Intent(context, MainActivity.class)
                .putExtra("open_calendar", true)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent tap = PendingIntent.getActivity(context, 0, openCalendar,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        manager.notify(getInputData().getInt("id", 0), new NotificationCompat.Builder(context, CHANNEL)
                .setSmallIcon(R.drawable.bottom_menu_calendar_icon)
                .setContentTitle(InfoPageText.get(context, titleKey))
                .setContentText(body)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
                .setContentIntent(tap)
                .setAutoCancel(true)
                .build());
        return Result.success();
    }

    static void clear(Context context) {
        WorkManager.getInstance(context).cancelAllWorkByTag(TAG);
    }

    static void schedule(Context context, List<CalendarApi.Pickup> pickups, Set<String> excluded, String time) {
        clear(context);
        Map<LocalDate, List<String>> days = new TreeMap<>();
        for (CalendarApi.Pickup pickup : pickups) {
            if (excluded.contains(pickup.label)) continue;
            try {
                LocalDate day = LocalDate.parse(pickup.date);
                List<String> labels = days.computeIfAbsent(day, ignored -> new ArrayList<>());
                if (!labels.contains(pickup.label)) labels.add(pickup.label);
            } catch (Exception ignored) { }
        }
        int count = 0;
        for (Map.Entry<LocalDate, List<String>> entry : days.entrySet()) {
            if (count++ >= 25) break;
            boolean today = "pickupMorning".equals(time);
            LocalDate day = today ? entry.getKey() : entry.getKey().minusDays(1);
            int hour = "eveningBefore".equals(time) ? 18 : 9;
            long delay = LocalDateTime.of(day, java.time.LocalTime.of(hour, 0))
                    .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() - System.currentTimeMillis();
            if (delay <= 0) continue;
            String id = entry.getKey().toString();
            Data data = new Data.Builder().putString("labels", String.join(", ", entry.getValue()))
                    .putBoolean("today", today).putInt("id", id.hashCode()).build();
            OneTimeWorkRequest work = new OneTimeWorkRequest.Builder(CalendarReminderWorker.class)
                    .setInitialDelay(delay, TimeUnit.MILLISECONDS).setInputData(data).addTag(TAG).build();
            WorkManager.getInstance(context).enqueueUniqueWork("calendar-reminder-" + id, ExistingWorkPolicy.REPLACE, work);
        }
    }
}
