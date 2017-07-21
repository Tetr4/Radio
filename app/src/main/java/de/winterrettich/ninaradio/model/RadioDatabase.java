package de.winterrettich.ninaradio.model;

import android.content.SharedPreferences;
import android.util.Log;

import com.activeandroid.ActiveAndroid;
import com.activeandroid.query.Select;
import com.activeandroid.sqlbrite.BriteDatabase;
import com.squareup.otto.Subscribe;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import de.winterrettich.ninaradio.event.BufferEvent;
import de.winterrettich.ninaradio.event.DatabaseEvent;
import de.winterrettich.ninaradio.event.PlaybackEvent;
import de.winterrettich.ninaradio.event.SelectStationEvent;

public class RadioDatabase {
    private static final String TAG = RadioDatabase.class.getSimpleName();
    public static final String PREF_FIRST_LAUNCH = "PREF_FIRST_LAUNCH";
    public static final String PREF_LAST_STATION_NAME = "PREF_LAST_STATION_NAME";
    public static final String PREF_LAST_STATION_URL = "PREF_LAST_STATION_URL";

    public PlaybackEvent playbackState = PlaybackEvent.STOP;
    public BufferEvent bufferingState = BufferEvent.BUFFERING;
    public Station selectedStation = null;

    private List<Station> mStations;
    private SharedPreferences mPreferences;

    public RadioDatabase(SharedPreferences prefs) {
        mPreferences = prefs;
        // check first launch
        boolean isFirstLaunch = prefs.getBoolean(PREF_FIRST_LAUNCH, true);
        // set to false next time
        prefs.edit().putBoolean(PREF_FIRST_LAUNCH, false).apply();

        initStations(isFirstLaunch);
        initLastStation();
    }

    private void initStations(boolean isFirstLaunch) {
        if (isFirstLaunch) {
            Log.d(TAG, "Adding default stations");
            loadDefaultStations();
        } else {
            Log.d(TAG, "Loading stations from database");
            loadStationsFromDatabase();
        }
    }

    private void initLastStation() {
        // load last station, e.g. when app is recreated after being terminated because of low memory
        if (mPreferences.contains(PREF_LAST_STATION_NAME)) {
            String lastStationName = mPreferences.getString(PREF_LAST_STATION_NAME, null);
            String lastStationUrl = mPreferences.getString(PREF_LAST_STATION_URL, null);
            selectedStation = findMatchingStation(lastStationName, lastStationUrl);
            if (selectedStation == null) {
                selectedStation = new Station(lastStationName, lastStationUrl);
            }
            playbackState = PlaybackEvent.PAUSE;
        }
    }

    private void loadDefaultStations() {
        mStations = new LinkedList<>();
        mStations.add(new Station("FFN", "http://player.ffn.de/ffnstream.mp3"));
        mStations.add(new Station("Antenne Niedersachsen", "http://stream.antenne.com/antenne-nds/mp3-128/radioplayer/"));
        mStations.add(new Station("1Live", "http://gffstream.ic.llnwd.net/stream/gffstream_stream_wdr_einslive_a"));
        mStations.add(new Station("Radio Gütersloh", "http://edge.live.mp3.mdn.newmedia.nacamar.net/radioguetersloh/livestream.mp3"));
        mStations.add(new Station("Der Barde", "http://stream.laut.fm/der-barde"));

        // save all
        BriteDatabase.Transaction transaction = ActiveAndroid.beginTransaction();
        try {
            for (Station station : mStations) {
                station.save();
            }
            ActiveAndroid.setTransactionSuccessful(transaction);
        } finally {
            ActiveAndroid.endTransaction(transaction);
        }
    }

    private void loadStationsFromDatabase() {
        mStations = new Select()
                .from(Station.class)
                .orderBy("Name ASC")
                .execute();
    }

    public Station findStationById(long id) {
        // TODO hashmap?
        for (Station station : mStations) {
            if (station.getId() == id) {
                return station;
            }
        }
        return null;
    }

    public Station findMatchingStation(Station station) {
        return findMatchingStation(station.name, station.url);
    }

    public Station findMatchingStation(String name, String url) {
        for (Station station : mStations) {
            if (station.name.equals(name) && station.url.equals(url)) {
                return station;
            }
        }
        if (selectedStation != null && selectedStation.name.equals(name) && selectedStation.url.equals(url)) {
            return selectedStation;
        }
        return null;
    }

    @Subscribe
    public void handleDatabaseEvent(DatabaseEvent event) {
        switch (event.operation) {
            case CREATE_STATION:
                event.station.save();
                mStations.add(event.station);
                break;
            case DELETE_STATION:
                mStations.remove(event.station);
                event.station.delete();
                break;
            case UPDATE_STATION:
                event.station.save();
                break;
        }

    }

    public List<Station> getStations() {
        // error when trying to modify
        return Collections.unmodifiableList(mStations);
    }

    @Subscribe
    public void handleSelectStationEvent(SelectStationEvent event) {
        selectedStation = event.station;
        if (selectedStation != null) {
            mPreferences.edit()
                    .putString(PREF_LAST_STATION_NAME, selectedStation.name)
                    .putString(PREF_LAST_STATION_URL, selectedStation.url)
                    .apply();
        } else {
            mPreferences.edit()
                    .remove(PREF_LAST_STATION_NAME)
                    .remove(PREF_LAST_STATION_URL)
                    .apply();
        }
    }

    @Subscribe
    public void handleBufferEvent(BufferEvent event) {
        bufferingState = event;
    }

    @Subscribe
    public void handlePlaybackEvent(PlaybackEvent event) {
        playbackState = event;
        if (playbackState == PlaybackEvent.STOP) {
            selectedStation = null;
            mPreferences.edit()
                    .remove(PREF_LAST_STATION_NAME)
                    .remove(PREF_LAST_STATION_URL)
                    .apply();
        }
    }

}
