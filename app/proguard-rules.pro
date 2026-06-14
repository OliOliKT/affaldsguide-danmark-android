# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# Remove all logs in release builds
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
    public static *** w(...);
    public static *** e(...);
}

-keep class com.simpleweb.affaldsguidedanmark.TrashType { *; }

-keepattributes Signature
-keepattributes *Annotation*

-keepclassmembers class **.R$raw {
    public static final int affald_data;
    public static final int affaldsfraktioner_data;
    public static final int fraction_enhancements;
    public static final int genbrugspladser_data;
    public static final int kommuner_data;
}

-keep class com.simpleweb.affaldsguidedanmark.TrashDB$TrashItem {
    *;
}

-keep class com.simpleweb.affaldsguidedanmark.TrashDB$TrashTypeJson {
    *;
}

-keep class com.simpleweb.affaldsguidedanmark.Municipality { *; }
-keep class com.simpleweb.affaldsguidedanmark.Municipality$* { *; }
-keep class com.simpleweb.affaldsguidedanmark.MunicipalityDetailsFragment$RecyclingCenter { *; }
-keep class com.simpleweb.affaldsguidedanmark.FractionEnhancement { *; }
-keep class com.simpleweb.affaldsguidedanmark.FractionEnhancement$* { *; }

-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken

# WorkManager uses Room-generated database classes that are created by reflection.
# Keep them stable in release builds so AndroidX Startup can initialize WorkManager.
-keep class androidx.work.impl.WorkDatabase { *; }
-keep class androidx.work.impl.WorkDatabase_Impl { *; }
-keep class androidx.work.impl.model.** { *; }
-keep class * extends androidx.room.RoomDatabase { *; }
