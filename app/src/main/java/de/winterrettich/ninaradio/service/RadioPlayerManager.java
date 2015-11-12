package de.winterrettich.ninaradio.service;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.PowerManager;
import android.util.Log;

import java.io.IOException;

import de.winterrettich.ninaradio.RadioApplication;
import de.winterrettich.ninaradio.event.BufferEvent;
import de.winterrettich.ninaradio.event.PlaybackEvent;
import de.winterrettich.ninaradio.event.PlayerErrorEvent;
import de.winterrettich.ninaradio.model.Station;

/**
 * encapsulates a {@link MediaPlayer}
 */
public class RadioPlayerManager implements MediaPlayer.OnPreparedListener, MediaPlayer.OnInfoListener, MediaPlayer.OnErrorListener {
    public static final String TAG = RadioPlayerManager.class.getSimpleName();
    private Context mContext;
    private MediaPlayer mPlayer;
    private Station mStation;
    private boolean isPreparing = false;
    private boolean mustPrepare = true;

    public RadioPlayerManager(Context context) {
        mContext = context;
        initPlayer();

    }

    private void initPlayer() {
        mPlayer = new MediaPlayer();
        mPlayer.setWakeMode(mContext, PowerManager.PARTIAL_WAKE_LOCK);
        mPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mPlayer.setOnPreparedListener(this);
        mPlayer.setOnInfoListener(this);
        mPlayer.setOnErrorListener(this);
        Log.d(TAG, "State: idle");
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        Log.d(TAG, "State: prepared");
        RadioApplication.sBus.post(BufferEvent.DONE);
        isPreparing = false;
        // check if state changed in the meantime
        boolean correctState = RadioApplication.sDatabase.playbackState == PlaybackEvent.PLAY;
        if (correctState && !mp.isPlaying()) {
            mp.start();
            Log.d(TAG, "State: started");
        } else {
            Log.d(TAG, "Start canceled");
        }
    }

    public void play() {
        if (mStation == null) {
            throw new IllegalStateException("Select a Station before playing");
        }

        if (mPlayer.isPlaying()) {
            Log.d(TAG, "already playing");
        } else if (isPreparing) {
            Log.d(TAG, "already called prepareAsync");
        } else if (mustPrepare) {
            Log.d(TAG, "preparing async");
            isPreparing = true;
            RadioApplication.sBus.post(BufferEvent.BUFFERING);
            try {
                mPlayer.prepareAsync();
            } catch (IllegalStateException e) {
                // FIXME prepareAsync called in state 8
                RadioApplication.sBus.post(BufferEvent.DONE);
                restart();
                //RadioApplication.sBus.post(new PlayerErrorEvent("Could not prepare media player"));
            }
        } else {
            Log.d(TAG, "resume");
            mPlayer.start();
            Log.d(TAG, "State: started");
        }
    }

    public void pause() {
        if (mPlayer.isPlaying()) {
            Log.d(TAG, "pausing");
            mPlayer.pause();
            Log.d(TAG, "State: paused");
        } else {
            Log.d(TAG, "already paused");
        }
        mustPrepare = false;
    }

    public void stop() {
        Log.d(TAG, "stopping");
        // cancel starting after prepareAsync callback
        mPlayer.release();
        Log.d(TAG, "State: released");
    }

    private void restart() {
        if (mPlayer != null) {
            mPlayer.release();
        }

        initPlayer();
        switchStation(mStation);
        play();
    }

    public void switchStation(Station station) {
        mStation = station;
        if (mPlayer.isPlaying()) {
            Log.d(TAG, "stopping before switch");
            mPlayer.stop();
        }
        mPlayer.reset();
        mustPrepare = true;
        isPreparing = false;

        Log.d(TAG, "switching station");
        try {
            mPlayer.setDataSource(station.url);
        } catch (IOException e) {
            Log.e(TAG, "Could not switch station");
            restart();
        }
        Log.d(TAG, "State: initialized");
        if (RadioApplication.sDatabase.playbackState == PlaybackEvent.PLAY) {
            play();
        }
    }

    public void setVolume(float volume) {
        mPlayer.setVolume(volume, volume);
    }

    @Override
    public boolean onInfo(MediaPlayer mp, int what, int extra) {
        switch (what) {
            case MediaPlayer.MEDIA_INFO_BUFFERING_START:
                RadioApplication.sBus.post(BufferEvent.BUFFERING);
                break;
            case MediaPlayer.MEDIA_INFO_BUFFERING_END:
                RadioApplication.sBus.post(BufferEvent.DONE);
                break;
        }

        return false;
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        RadioApplication.sBus.post(BufferEvent.DONE);

        // TODO String resources
        switch (what) {
            case MediaPlayer.MEDIA_ERROR_IO:
                RadioApplication.sBus.post(new PlayerErrorEvent("MEDIA_ERROR_IO"));
                break;
            case MediaPlayer.MEDIA_ERROR_MALFORMED:
                RadioApplication.sBus.post(new PlayerErrorEvent("MEDIA_ERROR_MALFORMED"));
                break;
            case MediaPlayer.MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK:
                RadioApplication.sBus.post(new PlayerErrorEvent("MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK"));
                break;
            case MediaPlayer.MEDIA_ERROR_SERVER_DIED:
                Log.w(TAG, "Restarting Media Player after media server died");
                restart();
                break;
            case MediaPlayer.MEDIA_ERROR_TIMED_OUT:
                RadioApplication.sBus.post(new PlayerErrorEvent("MEDIA_ERROR_TIMED_OUT"));
                break;
            case MediaPlayer.MEDIA_ERROR_UNKNOWN:
                RadioApplication.sBus.post(new PlayerErrorEvent("MEDIA_ERROR_UNKNOWN"));
                break;
            case MediaPlayer.MEDIA_ERROR_UNSUPPORTED:
                RadioApplication.sBus.post(new PlayerErrorEvent("MEDIA_ERROR_UNSUPPORTED"));
                break;
        }

        return true;
    }
}
