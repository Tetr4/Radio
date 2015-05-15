package de.winterrettich.ninaradio;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.PowerManager;
import android.util.Log;

import java.io.IOException;

import de.winterrettich.ninaradio.model.Station;

/**
 * encapsulates a {@link MediaPlayer}
 */
public class RadioPlayerManager implements MediaPlayer.OnBufferingUpdateListener, MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener {
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
        mPlayer.setOnBufferingUpdateListener(this);
        mPlayer.setOnPreparedListener(this);
        mPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mPlayer.setOnBufferingUpdateListener(this);
        mPlayer.setOnPreparedListener(this);
        mPlayer.setOnErrorListener(this);
        Log.d(TAG, "State: idle");
    }

    @Override
    public void onBufferingUpdate(MediaPlayer mp, int percent) {
        // playSeekBar.setSecondaryProgress(percent);
        Log.i(TAG, "Buffering: " + percent + "%");
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        Log.d(TAG, "State: prepared");
        isPreparing = false;
        if (!cancelStart && !mp.isPlaying()) {
            mp.start();
            Log.d(TAG, "State: started");
        } else {
            Log.d(TAG, "Start canceled");
        }
    }

    public void play() {
        cancelStart = false;

        if (mStation == null) {
            throw new IllegalStateException("Select a Station before playing");
        }
        if (isPreparing) {
            Log.d(TAG, "already called prepareAsync");
            return;
        }
        if (isPaused) {
            // already prepared -> just resume without prepare
            Log.d(TAG, "resume without prepare");
            mPlayer.start();
            Log.d(TAG, "State: started");
        } else {
            isPreparing = true;
            mPlayer.prepareAsync();
            Log.d(TAG, "preparing async");
        }
    }

    public void pause() {
        if (mPlayer != null) {
            if (mPlayer.isPlaying()) {
                mPlayer.pause();
                isPaused = true;
                Log.d(TAG, "State: paused");
            } else {
                Log.d(TAG, "Player already paused");
                // not playing but prepareAsync may be called -> prevent starting in onPrepared
                cancelStart = true;
            }
        }
    }

    public void stop() {
        if (mPlayer != null) {
            // cancel starting after prepareAsync callback
            cancelStart = true;
            mPlayer.release();
            Log.d(TAG, "Player released");
        }
        //initPlayer();
    }

    public void switchStation(Station station) {
        mStation = station;
        if(mPlayer.isPlaying()) {
            mPlayer.stop();
        }
        mPlayer.reset();
        isPaused = false;
        isPreparing = false;
        cancelStart = false;

        try {
            mPlayer.setDataSource(station.url);
        } catch (IOException e) {
            e.printStackTrace();
        }
        Log.d(TAG, "State: initialized");
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        return false;
    }
}
