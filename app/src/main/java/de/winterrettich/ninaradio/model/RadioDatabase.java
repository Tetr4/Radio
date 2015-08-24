package de.winterrettich.ninaradio.model;

import android.util.Log;

import com.activeandroid.ActiveAndroid;
import com.activeandroid.query.Select;
import com.squareup.otto.Subscribe;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import de.winterrettich.ninaradio.RadioApplication;
import de.winterrettich.ninaradio.event.BufferEvent;
import de.winterrettich.ninaradio.event.DatabaseEvent;
import de.winterrettich.ninaradio.event.PlaybackEvent;
import de.winterrettich.ninaradio.event.SelectStationEvent;

public class RadioDatabase {
    public List<Station> mStations;
    public PlaybackEvent playbackState = PlaybackEvent.STOP;
    public BufferEvent bufferingState = BufferEvent.DONE;
    public Station selectedStation = null;

    public RadioDatabase() {
        if (RadioApplication.sIsFirstLaunch) {
            Log.d("DB", "Adding default stations");
            addDefaultStations();
        } else {
            Log.d("DB", "Loading from db");
            loadStationsFromDatabase();
        }
    }

    private void addDefaultStations() {
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
                // TODO update item with matching id?
                event.station.save();
                break;
        }

    }

    public int getStationsCount() {
        return mStations.size();
    }

    public List<Station> getStations() {
        return Collections.unmodifiableList(mStations);
    }

    @Subscribe
    public void handleSelectStationEvent(SelectStationEvent event) {
        selectedStation = event.station;
    }

    @Subscribe
    public void handleBufferEvent(BufferEvent event) {
        bufferingState = event;
    }

    @Subscribe
    public void handlePlaybackEvent(PlaybackEvent event) {
        playbackState = event;
        if (event == PlaybackEvent.STOP) {
            selectedStation = null;
        }
    }

}
