package com.simpleweb.affaldsguidedanmark;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.RemoteViews;

public class PhotoSearchWidgetProvider extends AppWidgetProvider {
    public static final String EXTRA_SHORTCUT = "widget_shortcut";
    public static final String SHORTCUT_SEARCH = "search";
    public static final String SHORTCUT_CAMERA = "camera";

    @Override
    public void onUpdate(Context context, AppWidgetManager manager, int[] appWidgetIds) {
        for (int id : appWidgetIds) update(context, manager, id);
    }

    @Override public void onAppWidgetOptionsChanged(Context context, AppWidgetManager manager, int id, Bundle options) {
        update(context, manager, id);
    }

    public static void updateAll(Context context) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        ComponentName component = new ComponentName(context, PhotoSearchWidgetProvider.class);
        int[] ids = manager.getAppWidgetIds(component);
        for (int id : ids) update(context, manager, id);
    }

    private static void update(Context context, AppWidgetManager manager, int id) {
        Context localized = LanguageManager.wrapContext(context);
        boolean compact = manager.getAppWidgetOptions(id).getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 160) < 130;
        RemoteViews views = new RemoteViews(context.getPackageName(), compact ? R.layout.widget_photo_search_compact : R.layout.widget_photo_search);
        views.setInt(R.id.widgetRoot, "setLayoutDirection", LanguageManager.isArabic(context) ? View.LAYOUT_DIRECTION_RTL : View.LAYOUT_DIRECTION_LTR);
        views.setTextViewText(R.id.widgetTitle, localized.getString(R.string.widget_title));
        views.setTextViewText(R.id.widgetSearch, localized.getString(R.string.widget_search));
        views.setTextViewText(R.id.widgetCamera, localized.getString(R.string.widget_camera));
        views.setContentDescription(R.id.widgetSearch, localized.getString(R.string.widget_search));
        views.setContentDescription(R.id.widgetCamera, localized.getString(R.string.widget_camera));
        views.setOnClickPendingIntent(R.id.widgetSearchAction, shortcutIntent(context, id, SHORTCUT_SEARCH, 0));
        views.setOnClickPendingIntent(R.id.widgetCameraAction, shortcutIntent(context, id, SHORTCUT_CAMERA, 1));
        manager.updateAppWidget(id, views);
    }

    private static PendingIntent shortcutIntent(Context context, int id, String shortcut, int offset) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.putExtra(EXTRA_SHORTCUT, shortcut);
        intent.setData(android.net.Uri.parse("affaldsguide://widget/" + id + "/" + shortcut));
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        return PendingIntent.getActivity(context, id * 2 + offset, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }
}
