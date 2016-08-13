package de.winterrettich.ninaradio.model;

import android.content.SharedPreferences;
import android.util.Log;

import com.activeandroid.ActiveAndroid;
import com.activeandroid.query.Select;
import com.squareup.otto.Subscribe;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import de.winterrettich.ninaradio.event.BufferEvent;
import de.winterrettich.ninaradio.event.DatabaseEvent;
import de.winterrettich.ninaradio.event.PlaybackEvent;
import de.winterrettich.ninaradio.event.SelectStationEvent;

public class RadioDatabase {
    public static final String TAG = RadioDatabase.class.getSimpleName();
    public static final String PREF_FIRST_LAUNCH = "PREF_FIRST_LAUNCH";
    public static final String PREF_LAST_STATION_ID = "PREF_LAST_STATION_ID";
    public static final String PREF_LAST_STATE = "PREF_LAST_STATE";

    public PlaybackEvent playbackState = PlaybackEvent.STOP;
    public BufferEvent bufferingState = BufferEvent.BUFFERING;
    public Station selectedStation = null;

    private List<Station> mStations;
    private SharedPreferences mPreferences;

    public RadioDatabase(SharedPreferences prefs) {
        mPreferences = prefs;

        // check first launch
        boolean isFirstLaunch = mPreferences.getBoolean(PREF_FIRST_LAUNCH, true);
        // set to false next time
        mPreferences.edit().putBoolean(PREF_FIRST_LAUNCH, false).apply();

        initStations(isFirstLaunch);
        initLastStation();
        initLastState();
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
        // TODO database instead of preferences?
        if (mPreferences.contains(PREF_LAST_STATION_ID)) {
            long lastStationId = mPreferences.getLong(PREF_LAST_STATION_ID, -1);
            selectedStation = findStationById(lastStationId);
        }
    }

    private void initLastState() {
        // TODO database instead of preferences?
        if (mPreferences.contains(PREF_LAST_STATE)) {
            String lastState = mPreferences.getString(PREF_LAST_STATE, PlaybackEvent.STOP.name());
            playbackState = PlaybackEvent.valueOf(lastState);
        }
    }

    private void loadDefaultStations() {
        mStations = new LinkedList<>();
        mStations.add(new Station("Rock", "http://197.189.206.172:8000/stream"));
        mStations.add(new Station("Spanisch", "http://usa8-vn.mixstream.net:8138"));
        mStations.add(new Station("FFN", "http://player.ffn.de/ffnstream.mp3"));
        mStations.add(new Station("Antenne Niedersachsen", "http://stream.antenne.com/antenne-nds/mp3-128/radioplayer/"));
        mStations.add(new Station("1Live", "http://gffstream.ic.llnwd.net/stream/gffstream_stream_wdr_einslive_a"));
        mStations.add(new Station("Radio Gütersloh", "http://edge.live.mp3.mdn.newmedia.nacamar.net/radioguetersloh/livestream.mp3"));
        mStations.add(new Station("Rock2", "http://197.189.206.172:8000/stream"));
        mStations.add(new Station("Rock3", "http://197.189.206.172:8000/stream"));
        mStations.add(new Station("Rock4", "http://197.189.206.172:8000/stream"));
        mStations.add(new Station("Rock5", "http://197.189.206.172:8000/stream"));

        // save all
        ActiveAndroid.beginTransaction();
        try {
            for (Station station : mStations) {
                station.save();
            }
            ActiveAndroid.setTransactionSuccessful();
        } finally {
            ActiveAndroid.endTransaction();
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
        if (event.station != null) {
            mPreferences.edit().putLong(PREF_LAST_STATION_ID, event.station.getId()).apply();
        } else {
            mPreferences.edit().remove(PREF_LAST_STATION_ID).apply();
        }
    }

    @Subscribe
    public void handleBufferEvent(BufferEvent event) {
        bufferingState = event;
    }

    @Subscribe
    public void handlePlaybackEvent(PlaybackEvent event) {
        playbackState = event;
        SharedPreferences.Editor editor = mPreferences.edit();
        editor.putString(PREF_LAST_STATE, event.name());

        if (event == PlaybackEvent.STOP) {
            selectedStation = null;
            editor.remove(PREF_LAST_STATION_ID);
        }

        editor.apply();
    }

}
