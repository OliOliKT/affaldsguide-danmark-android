package com.simpleweb.affaldsguidedanmark;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.List;

public class TrashType implements Parcelable {
    private String Navn;
    private String DanishNavn;
    private String Beskrivelse;
    private String UdvidetBeskrivelse;
    private List<String> pros;
    private List<String> cons;
    private int imageResId;

    public TrashType() {
    }

    public String getNavn() {
        return Navn;
    }

    public void setNavn(String name) {
        this.Navn = name;
    }

    public String getDanishNavn() {
        return DanishNavn != null && !DanishNavn.isEmpty() ? DanishNavn : Navn;
    }

    public void setDanishNavn(String danishName) {
        this.DanishNavn = danishName;
    }

    public String getBeskrivelse() {
        return Beskrivelse;
    }

    public void setBeskrivelse(String description) {
        this.Beskrivelse = description;
    }

    public String getUdvidetBeskrivelse() {
        return UdvidetBeskrivelse;
    }

    public void setUdvidetBeskrivelse(String extendedDescription) {
        this.UdvidetBeskrivelse = extendedDescription;
    }

    public List<String> getPros() {
        return pros;
    }

    public void setPros(List<String> pros) {
        this.pros = pros;
    }

    public List<String> getCons() {
        return cons;
    }

    public void setCons(List<String> cons) {
        this.cons = cons;
    }

    public int getImageResId() {
        return imageResId;
    }

    public void setImageResId(int imageResId) {
        this.imageResId = imageResId;
    }

    // Parcelable implementation
    protected TrashType(Parcel in) {
        Navn = in.readString();
        DanishNavn = in.readString();
        Beskrivelse = in.readString();
        UdvidetBeskrivelse = in.readString();
        imageResId = in.readInt();
        pros = in.createStringArrayList();
        cons = in.createStringArrayList();
    }

    public static final Creator<TrashType> CREATOR = new Creator<TrashType>() {
        @Override
        public TrashType createFromParcel(Parcel in) {
            return new TrashType(in);
        }

        @Override
        public TrashType[] newArray(int size) {
            return new TrashType[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(Navn);
        dest.writeString(DanishNavn);
        dest.writeString(Beskrivelse);
        dest.writeString(UdvidetBeskrivelse);
        dest.writeInt(imageResId);
        dest.writeStringList(pros);
        dest.writeStringList(cons);
    }

}
