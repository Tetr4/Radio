package de.winterrettich.ninaradio;

import android.app.Application;
import android.content.Intent;

import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;
import com.squareup.otto.ThreadEnforcer;

import de.winterrettich.ninaradio.event.PlaybackEvent;
import de.winterrettich.ninaradio.event.SelectStationEvent;
import de.winterrettich.ninaradio.model.Station;
import de.winterrettich.ninaradio.service.RadioPlayerService;

public class RadioApplication extends Application {
    public static Bus sBus = new Bus(ThreadEnforcer.MAIN);
    public static PlaybackEvent.Type sPlaybackState = null;
    public static Station sStation;

    @Override
    public void onCreate() {
        super.onCreate();
        sBus.register(this);
    }

    @Subscribe
    public void handlePlaybackEvent(PlaybackEvent event) {
        sPlaybackState = event.type;

        switch (event.type) {
            case PLAY:
                if(sStation == null) {
                    throw new IllegalStateException("Select a Station before playing");
                }

                Intent serviceIntent = new Intent(this, RadioPlayerService.class);
                serviceIntent.putExtra("station", sStation);
                startService(serviceIntent);
                break;
//            case STOP:
//                Intent intent = new Intent(getApplicationContext(), RadioPlayerService.class);
//                stopService(intent);
//                break;
        }
    }

    @Subscribe
    public void handleSelectStationEvent(SelectStationEvent event) {
        sStation = event.station;
    }

}
