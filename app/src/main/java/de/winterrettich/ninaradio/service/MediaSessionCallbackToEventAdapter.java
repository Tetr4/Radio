package de.winterrettich.ninaradio.service;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.media.session.MediaSessionCompat;

import de.winterrettich.ninaradio.RadioApplication;
import de.winterrettich.ninaradio.event.PlaybackEvent;
import de.winterrettich.ninaradio.event.SelectStationEvent;
import de.winterrettich.ninaradio.model.Station;

/**
 * Convert media session callbacks to events
 */
public class MediaSessionCallbackToEventAdapter extends MediaSessionCompat.Callback {

    @Override
    public void onPlay() {
        super.onPlay();
        RadioApplication.sBus.post(new PlaybackEvent(PlaybackEvent.Type.PLAY));
    }

    @Override
    public void onPause() {
        super.onPause();
        RadioApplication.sBus.post(new PlaybackEvent(PlaybackEvent.Type.PAUSE));
    }

    @Override
    public void onStop() {
        super.onStop();
        RadioApplication.sBus.post(new PlaybackEvent(PlaybackEvent.Type.STOP));
    }

    @Override
    public void onPlayFromMediaId(String mediaId, Bundle extras) {
        // FIXME
        //Station station = extras.getParcelable("station");
        // Log.e(TAG, "switch station" + station.name);
        RadioApplication.sBus.post(new SelectStationEvent(new Station("STATION NAME HERE", mediaId)));
    }

    @Override
    public boolean onMediaButtonEvent(final Intent mediaButtonIntent) {
        // superclass calls appropriate onPlay/onPause etc.
        return super.onMediaButtonEvent(mediaButtonIntent);
    }
}