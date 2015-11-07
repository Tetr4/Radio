package de.winterrettich.ninaradio.model;

import android.support.annotation.NonNull;

import com.activeandroid.Model;
import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;

@Table(name = "Stations")
public class Station extends Model implements Comparable<Station> {

    @Column(name = "Name")
    public String name;

    @Column(name = "url")
    public String url;

    public Station() {
        super();
    }

    public Station(String name, String url) {
        super();
        this.name = name;
        this.url = url;
    }

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
