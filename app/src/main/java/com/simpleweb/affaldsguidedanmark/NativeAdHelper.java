package com.simpleweb.affaldsguidedanmark;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.View;

import androidx.annotation.NonNull;

import com.google.android.ads.nativetemplates.NativeTemplateStyle;
import com.google.android.ads.nativetemplates.TemplateView;
import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.nativead.NativeAd;

final class NativeAdHelper {

    private static final String NATIVE_AD_UNIT_ID = "ca-app-pub-7562137360750567/1519469377";
    // Temporarily disabled. Set to true when ads should be shown again.
    private static final boolean ADS_ENABLED = false;

    private NativeAdHelper() {
    }

    static void loadNativeAd(@NonNull Context context, @NonNull View rootView) {
        TemplateView template = rootView.findViewById(R.id.my_template);
        if (!ADS_ENABLED) {
            if (template != null) {
                template.setVisibility(View.GONE);
            }
            return;
        }

        AdLoader adLoader = new AdLoader.Builder(context, NATIVE_AD_UNIT_ID)
                .forNativeAd(new NativeAd.OnNativeAdLoadedListener() {
                    @Override
                    public void onNativeAdLoaded(@NonNull NativeAd nativeAd) {
                        ColorDrawable background = new ColorDrawable(Color.WHITE);
                        NativeTemplateStyle styles = new NativeTemplateStyle.Builder()
                                .withMainBackgroundColor(background)
                                .build();
                        if (template != null) {
                            template.setVisibility(View.VISIBLE);
                            template.setStyles(styles);
                            template.setNativeAd(nativeAd);
                        }
                    }
                })
                .build();

        adLoader.loadAd(new AdRequest.Builder().build());
    }
}
