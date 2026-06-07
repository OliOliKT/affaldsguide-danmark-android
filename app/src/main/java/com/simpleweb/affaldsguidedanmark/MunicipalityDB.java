package com.simpleweb.affaldsguidedanmark;

import android.content.res.Resources;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

public class MunicipalityDB {
    private static final String TAG = "MunicipalityDB";
    private final Resources resources;

    public MunicipalityDB(Resources resources) {
        this.resources = resources;
    }

    public List<Municipality> getMunicipalities() {
        try (InputStream inputStream = resources.openRawResource(R.raw.kommuner_data);
             InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
            Gson gson = new Gson();
            Type listType = new TypeToken<List<Municipality>>() {}.getType();
            List<Municipality> municipalities = gson.fromJson(reader, listType);
            if (municipalities == null) {
                return Collections.emptyList();
            }

            List<Municipality> validMunicipalities = new ArrayList<>();
            for (Municipality municipality : municipalities) {
                if (municipality != null
                        && municipality.getMunicipality() != null
                        && !municipality.getMunicipality().trim().isEmpty()) {
                    validMunicipalities.add(municipality);
                }
            }

            Collections.sort(validMunicipalities, (first, second) ->
                    String.CASE_INSENSITIVE_ORDER.compare(first.getMunicipality(), second.getMunicipality()));
            return validMunicipalities;
        } catch (Exception e) {
            Log.e(TAG, "Error loading municipality JSON file", e);
            return Collections.emptyList();
        }
    }
}
