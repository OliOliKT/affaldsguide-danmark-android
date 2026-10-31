package com.simpleweb.affaldsguidedanmark;

import android.app.Activity;
import android.content.*;
import android.net.Uri;
import android.widget.Toast;
import com.google.android.play.core.review.ReviewManager;
import com.google.android.play.core.review.ReviewManagerFactory;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.function.BooleanSupplier;

/** Stores engagement and request attempts locally; never claims that a review was submitted. */
final class AppReviewCoordinator {
    private static AppReviewCoordinator instance;
    private final SharedPreferences prefs;
    private boolean completedTask, attemptedThisSession;
    private AppReviewCoordinator(Context context) { prefs=context.getApplicationContext().getSharedPreferences("app_review_v1",Context.MODE_PRIVATE); }
    static synchronized AppReviewCoordinator get(Context context) {
        if(instance==null)instance=new AppReviewCoordinator(context);
        return instance;
    }
    void recordActiveDay() {
        long today=LocalDate.now().toEpochDay();
        SharedPreferences.Editor edit=prefs.edit();
        if(!prefs.contains("firstUse"))edit.putLong("firstUse",System.currentTimeMillis());
        int days=AppReviewPolicy.activeDays(prefs.getInt("activeDays",0),prefs.getLong("lastActiveDay",Long.MIN_VALUE),today);
        edit.putInt("activeDays",days).putLong("lastActiveDay",today).apply();
    }
    static void completedTask(Context context) {
        get(context).completedTask=true;
        Context current=context;
        while(current instanceof ContextWrapper) {
            if(current instanceof MainActivity){((MainActivity)current).completedReviewTask();return;}
            Context next=((ContextWrapper)current).getBaseContext();if(next==current)break;current=next;
        }
    }
    private String version(Context context) {
        try { android.content.pm.PackageInfo info=context.getPackageManager().getPackageInfo(context.getPackageName(),0);return info.versionName == null ? String.valueOf(info.getLongVersionCode()) : info.versionName; }
        catch(Exception error){return "unknown";}
    }
    boolean eligible(Context context) {
        return !attemptedThisSession && AppReviewPolicy.eligible(System.currentTimeMillis(),prefs.getLong("firstUse",0),prefs.getInt("activeDays",0),completedTask,
                prefs.getLong("lastAttempt",0),prefs.getString("lastVersion",""),version(context),ZoneId.systemDefault());
    }
    private void recordAttempt(Context context) {
        attemptedThisSession=true;
        prefs.edit().putLong("lastAttempt",System.currentTimeMillis()).putString("lastVersion",version(context)).apply();
    }
    void request(Activity activity,BooleanSupplier ready) {
        if(!eligible(activity)||!ready.getAsBoolean())return;
        attemptedThisSession=true; // At most one SDK request per process, including failures.
        try {
            ReviewManager manager=ReviewManagerFactory.create(activity);
            manager.requestReviewFlow().addOnCompleteListener(task->{
                if(!task.isSuccessful()||activity.isFinishing()||activity.isDestroyed()||!ready.getAsBoolean())return;
                recordAttempt(activity);
                // Google may decide not to display UI. Never fall back to an unsolicited store redirect.
                try { manager.launchReviewFlow(activity,task.getResult()).addOnCompleteListener(ignored->{}); }
                catch (RuntimeException ignored) { /* Optional review UI must never interrupt the app. */ }
            });
        } catch (RuntimeException ignored) { /* Keep the normal flow when the store service is unavailable. */ }
    }
    void openStore(Activity activity) {
        recordAttempt(activity); // A manual request also suppresses automatic prompting.
        String id=activity.getPackageName();
        try {
            activity.startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse("market://details?id="+id)).setPackage("com.android.vending"));
        } catch(ActivityNotFoundException error) {
            try {activity.startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse("https://play.google.com/store/apps/details?id="+id)));}
            catch(ActivityNotFoundException unavailable){Toast.makeText(activity,InfoPageText.get(activity,"review.openError"),Toast.LENGTH_LONG).show();}
        }
    }
}
