package de.winterrettich.ninaradio.model;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;

public class Station implements Parcelable, Comparable<Station> {
    public String name;
    public String url;

    public Station(String name, String url) {
        this.name = name;
        this.url = url;
    }

    public Station(Parcel in) {
        this.name = in.readString();
        this.url = in.readString();
    }

    @Override
    public boolean equals(Object other) {
        if (other == null) return false;
        if (other == this) return true;
        if (!(other instanceof Station)) return false;
        Station otherStation = (Station) other;
        return name.equals(otherStation.name) && url.equals(otherStation.url);
    }

    @Override
    public int hashCode() {
        return name.hashCode() * url.hashCode();
    }

    @Override
    public String toString() {
        return name + "(" + url + ")";
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(name);
        dest.writeString(url);
    }

    @SuppressWarnings("unused")
    public static final Parcelable.Creator<Station> CREATOR = new Parcelable.Creator<Station>() {
        @Override
        public Station createFromParcel(Parcel in) {
            return new Station(in);
        }

        @Override
        public Station[] newArray(int size) {
            return new Station[size];
        }
    };

    @Override
    public int compareTo(@NonNull Station another) {
        // compare lexographically
        return name.compareToIgnoreCase(another.name);
    }
}
