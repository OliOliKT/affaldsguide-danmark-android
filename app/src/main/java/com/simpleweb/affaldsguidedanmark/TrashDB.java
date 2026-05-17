package com.simpleweb.affaldsguidedanmark;

import android.content.Context;
import android.content.res.Resources;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TrashDB {
    private static final String TAG = "TrashDB";

    private final Resources resources;
    public List<TrashItem> trashItems;

    public TrashDB(Resources resources) {
        this.resources = resources;
        try (InputStream inputStream = resources.openRawResource(R.raw.affald_data);
             InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
            Gson gson = new Gson();
            Type listType = new TypeToken<List<TrashItem>>() {}.getType();
            this.trashItems = gson.fromJson(reader, listType);
        } catch (Exception e) {
            Log.e(TAG, "Error loading JSON file", e);
            this.trashItems = null;
        }
    }

    public void searchProductJson(String what, boolean useEnglish, OnSearchCompleteListener listener) {
        if (trashItems == null) {
            Log.w(TAG, "JSON data is unavailable.");
            listener.onSearchComplete(Collections.singletonMap("error", ""));
            return;
        }

        Map<String, String> sorteringMap = new HashMap<>();
        for (TrashItem item : trashItems) {
            String productName = useEnglish && item.productEn != null && !item.productEn.isEmpty() ? item.productEn : item.product;
            if (productName != null && productName.equalsIgnoreCase(what)) {
                if (item.sorting != null) {
                    for (Map.Entry<String, String> entry : item.sorting.entrySet()) {
                        sorteringMap.put(entry.getKey(), useEnglish ? translateGuidance(entry.getValue()) : entry.getValue());
                    }
                }
                break;
            }
        }

        if (!sorteringMap.isEmpty()) {
            listener.onSearchComplete(sorteringMap);
        } else {
            listener.onSearchComplete(Collections.singletonMap("not found", ""));
        }
    }

    static class TrashItem {
        @SerializedName("Produkt")
        String product;

        @SerializedName("Produkt_en")
        String productEn;

        @SerializedName("Sortering")
        Map<String, String> sorting;

        @SerializedName("beskrivelse")
        String description;

        @SerializedName("beskrivelse_en")
        String descriptionEn;

        String getDisplayProduct(boolean useEnglish) {
            if (useEnglish && productEn != null && !productEn.isEmpty()) {
                return productEn;
            }
            return product;
        }
    }

    public interface OnSearchCompleteListener {
        void onSearchComplete(Map<String, String> sorteringMap);
    }

    public List<String> getProductNamesForCategory(String selectedTrashGroup, boolean useEnglish) {
        if (trashItems == null || selectedTrashGroup == null) {
            return Collections.emptyList();
        }

        String danishTrashGroup = useEnglish ? toDanishSortingKey(selectedTrashGroup) : selectedTrashGroup;
        List<String> productNames = new ArrayList<>();
        for (TrashItem item : trashItems) {
            String productName = item.getDisplayProduct(useEnglish);
            if (productName != null && item.sorting != null && item.sorting.containsKey(danishTrashGroup)) {
                productNames.add(productName);
            }
        }

        Collections.sort(productNames, String.CASE_INSENSITIVE_ORDER);
        return productNames;
    }

    public String getProductDescription(String productName, boolean useEnglish) {
        if (trashItems == null || productName == null) {
            return "";
        }

        for (TrashItem item : trashItems) {
            String displayProduct = item.getDisplayProduct(useEnglish);
            if (displayProduct != null && displayProduct.equalsIgnoreCase(productName)) {
                if (useEnglish && item.descriptionEn != null && !item.descriptionEn.isEmpty()) {
                    return item.descriptionEn;
                }
                return item.description != null ? item.description : "";
            }
        }

        return "";
    }

    public List<TrashType> getLocalTrashTypes(boolean useEnglish) {
        try (InputStream inputStream = resources.openRawResource(R.raw.affaldsfraktioner_data);
             InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
            Gson gson = new Gson();
            Type listType = new TypeToken<List<TrashTypeJson>>() {}.getType();
            List<TrashTypeJson> trashTypeJsonList = gson.fromJson(reader, listType);
            List<TrashType> trashTypes = new ArrayList<>();

            for (TrashTypeJson trashTypeJson : trashTypeJsonList) {
                TrashType trashType = new TrashType();
                trashType.setNavn(useEnglish && trashTypeJson.nameEn != null && !trashTypeJson.nameEn.isEmpty() ? trashTypeJson.nameEn : trashTypeJson.name);
                trashType.setDanishNavn(trashTypeJson.name);
                trashType.setBeskrivelse(useEnglish && trashTypeJson.descriptionEn != null && !trashTypeJson.descriptionEn.isEmpty() ? trashTypeJson.descriptionEn : trashTypeJson.description);
                trashType.setUdvidetBeskrivelse(useEnglish && trashTypeJson.extendedDescriptionEn != null && !trashTypeJson.extendedDescriptionEn.isEmpty() ? trashTypeJson.extendedDescriptionEn : trashTypeJson.extendedDescription);
                trashType.setPros(useEnglish && trashTypeJson.prosEn != null ? trashTypeJson.prosEn : trashTypeJson.pros != null ? trashTypeJson.pros : Collections.emptyList());
                trashType.setCons(useEnglish && trashTypeJson.consEn != null ? trashTypeJson.consEn : trashTypeJson.cons != null ? trashTypeJson.cons : Collections.emptyList());
                trashType.setImageResId(getImageResourceForName(trashTypeJson.imageResourceName));
                trashTypes.add(trashType);
            }

            return trashTypes;
        } catch (Exception e) {
            Log.e(TAG, "Error loading trash type JSON file", e);
            return Collections.emptyList();
        }
    }

    private static class TrashTypeJson {
        @SerializedName("Navn")
        String name;

        @SerializedName("Navn_en")
        String nameEn;

        @SerializedName("Beskrivelse")
        String description;

        @SerializedName("Beskrivelse_en")
        String descriptionEn;

        @SerializedName("UdvidetBeskrivelse")
        String extendedDescription;

        @SerializedName("UdvidetBeskrivelse_en")
        String extendedDescriptionEn;

        @SerializedName("pros")
        List<String> pros;

        @SerializedName("pros_en")
        List<String> prosEn;

        @SerializedName("cons")
        List<String> cons;

        @SerializedName("cons_en")
        List<String> consEn;

        @SerializedName("imageResourceName")
        String imageResourceName;
    }

    public void colorProductName(TextView textView, String text, Context context, String isSpecialText) {
        String firstLetter = text.substring(0, 1).toUpperCase();
        String remainingText = text.substring(1);
        String fullText;
        String whatToDo;

        if (isSpecialText.equals("vask")) {
            whatToDo = " kan afskaffes således:";
            fullText = firstLetter + remainingText + whatToDo;
        }
        else {
            whatToDo = " skal sorteres som: ";
            fullText = firstLetter + remainingText + whatToDo;
        }

        SpannableString spannableString = new SpannableString(fullText);
        int greenDarkColor = ContextCompat.getColor(context, R.color.green_light);
        ForegroundColorSpan greenColorSpan = new ForegroundColorSpan(greenDarkColor);
        spannableString.setSpan(greenColorSpan, 0, fullText.length() - whatToDo.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        textView.setText(spannableString);
    }

    public void colorProductName(TextView textView, String text, Context context, String isSpecialText, boolean useEnglish) {
        if (!useEnglish) {
            colorProductName(textView, text, context, isSpecialText);
            return;
        }

        String firstLetter = text.substring(0, 1).toUpperCase();
        String remainingText = text.substring(1);
        String whatToDo;

        if (isSpecialText.equals("vask")) {
            whatToDo = " should be disposed of like this:";
        } else {
            whatToDo = " should be sorted as: ";
        }

        String fullText = firstLetter + remainingText + whatToDo;
        SpannableString spannableString = new SpannableString(fullText);
        int greenDarkColor = ContextCompat.getColor(context, R.color.green_light);
        ForegroundColorSpan greenColorSpan = new ForegroundColorSpan(greenDarkColor);
        spannableString.setSpan(greenColorSpan, 0, fullText.length() - whatToDo.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        textView.setText(spannableString);
    }

    public void setImageViewAndText(ImageView imageView, TextView textView, Resources resources, String key, String value, boolean useEnglish) {
        int imageResource = getImageResourceForKey(key);
        imageView.setImageDrawable(ResourcesCompat.getDrawable(resources, imageResource, null));

        textView.setText(value);
    }

    int getImageResourceForKey(String key) {
        switch (key) {
            case "Madaffald":
            case "Food waste":
                return R.drawable.madaffald_ikon;
            case "Pap":
            case "Cardboard":
                return R.drawable.pap_ikon;
            case "Papir":
            case "Paper":
                return R.drawable.papir_ikon;
            case "Farligt affald":
            case "Hazardous waste":
                return R.drawable.farligt_affald_ikon;
            case "Plast":
            case "Plastic":
                return R.drawable.plast_ikon;
            case "Metal":
                return R.drawable.metal_ikon;
            case "Glas":
            case "Glass":
                return R.drawable.glas_ikon;
            case "Mad- og drikkekartoner":
            case "Food and beverage cartons":
                return R.drawable.mad_og_drikkekartoner_ikon;
            case "Tekstilaffald":
            case "Textile waste":
                return R.drawable.tekstilaffald_ikon;
            case "Restaffald":
            case "Residual waste":
                return R.drawable.restaffald_ikon;
            case "Genbrugsplads":
            case "Recycling centre":
                return R.drawable.genbrugsplads;
            case "Vask":
            case "Sink":
                return R.drawable.vask;
            case "Genbrug":
            case "Reuse":
                return R.drawable.genbrug;
            case "Politistation":
            case "Police station":
                return R.drawable.politistation;
            case "Apotek":
            case "Pharmacy":
                return R.drawable.apotek;
            case "Batterier":
            case "Batteries":
                return R.drawable.batterier;
            case "Småt elektronik":
            case "Small electronics":
                return R.drawable.smaat_elektronik;
            case "Pant":
            case "Deposit":
                return R.drawable.pant;
            default:
                return R.drawable.fejl;
        }
    }

    private int getImageResourceForName(String resourceName) {
        if (resourceName == null) {
            return R.drawable.fejl;
        }

        switch (resourceName) {
            case "madaffald_ikon":
                return R.drawable.madaffald_ikon;
            case "pap_ikon":
                return R.drawable.pap_ikon;
            case "papir_ikon":
                return R.drawable.papir_ikon;
            case "farligt_affald_ikon":
                return R.drawable.farligt_affald_ikon;
            case "plast_ikon":
                return R.drawable.plast_ikon;
            case "metal_ikon":
                return R.drawable.metal_ikon;
            case "glas_ikon":
                return R.drawable.glas_ikon;
            case "mad_og_drikkekartoner_ikon":
                return R.drawable.mad_og_drikkekartoner_ikon;
            case "tekstilaffald_ikon":
                return R.drawable.tekstilaffald_ikon;
            case "restaffald_ikon":
                return R.drawable.restaffald_ikon;
            case "batterier":
                return R.drawable.batterier;
            case "smaat_elektronik":
                return R.drawable.smaat_elektronik;
            default:
                return R.drawable.fejl;
        }
    }

    public String translateSortingKey(String key) {
        switch (key) {
            case "Madaffald": return "Food waste";
            case "Pap": return "Cardboard";
            case "Papir": return "Paper";
            case "Farligt affald": return "Hazardous waste";
            case "Plast": return "Plastic";
            case "Glas": return "Glass";
            case "Mad- og drikkekartoner": return "Food and beverage cartons";
            case "Tekstilaffald": return "Textile waste";
            case "Restaffald": return "Residual waste";
            case "Genbrugsplads": return "Recycling centre";
            case "Vask": return "Sink";
            case "Genbrug": return "Reuse";
            case "Politistation": return "Police station";
            case "Apotek": return "Pharmacy";
            case "Batterier": return "Batteries";
            case "Småt elektronik": return "Small electronics";
            case "Pant": return "Deposit";
            default: return key;
        }
    }

    public String toDanishSortingKey(String key) {
        switch (key) {
            case "Food waste": return "Madaffald";
            case "Cardboard": return "Pap";
            case "Paper": return "Papir";
            case "Hazardous waste": return "Farligt affald";
            case "Plastic": return "Plast";
            case "Glass": return "Glas";
            case "Food and beverage cartons": return "Mad- og drikkekartoner";
            case "Textile waste": return "Tekstilaffald";
            case "Residual waste": return "Restaffald";
            case "Recycling centre": return "Genbrugsplads";
            case "Sink": return "Vask";
            case "Reuse": return "Genbrug";
            case "Police station": return "Politistation";
            case "Pharmacy": return "Apotek";
            case "Batteries": return "Batterier";
            case "Small electronics": return "Småt elektronik";
            case "Deposit": return "Pant";
            default: return key;
        }
    }

    private String translateGuidance(String guidance) {
        switch (guidance) {
            case "Hele genstand": return "Whole item";
            case "Flaske": return "Bottle";
            case "Emballage": return "Packaging";
            case "Indhold": return "Contents";
            case "Plastfilm": return "Plastic film";
            case "Etui": return "Case";
            case "Bakke": return "Tray";
            case "Beholder": return "Container";
            case "Papir": return "Paper";
            case "Låg": return "Lid";
            case "Skal": return "Shell";
            case "Hvis ødelagt": return "If broken";
            case "Hvis ikke ødelagt": return "If not broken";
            case "Hvis små mængder": return "For small amounts";
            case "Hvis store mængder": return "For large amounts";
            case "Håndtag": return "Handle";
            case "Fjern batterier først": return "Remove batteries first";
            case "I lukket pose": return "In a closed bag";
            case "Kartonsvøb": return "Cardboard sleeve";
            case "Låg, folie og bæger": return "Lid, foil and cup";
            case "Forsegling": return "Seal";
            case "Folie": return "Foil";
            case "Bæger": return "Cup";
            default: return guidance;
        }
    }

}
