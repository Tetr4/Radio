package de.winterrettich.ninaradio.event;

import android.util.Log;

import com.squareup.otto.Subscribe;

public class EventLogger {
    private static final String TAG = EventLogger.class.getSimpleName();

    @Subscribe
    public void handlePlaybackEvent(PlaybackEvent event) {
        Log.i(TAG, PlaybackEvent.class.getSimpleName() + ": " + event.name());
    }

    @Subscribe
    public void handleSelectStationEvent(SelectStationEvent event) {
        Log.i(TAG, SelectStationEvent.class.getSimpleName() + ": " +
                event.station.name + " -> " + event.station.url);
    }

    @Subscribe
    public void handleHeadphoneDisconnectEvent(HeadphoneDisconnectEvent event) {
        Log.i(TAG, HeadphoneDisconnectEvent.class.getSimpleName());
    }

    @Subscribe
    public void handleDismissNotificationEvent(DismissNotificationEvent event) {
        Log.i(TAG, DismissNotificationEvent.class.getSimpleName());
    }

    @Subscribe
    public void handleAdjustVolumeEvent(AdjustVolumeEvent event) {
        Log.i(TAG, AdjustVolumeEvent.class.getSimpleName() + ": " + event.volume);
    }

    @Subscribe
    public void handleAudioFocusEvent(AudioFocusEvent event) {
        Log.i(TAG, AudioFocusEvent.class.getSimpleName() + ": " + event.name());
    }

    @Subscribe
    public void handleBufferEvent(BufferEvent event) {
        Log.i(TAG, BufferEvent.class.getSimpleName() + ": " + event.name());
    }

    @Subscribe
    public void handleDatabaseEvent(DatabaseEvent event) {
        Log.i(TAG, DatabaseEvent.class.getSimpleName() + " (" + event.operation.name() + "): " +
                event.station.name + " -> " + event.station.url);
    }

    @Subscribe
    public void handlePlayerErrorEvent(PlayerErrorEvent event) {
        Log.i(TAG, PlayerErrorEvent.class.getSimpleName() + ": " + event.message);
    }

    @Subscribe
    public void handleMetadataEvent(MetadataEvent event) {
        Log.i(TAG, MetadataEvent.class.getSimpleName() + ": " + event.title);
    }

}
