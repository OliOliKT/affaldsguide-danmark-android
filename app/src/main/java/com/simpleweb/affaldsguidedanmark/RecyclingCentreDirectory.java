package com.simpleweb.affaldsguidedanmark;

import android.content.res.Resources;
import androidx.annotation.Keep;
import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.ArrayList;
import java.util.Locale;

final class RecyclingCentreDirectory {
    @Keep static final class Centre {
        @SerializedName("Navn") String name;
        @SerializedName("Adresse") String address;
        @SerializedName("Kommune") String municipality;
        @SerializedName("lat") Double latitude;
        @SerializedName("lng") Double longitude;
        boolean hasCoordinates() {
            return latitude != null && longitude != null && Double.isFinite(latitude) && Double.isFinite(longitude)
                    && Math.abs(latitude) <= 90 && Math.abs(longitude) <= 180 && (latitude != 0 || longitude != 0);
        }
        boolean matches(String query) {
            String content = (name + " " + address + " " + municipality).toLowerCase(Locale.ROOT);
            return content.contains(query.trim().toLowerCase(Locale.ROOT));
        }
    }
    static List<Centre> load(Resources resources) throws Exception {
        try (InputStreamReader reader = new InputStreamReader(resources.openRawResource(R.raw.genbrugspladser_data), StandardCharsets.UTF_8)) {
            List<Centre> values = new Gson().fromJson(reader,new TypeToken<List<Centre>>(){}.getType());
            List<Centre> valid = new ArrayList<>();
            if (values != null) for (Centre centre : values) {
                if (centre != null && centre.name != null && centre.address != null) valid.add(centre);
            }
            return valid;
        }
    }
}
