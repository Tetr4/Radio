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
        RadioApplication.sBus.post(PlaybackEvent.PLAY);
    }

    @Override
    public void onPause() {
        super.onPause();
        RadioApplication.sBus.post(PlaybackEvent.PAUSE);
    }

    @Override
    public void onStop() {
        super.onStop();
        RadioApplication.sBus.post(PlaybackEvent.STOP);
    }

    @Override
    public void onPlayFromMediaId(String mediaId, Bundle extras) {
        // TODO Resource
        Station station;
        if (extras.containsKey(RadioPlayerService.EXTRA_STATION)) {
            station = extras.getParcelable(RadioPlayerService.EXTRA_STATION);
        } else {
            station = new Station("Unknown Station", mediaId);
        }
        RadioApplication.sBus.post(new SelectStationEvent(station));
    }

    @Override
    public boolean onMediaButtonEvent(final Intent mediaButtonIntent) {
        // superclass calls appropriate onPlay/onPause etc.
        return super.onMediaButtonEvent(mediaButtonIntent);
    }
}