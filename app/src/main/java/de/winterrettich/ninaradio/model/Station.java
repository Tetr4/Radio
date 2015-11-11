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
    public int compareTo(@NonNull Station another) {
        // compare lexographically
        return name.compareToIgnoreCase(another.name);
    }

    @Override
    public String toString() {
        return name + "(" + url + ")";
    }

}
