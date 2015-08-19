package de.winterrettich.ninaradio.service;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.PowerManager;
import android.util.Log;

import java.io.IOException;

import de.winterrettich.ninaradio.RadioApplication;
import de.winterrettich.ninaradio.event.BufferEvent;
import de.winterrettich.ninaradio.model.Station;

/**
 * encapsulates a {@link MediaPlayer}
 */
public class RadioPlayerManager implements MediaPlayer.OnPreparedListener {
    public static final String TAG = RadioPlayerManager.class.getSimpleName();
    private Context mContext;
    private MediaPlayer mPlayer;
    private Station mStation;
    private boolean isPreparing = false;
    private boolean isPaused = false;
    private boolean cancelStart = false;

    public RadioPlayerManager(Context context) {
        mContext = context;
        initPlayer();
    }

    private void initPlayer() {
        mPlayer = new MediaPlayer();
        mPlayer.setWakeMode(mContext, PowerManager.PARTIAL_WAKE_LOCK);
        mPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mPlayer.setOnPreparedListener(this);
        Log.d(TAG, "State: idle");
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        Log.d(TAG, "State: prepared");
        RadioApplication.sBus.post(BufferEvent.DONE);
        isPreparing = false;
        if (!cancelStart && !mp.isPlaying()) {
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

        cancelStart = false;

        if (mPlayer.isPlaying()) {
            Log.d(TAG, "already playing");
        } else if (isPreparing) {
            Log.d(TAG, "already called prepareAsync");
        } else if (isPaused) {
            Log.d(TAG, "resume");
            mPlayer.start();
            Log.d(TAG, "State: started");
        } else {
            Log.d(TAG, "preparing async");
            isPreparing = true;
            // TODO try ExoPlayer, because MediaPlayer doesn't provide buffer info for webradio streams
            RadioApplication.sBus.post(BufferEvent.BUFFERING);
            try {
                mPlayer.prepareAsync();
            } catch (IllegalStateException e) {
                // FIXME prepareAsync called in state 8
                RadioApplication.sBus.post(BufferEvent.BUFFERING);
                Log.e(TAG, "Could not prepare", e);
                e.printStackTrace();
            }
        }
    }

    public void pause() {
        if (mPlayer.isPlaying()) {
            Log.d(TAG, "pausing");
            mPlayer.pause();
            Log.d(TAG, "State: paused");
        } else {
            Log.d(TAG, "already paused");
            // not playing but prepareAsync may be called -> prevent starting in onPrepared
            cancelStart = true;
        }
        isPaused = true;
    }

    public void stop() {
        Log.d(TAG, "stopping");
        // cancel starting after prepareAsync callback
        cancelStart = true;
        mPlayer.release();
        Log.d(TAG, "State: released");
    }

    public void switchStation(Station station) {
        mStation = station;
        if (mPlayer.isPlaying()) {
            Log.d(TAG, "stopping before switch");
            mPlayer.stop();
        }
        mPlayer.reset();
        isPaused = false;
        isPreparing = false;
        cancelStart = false;

        Log.d(TAG, "switching station");
        try {
            mPlayer.setDataSource(station.url);
        } catch (IOException e) {
            e.printStackTrace();
        }
        Log.d(TAG, "State: initialized");
    }

    public void setVolume(float volume) {
        mPlayer.setVolume(volume, volume);
    }

}
