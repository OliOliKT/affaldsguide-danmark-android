package com.simpleweb.affaldsguidedanmark;

import android.content.Context;
import android.content.res.Resources;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Keep;
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
    private static final Map<String, String> GUIDANCE_TRANSLATIONS = createGuidanceTranslations();

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

    @Keep
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

    @Keep
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
        return GUIDANCE_TRANSLATIONS.getOrDefault(guidance, guidance);
    }

    private static Map<String, String> createGuidanceTranslations() {
        Map<String, String> translations = new HashMap<>();
        translations.put("Affyret", "Used");
        translations.put("Afkølet og indpakket", "Cooled and wrapped");
        translations.put("Bakke", "Tray");
        translations.put("Behandlet træ", "Treated wood");
        translations.put("Beholder", "Container");
        translations.put("BH uden metalbøjle", "Bra without underwire");
        translations.put("Bordplade i skifer", "Slate tabletop");
        translations.put("Brugte tændstikker", "Used matches");
        translations.put("Både brugt og ubrugt", "Used or unused");
        translations.put("Både udløst og ikke udløst", "Used or unused");
        translations.put("Cellofan og gavefolie", "Cellophane and gift foil");
        translations.put("Chipsrester", "Chip residues");
        translations.put("Cykelkabel", "Bike cable");
        translations.put("Cykelkæde", "Bike chain");
        translations.put("Cykelstel", "Bike frame");
        translations.put("Drænrør", "Drain pipe");
        translations.put("Dåse", "Can");
        translations.put("Dåser og flasker med pant", "Deposit cans and bottles");
        translations.put("Efter kommunens anvisning", "Follow municipal guidance");
        translations.put("Efter lokal anvisning", "Follow local guidance");
        translations.put("El-høvl og afretter", "Electric planer");
        translations.put("El-pumpe", "Electric pump");
        translations.put("Elektriske dele", "Electrical parts");
        translations.put("Emballage", "Packaging");
        translations.put("Emballage sorteres adskilt", "Sort packaging separately");
        translations.put("Emballage sorteres for sig", "Sort packaging separately");
        translations.put("Epoxy-maling lak", "Epoxy paint or varnish");
        translations.put("Etui", "Case");
        translations.put("Fisk og fiskerest", "Fish and fish scraps");
        translations.put("Fjern batteri", "Remove battery");
        translations.put("Fjern batteri først", "Remove battery first");
        translations.put("Fjern batterier først", "Remove batteries first");
        translations.put("Fjern løse batterier først", "Remove loose batteries first");
        translations.put("Flaske", "Bottle");
        translations.put("Flydende eller fast", "Liquid or solid");
        translations.put("Flyvehavre", "Wild oat");
        translations.put("Folie", "Foil");
        translations.put("Foliepapir", "Foil paper");
        translations.put("Forsegling", "Seal");
        translations.put("Forstærker", "Amplifier");
        translations.put("Fra flasker og emballageglas", "From bottles and glass packaging");
        translations.put("Fremkaldervæske", "Developer fluid");
        translations.put("Frugt og kerne", "Fruit and stone");
        translations.put("Fyrfadslys uden voksrester", "Tea light without wax");
        translations.put("Glas", "Glass");
        translations.put("Glasbeholder", "Glass container");
        translations.put("Græsafklip", "Grass clippings");
        translations.put("Grøntsagsskræl", "Vegetable peelings");
        translations.put("Hele autostolen", "Whole car seat");
        translations.put("Hele citrusfrugten", "Whole citrus fruit");
        translations.put("Hele genstand", "Whole item");
        translations.put("Hele genstanden", "Whole item");
        translations.put("Hele kortet", "Whole card");
        translations.put("Hele produktet", "Whole product");
        translations.put("Hele spanden", "Whole bucket");
        translations.put("Hvis 'Batterier' ikke er mulig", "If batteries is not possible");
        translations.put("Hvis af glas", "If made of glass");
        translations.put("Hvis af keramik", "If ceramic");
        translations.put("Hvis af metal", "If made of metal");
        translations.put("Hvis af metal og tømt", "If metal and empty");
        translations.put("Hvis af pap eller keramik", "If cardboard or ceramic");
        translations.put("Hvis af pap og ren", "If cardboard and clean");
        translations.put("Hvis af plast", "If made of plastic");
        translations.put("Hvis af plast og tømt", "If plastic and empty");
        translations.put("Hvis af plastik", "If made of plastic");
        translations.put("Hvis af træ", "If made of wood");
        translations.put("Hvis affyret", "If used");
        translations.put("Hvis almindeligt rent papir", "If ordinary clean paper");
        translations.put("Hvis andet", "If other material");
        translations.put("Hvis behandlet træ", "If treated wood");
        translations.put("Hvis beskidt", "If dirty");
        translations.put("Hvis beskidt eller belagt", "If dirty or coated");
        translations.put("Hvis beskidt/afvist", "If dirty or rejected");
        translations.put("Hvis beskidt/blandet", "If dirty or mixed");
        translations.put("Hvis blandede materialer", "If mixed materials");
        translations.put("Hvis blandet eller tung genstand", "If mixed or heavy");
        translations.put("Hvis brugt", "If used");
        translations.put("Hvis brugt eller fedtet", "If used or greasy");
        translations.put("Hvis brugt som affaldspose", "If used as waste bag");
        translations.put("Hvis brugt til mad", "If used for food");
        translations.put("Hvis brugt til mad eller drikke", "If used for food or drink");
        translations.put("Hvis brugt til rengøring", "If used for cleaning");
        translations.put("Hvis den er af metal", "If it is metal");
        translations.put("Hvis den er ødelagt", "If broken");
        translations.put("Hvis den kan bruges igen", "If reusable");
        translations.put("Hvis den kan være i beholderen", "If it fits the bin");
        translations.put("Hvis den kan være i ordningen", "If accepted locally");
        translations.put("Hvis der er kemikalierester", "If chemical residue remains");
        translations.put("Hvis der er malingrester", "If paint residue remains");
        translations.put("Hvis der er medicinrester", "If medicine residue remains");
        translations.put("Hvis der er piller tilbage", "If pills remain");
        translations.put("Hvis der er plads", "If there is room");
        translations.put("Hvis der er rester", "If residue remains");
        translations.put("Hvis der er væskerester", "If liquid remains");
        translations.put("Hvis der stadig er rester i", "If residue remains");
        translations.put("Hvis det er stort", "If large");
        translations.put("Hvis det er ødelagt", "If broken");
        translations.put("Hvis det kan bruges igen", "If reusable");
        translations.put("Hvis det passer i ordningen", "If accepted locally");
        translations.put("Hvis digitalt", "If digital");
        translations.put("Hvis elektrisk", "If electric");
        translations.put("Hvis emballage og ren", "If packaging and clean");
        translations.put("Hvis fedtet eller med madrester", "If greasy or food-stained");
        translations.put("Hvis for stor", "If too large");
        translations.put("Hvis for stor til elektronik", "If too large for electronics");
        translations.put("Hvis for stort til restaffald", "If too large for residual waste");
        translations.put("Hvis fra vape eller e-cigaret", "If from vape or e-cigarette");
        translations.put("Hvis få", "If few");
        translations.put("Hvis genbrugsplads ikke er mulig", "If recycling centre is not possible");
        translations.put("Hvis glittet/glitter", "If glossy or glittery");
        translations.put("Hvis helt ren", "If completely clean");
        translations.put("Hvis helt tom", "If completely empty");
        translations.put("Hvis helt tomt og rent", "If empty and clean");
        translations.put("Hvis hård plast modtages", "If hard plastic is accepted");
        translations.put("Hvis ikke accepteret lokalt", "If not accepted locally");
        translations.put("Hvis ikke af metal", "If not metal");
        translations.put("Hvis ikke af plast", "If not plastic");
        translations.put("Hvis ikke genanvendelig", "If not recyclable");
        translations.put("Hvis ikke lavet af PVC", "If not made of PVC");
        translations.put("Hvis ikke metal lokalt", "If metal is not accepted locally");
        translations.put("Hvis ikke småt elektronik", "If not small electronics");
        translations.put("Hvis ikke ødelagt", "If not broken");
        translations.put("Hvis kafferest ikke kan fjernes", "If coffee residue cannot be removed");
        translations.put("Hvis kommunen accepterer det", "If accepted locally");
        translations.put("Hvis lamineret eller plastbelagt", "If laminated or plastic-coated");
        translations.put("Hvis lamineret/glitter", "If laminated or glittery");
        translations.put("Hvis lavet af PVC", "If made of PVC");
        translations.put("Hvis lavet af metal", "If made of metal");
        translations.put("Hvis lavet af papir", "If made of paper");
        translations.put("Hvis lavet af plast", "If made of plastic");
        translations.put("Hvis lavet af plastik", "If made of plastic");
        translations.put("Hvis lavet af tekstil", "If made of textile");
        translations.put("Hvis lidt", "If a little");
        translations.put("Hvis lille", "If small");
        translations.put("Hvis lille genstand", "If small item");
        translations.put("Hvis lille kabel", "If small cable");
        translations.put("Hvis lille og beskidt", "If small and dirty");
        translations.put("Hvis lille og blandet materiale", "If small and mixed");
        translations.put("Hvis lille og med elektronik", "If small with electronics");
        translations.put("Hvis lille og uden elektronik", "If small without electronics");
        translations.put("Hvis lille og ødelagt", "If small and broken");
        translations.put("Hvis lille stueplante", "If small houseplant");
        translations.put("Hvis lyskilder afleveres der", "If light sources are accepted there");
        translations.put("Hvis madrester sidder fast", "If food residue is stuck");
        translations.put("Hvis mange", "If many");
        translations.put("Hvis mange/tykke kabler", "If many or thick cables");
        translations.put("Hvis med faremærker", "If hazard-labelled");
        translations.put("Hvis med kviksølv", "If containing mercury");
        translations.put("Hvis med maling, lim eller støv", "If with paint, glue or dust");
        translations.put("Hvis med metalinderside", "If metal-lined");
        translations.put("Hvis med pant", "If deposit-marked");
        translations.put("Hvis med plast", "If with plastic");
        translations.put("Hvis meget", "If a lot");
        translations.put("Hvis meget beskidt", "If very dirty");
        translations.put("Hvis mekanisk", "If mechanical");
        translations.put("Hvis mest metal", "If mostly metal");
        translations.put("Hvis mest plast", "If mostly plastic");
        translations.put("Hvis metalfolie", "If metal foil");
        translations.put("Hvis metalfolie/blandet", "If metal foil or mixed");
        translations.put("Hvis naturkork eller plast", "If natural cork or plastic");
        translations.put("Hvis papemballage", "If cardboard packaging");
        translations.put("Hvis plastbelagt eller beskidt", "If plastic-coated or dirty");
        translations.put("Hvis plastemballage", "If plastic packaging");
        translations.put("Hvis plastfolie", "If plastic film");
        translations.put("Hvis primært metal", "If mostly metal");
        translations.put("Hvis pumpe", "If pump");
        translations.put("Hvis på kartonen", "If on the carton");
        translations.put("Hvis ren", "If clean");
        translations.put("Hvis ren metaldel", "If clean metal part");
        translations.put("Hvis ren og tom", "If clean and empty");
        translations.put("Hvis ren og tør", "If clean and dry");
        translations.put("Hvis ren og uden belægning", "If clean and uncoated");
        translations.put("Hvis ren plastbakke/film", "If clean plastic tray/film");
        translations.put("Hvis ren plastfolie", "If clean plastic film");
        translations.put("Hvis ren plastpose", "If clean plastic bag");
        translations.put("Hvis rent", "If clean");
        translations.put("Hvis rent og accepteres lokalt", "If clean and accepted locally");
        translations.put("Hvis rent og tørt", "If clean and dry");
        translations.put("Hvis rent og uden belægning", "If clean and uncoated");
        translations.put("Hvis rent og uden plastbelægning", "If clean and not plastic-coated");
        translations.put("Hvis rent papir uden belægning", "If clean uncoated paper");
        translations.put("Hvis rent plastnet", "If clean plastic net");
        translations.put("Hvis rigtig perle", "If real pearl");
        translations.put("Hvis slidt eller beskidt", "If worn or dirty");
        translations.put("Hvis små metaldele modtages", "If small metal parts are accepted");
        translations.put("Hvis små mængder", "For small amounts");
        translations.put("Hvis småt og tilladt lokalt", "If small and accepted locally");
        translations.put("Hvis som haveaffald", "If garden waste");
        translations.put("Hvis spraydåse", "If aerosol can");
        translations.put("Hvis stor", "If large");
        translations.put("Hvis stor eller blandet", "If large or mixed");
        translations.put("Hvis stor eller med glas", "If large or with glass");
        translations.put("Hvis stor eller tung", "If large or heavy");
        translations.put("Hvis stor hård plast", "If large hard plastic");
        translations.put("Hvis stor/træaffald", "If large or wood waste");
        translations.put("Hvis store mængder", "For large amounts");
        translations.put("Hvis stort eller ødelagt", "If large or broken");
        translations.put("Hvis større genstand", "If larger item");
        translations.put("Hvis termopapir", "If thermal paper");
        translations.put("Hvis tom og blandet materiale", "If empty and mixed");
        translations.put("Hvis tom og ren", "If empty and clean");
        translations.put("Hvis tom og tør", "If empty and dry");
        translations.put("Hvis tom, ren og tør", "If empty, clean and dry");
        translations.put("Hvis tomt", "If empty");
        translations.put("Hvis tømt eller let rengjort", "If empty or lightly cleaned");
        translations.put("Hvis tømt og nogenlunde ren", "If empty and fairly clean");
        translations.put("Hvis ubrugt", "If unused");
        translations.put("Hvis ubrugt og rent", "If unused and clean");
        translations.put("Hvis uden faremærker", "If not hazard-labelled");
        translations.put("Hvis uden metalinderside", "If not metal-lined");
        translations.put("Hvis uden pant", "If no deposit");
        translations.put("Hvis uden plast", "If without plastic");
        translations.put("Hvis uden rester", "If no residue remains");
        translations.put("Hvis uld eller bomuld", "If wool or cotton");
        translations.put("Hvis våd eller beskidt", "If wet or dirty");
        translations.put("Hvis våd/beskidt", "If wet or dirty");
        translations.put("Hvis ødelagt", "If broken");
        translations.put("Hvis ødelagt/blandet", "If broken or mixed");
        translations.put("Håndtag", "Handle");
        translations.put("I lukket pose", "In a closed bag");
        translations.put("I skraldespand", "In a bin");
        translations.put("I supermarked", "At the supermarket");
        translations.put("Ikke emballage", "Not packaging");
        translations.put("Ikke emballageglas", "Not glass packaging");
        translations.put("Indeholder PVC", "Contains PVC");
        translations.put("Indhold", "Contents");
        translations.put("Indhold og emballage", "Contents and packaging");
        translations.put("Is-emballage", "Ice cream packaging");
        translations.put("Iturevet tøj", "Torn clothing");
        translations.put("Kaffegrums", "Coffee grounds");
        translations.put("Kaffepose", "Coffee bag");
        translations.put("Kaffepude", "Coffee pod");
        translations.put("Kartonsvøb", "Cardboard sleeve");
        translations.put("Kontakt pladsen først", "Contact the site first");
        translations.put("Kun hvis den er ren", "Only if clean");
        translations.put("Kun ren folie", "Only clean film");
        translations.put("Luftmadras", "Air mattress");
        translations.put("Lysrester og voks", "Candle residue and wax");
        translations.put("Låg", "Lid");
        translations.put("Låg, folie og bæger", "Lid, foil and cup");
        translations.put("Løst plastlåg", "Loose plastic lid");
        translations.put("Med malingrester", "With paint residue");
        translations.put("Med pant", "With deposit");
        translations.put("Metalbeholder", "Metal container");
        translations.put("Metalbøjle", "Metal underwire");
        translations.put("Metaldele", "Metal parts");
        translations.put("Metalfolie", "Metal foil");
        translations.put("Metallåg", "Metal lid");
        translations.put("Mindre mængder", "Small amounts");
        translations.put("Muldjord", "Topsoil");
        translations.put("Omslag", "Cover");
        translations.put("Organisk affald", "Organic waste");
        translations.put("Pak gerne i pose", "Preferably bag it");
        translations.put("Pap-del", "Cardboard part");
        translations.put("Pap-emballage", "Cardboard packaging");
        translations.put("Pap-æske", "Cardboard box");
        translations.put("Papir", "Paper");
        translations.put("Papkrus/coatede krus", "Cardboard/coated cups");
        translations.put("Paprulle", "Cardboard roll");
        translations.put("Paprør", "Cardboard tube");
        translations.put("Planterester", "Plant residue");
        translations.put("Plastdele", "Plastic parts");
        translations.put("Plastfilm", "Plastic film");
        translations.put("Plastik-del", "Plastic part");
        translations.put("Plastlåg", "Plastic lid");
        translations.put("Plastskaft", "Plastic handle");
        translations.put("Pose", "Bag");
        translations.put("Pose med hundelort", "Bag with dog waste");
        translations.put("Pose og tap", "Bag and tap");
        translations.put("Ren del", "Clean part");
        translations.put("Ren papir", "Clean paper");
        translations.put("Ren pose", "Clean bag");
        translations.put("Rene plastkrus", "Clean plastic cups");
        translations.put("Rent og tørt", "Clean and dry");
        translations.put("Rester af indhold", "Content residue");
        translations.put("Rester af sæbe", "Soap residue");
        translations.put("Rester og beholdere med indhold", "Residue and containers with contents");
        translations.put("Ringmekanisme", "Ring mechanism");
        translations.put("Rør", "Tube");
        translations.put("Rør og bund", "Tube and base");
        translations.put("Sammen med papiret", "Together with the paper");
        translations.put("Selve skummet", "The foam itself");
        translations.put("Siderne", "Pages");
        translations.put("Skal", "Shell");
        translations.put("Skal og kerne", "Peel and core");
        translations.put("Skal være helt tom", "Must be completely empty");
        translations.put("Skal være rengjort", "Must be clean");
        translations.put("Skrabelag", "Scratch layer");
        translations.put("Skræl og kerner", "Peel and seeds");
        translations.put("Små afklip", "Small cuttings");
        translations.put("Små døde dyr", "Small dead animals");
        translations.put("Små mængder", "Small amounts");
        translations.put("Små rester", "Small residues");
        translations.put("Spiralryg", "Spiral binding");
        translations.put("Stearin", "Candle wax");
        translations.put("Sten fra skorsten", "Chimney stone");
        translations.put("Store eller tunge glasdele", "Large or heavy glass parts");
        translations.put("Store mængder", "Large amounts");
        translations.put("Større batterier", "Larger batteries");
        translations.put("Større mængder", "Larger amounts");
        translations.put("Større mængder eller byggeaffald", "Large amounts or construction waste");
        translations.put("Særskilt fraktion", "Separate category");
        translations.put("Søm og beslag", "Nails and fittings");
        translations.put("Tagplade", "Roofing sheet");
        translations.put("Tagrende i zink", "Zinc gutter");
        translations.put("Termobon", "Thermal receipt");
        translations.put("Tom beholder", "Empty container");
        translations.put("Tom flaske", "Empty bottle");
        translations.put("Tom glasemballage", "Empty glass packaging");
        translations.put("Tom plastemballage", "Empty plastic packaging");
        translations.put("Tråd og folie", "Thread and foil");
        translations.put("Træskaft", "Wooden handle");
        translations.put("Tube", "Tube");
        translations.put("Tømt for indhold", "Emptied");
        translations.put("Tørt krydderiindhold", "Dry spice contents");
        translations.put("Ubrugt", "Unused");
        translations.put("Uden emballage", "Without packaging");
        translations.put("Uden jord og potte", "Without soil and pot");
        translations.put("Uden malingrester", "Without paint residue");
        translations.put("Uden pant", "Without deposit");
        translations.put("Ulovligt stof", "Illegal substance");
        translations.put("Varmebehandlet træ", "Heat-treated wood");
        translations.put("Vaskepulverrester", "Washing powder residue");
        translations.put("Vinduesramme i træ", "Wooden window frame");
        translations.put("Wrapfolie", "Wrap film");
        translations.put("Åbn og skrab fri fra kafferest", "Open and scrape out coffee residue");
        translations.put("Ærtebælg", "Pea pod");
        translations.put("Æske", "Box");
        return translations;
    }

}
