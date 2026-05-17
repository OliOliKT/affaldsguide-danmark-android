package com.simpleweb.affaldsguidedanmark;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class Municipality implements Parcelable {
    @SerializedName("Kommune")
    private String municipality;

    @SerializedName("Adresse")
    private String address;

    @SerializedName("Postnr")
    private String postalCode;

    @SerializedName("By")
    private String city;

    @SerializedName("Mailadresse")
    private String email;

    @SerializedName("URL")
    private String url;

    @SerializedName("Beskrivelse")
    private String description;

    @SerializedName("Affaldsregler")
    private String wasteRules;

    @SerializedName("Beskrivelse_en")
    private String descriptionEn;

    @SerializedName("Affaldsregler_en")
    private String wasteRulesEn;

    @SerializedName("Detaljer")
    private Details details;

    protected Municipality(Parcel in) {
        municipality = in.readString();
        address = in.readString();
        postalCode = in.readString();
        city = in.readString();
        email = in.readString();
        url = in.readString();
        description = in.readString();
        wasteRules = in.readString();
        descriptionEn = in.readString();
        wasteRulesEn = in.readString();
        details = in.readParcelable(Details.class.getClassLoader());
    }

    public static final Creator<Municipality> CREATOR = new Creator<Municipality>() {
        @Override
        public Municipality createFromParcel(Parcel in) {
            return new Municipality(in);
        }

        @Override
        public Municipality[] newArray(int size) {
            return new Municipality[size];
        }
    };

    public String getMunicipality() {
        return municipality;
    }

    public String getAddress() {
        return address;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public String getCity() {
        return city;
    }

    public String getEmail() {
        return email;
    }

    public String getUrl() {
        return url;
    }

    public String getDescription() {
        return description;
    }

    public String getDescription(boolean useEnglish) {
        if (useEnglish && descriptionEn != null && !descriptionEn.isEmpty()) {
            return descriptionEn;
        }
        return description;
    }

    public String getWasteRules() {
        return wasteRules;
    }

    public String getWasteRules(boolean useEnglish) {
        if (useEnglish && wasteRulesEn != null && !wasteRulesEn.isEmpty()) {
            return wasteRulesEn;
        }
        return wasteRules;
    }

    public Details getDetails() {
        return details;
    }

    public String getFullAddress() {
        return address + ", " + postalCode + " " + city;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(municipality);
        dest.writeString(address);
        dest.writeString(postalCode);
        dest.writeString(city);
        dest.writeString(email);
        dest.writeString(url);
        dest.writeString(description);
        dest.writeString(wasteRules);
        dest.writeString(descriptionEn);
        dest.writeString(wasteRulesEn);
        dest.writeParcelable(details, flags);
    }

    public static class Details implements Parcelable {
        @SerializedName("SidstTjekket")
        private String lastChecked;

        @SerializedName("KildeNote")
        private String sourceNote;

        @SerializedName("KildeNote_en")
        private String sourceNoteEn;

        @SerializedName("Ordninger")
        private List<Scheme> schemes;

        @SerializedName("HurtigeFakta")
        private List<QuickFact> quickFacts;

        @SerializedName("Links")
        private List<OfficialLink> links;

        protected Details(Parcel in) {
            lastChecked = in.readString();
            sourceNote = in.readString();
            sourceNoteEn = in.readString();
            schemes = in.createTypedArrayList(Scheme.CREATOR);
            quickFacts = in.createTypedArrayList(QuickFact.CREATOR);
            links = in.createTypedArrayList(OfficialLink.CREATOR);
        }

        public static final Creator<Details> CREATOR = new Creator<Details>() {
            @Override
            public Details createFromParcel(Parcel in) {
                return new Details(in);
            }

            @Override
            public Details[] newArray(int size) {
                return new Details[size];
            }
        };

        public String getLastChecked() {
            return lastChecked;
        }

        public String getSourceNote(boolean useEnglish) {
            if (useEnglish && sourceNoteEn != null && !sourceNoteEn.isEmpty()) {
                return sourceNoteEn;
            }
            return sourceNote;
        }

        public List<Scheme> getSchemes() {
            return schemes;
        }

        public List<QuickFact> getQuickFacts() {
            return quickFacts;
        }

        public List<OfficialLink> getLinks() {
            return links;
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(lastChecked);
            dest.writeString(sourceNote);
            dest.writeString(sourceNoteEn);
            dest.writeTypedList(schemes);
            dest.writeTypedList(quickFacts);
            dest.writeTypedList(links);
        }
    }

    public static class Scheme implements Parcelable {
        @SerializedName("Titel")
        private String title;

        @SerializedName("Beskrivelse")
        private String description;

        @SerializedName("Title_en")
        private String titleEn;

        @SerializedName("Description_en")
        private String descriptionEn;

        protected Scheme(Parcel in) {
            title = in.readString();
            description = in.readString();
            titleEn = in.readString();
            descriptionEn = in.readString();
        }

        public static final Creator<Scheme> CREATOR = new Creator<Scheme>() {
            @Override
            public Scheme createFromParcel(Parcel in) {
                return new Scheme(in);
            }

            @Override
            public Scheme[] newArray(int size) {
                return new Scheme[size];
            }
        };

        public String getTitle(boolean useEnglish) {
            if (useEnglish && titleEn != null && !titleEn.isEmpty()) {
                return titleEn;
            }
            return title;
        }

        public String getDescription(boolean useEnglish) {
            if (useEnglish && descriptionEn != null && !descriptionEn.isEmpty()) {
                return descriptionEn;
            }
            return description;
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(title);
            dest.writeString(description);
            dest.writeString(titleEn);
            dest.writeString(descriptionEn);
        }
    }

    public static class QuickFact implements Parcelable {
        @SerializedName("Label")
        private String label;

        @SerializedName("Vaerdi")
        private String value;

        @SerializedName("Label_en")
        private String labelEn;

        @SerializedName("Value_en")
        private String valueEn;

        protected QuickFact(Parcel in) {
            label = in.readString();
            value = in.readString();
            labelEn = in.readString();
            valueEn = in.readString();
        }

        public static final Creator<QuickFact> CREATOR = new Creator<QuickFact>() {
            @Override
            public QuickFact createFromParcel(Parcel in) {
                return new QuickFact(in);
            }

            @Override
            public QuickFact[] newArray(int size) {
                return new QuickFact[size];
            }
        };

        public String getLabel(boolean useEnglish) {
            if (useEnglish && labelEn != null && !labelEn.isEmpty()) {
                return labelEn;
            }
            return label;
        }

        public String getValue(boolean useEnglish) {
            if (useEnglish && valueEn != null && !valueEn.isEmpty()) {
                return valueEn;
            }
            return value;
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(label);
            dest.writeString(value);
            dest.writeString(labelEn);
            dest.writeString(valueEn);
        }
    }

    public static class OfficialLink implements Parcelable {
        @SerializedName("Titel")
        private String title;

        @SerializedName("Url")
        private String url;

        @SerializedName("Title_en")
        private String titleEn;

        protected OfficialLink(Parcel in) {
            title = in.readString();
            url = in.readString();
            titleEn = in.readString();
        }

        public static final Creator<OfficialLink> CREATOR = new Creator<OfficialLink>() {
            @Override
            public OfficialLink createFromParcel(Parcel in) {
                return new OfficialLink(in);
            }

            @Override
            public OfficialLink[] newArray(int size) {
                return new OfficialLink[size];
            }
        };

        public String getTitle(boolean useEnglish) {
            if (useEnglish && titleEn != null && !titleEn.isEmpty()) {
                return titleEn;
            }
            return title;
        }

        public String getUrl() {
            return url;
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(title);
            dest.writeString(url);
            dest.writeString(titleEn);
        }
    }
}
