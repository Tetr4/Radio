package de.winterrettich.ninaradio.event.adapter;

import android.media.AudioManager;

import de.winterrettich.ninaradio.RadioApplication;
import de.winterrettich.ninaradio.event.AudioFocusEvent;

/**
 * Convert audio focus callbacks to events
 */
public class AudioFocusCallbackToEventAdapter implements AudioManager.OnAudioFocusChangeListener {

    @Override
    public void onAudioFocusChange(int focusChange) {
        switch (focusChange) {
            case AudioManager.AUDIOFOCUS_GAIN:
                RadioApplication.sBus.post(AudioFocusEvent.AUDIOFOCUS_GAIN);
                break;

            case AudioManager.AUDIOFOCUS_LOSS:
                RadioApplication.sBus.post(AudioFocusEvent.AUDIOFOCUS_LOSS);
                break;

            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                RadioApplication.sBus.post(AudioFocusEvent.AUDIOFOCUS_LOSS_TRANSIENT);
                break;

            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                RadioApplication.sBus.post(AudioFocusEvent.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK);
                break;
        }
    }
}
