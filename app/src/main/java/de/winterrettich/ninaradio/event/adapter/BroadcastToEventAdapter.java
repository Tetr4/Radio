package de.winterrettich.ninaradio.event.adapter;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.util.Log;

import de.winterrettich.ninaradio.RadioApplication;
import de.winterrettich.ninaradio.event.DismissNotificationEvent;
import de.winterrettich.ninaradio.event.HeadphoneDisconnectEvent;
import de.winterrettich.ninaradio.event.PlaybackEvent;
import de.winterrettich.ninaradio.service.RadioNotificationManager;

/**
 * Convert broadcasts to events
 */
public class BroadcastToEventAdapter extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(BroadcastToEventAdapter.class.getSimpleName(), "onReceive: " + intent.getAction());

        switch (intent.getAction()) {
            case AudioManager.ACTION_AUDIO_BECOMING_NOISY:
                RadioApplication.sBus.post(new HeadphoneDisconnectEvent());
                break;

            case RadioNotificationManager.ACTION_NOTIFICATION_DISMISS:
                RadioApplication.sBus.post(new DismissNotificationEvent());
                break;

            case RadioNotificationManager.ACTION_NOTIFICATION_PLAY:
                RadioApplication.sBus.post(PlaybackEvent.PLAY);
                break;

            case RadioNotificationManager.ACTION_NOTIFICATION_PAUSE:
                RadioApplication.sBus.post(PlaybackEvent.PAUSE);
                break;
        }
    }

}
