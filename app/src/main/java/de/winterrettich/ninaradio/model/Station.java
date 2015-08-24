package de.winterrettich.ninaradio.model;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;

import com.activeandroid.Model;
import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;

@Table(name = "Stations")
public class Station extends Model implements Parcelable, Comparable<Station> {

    @Column(name = "Name")
    public String name;

    @Column(name = "url")
    public String url;

    public Station() {
        super();
    }

    public Station(Parcel in) {
        super();
        this.name = in.readString();
        this.url = in.readString();
    }

    public Station(String name, String url) {
        super();
        this.name = name;
        this.url = url;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(name);
        dest.writeString(url);
    }

    @Override
    public int describeContents() {
        return 0;
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
    public boolean equals(Object other) {
        if (other == null) return false;
        if (other == this) return true;
        if (!(other instanceof Station)) return false;
        Station otherStation = (Station) other;
        return getId().equals(otherStation.getId());
    }

    @Override
    public int hashCode() {
        return getId().hashCode();
    }

    @Override
    public int compareTo(@NonNull Station another) {
        // compare lexographically
        return name.compareToIgnoreCase(another.name);
    }

    @Override
    public String toString() {
        return name + "(" + url + ")";
    }

}
