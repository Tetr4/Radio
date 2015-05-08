package de.winterrettich.ninaradio;

import android.media.AudioManager;
import android.media.MediaPlayer;
import android.util.Log;

import com.squareup.otto.Subscribe;

import java.io.IOException;

import de.winterrettich.ninaradio.event.PlaybackEvent;
import de.winterrettich.ninaradio.event.SelectStationEvent;
import de.winterrettich.ninaradio.model.Station;

/**
 * encapsulates a {@link MediaPlayer}
 */
public class RadioPlayer implements MediaPlayer.OnBufferingUpdateListener, MediaPlayer.OnPreparedListener {
    public static final String TAG = RadioPlayer.class.getSimpleName();
    private MediaPlayer mPlayer;
    private Station mStation;
    private boolean isPreparing = false;
    private boolean isPaused = false;
    private boolean cancelStart = false;


    public RadioPlayer() {
        RadioApplication.sBus.register(this);
        initPlayer();
    }

    private void initPlayer() {
        mPlayer = new MediaPlayer();
        mPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mPlayer.setOnBufferingUpdateListener(this);
        mPlayer.setOnPreparedListener(this);
    }

    private void switchStation(Station station) {
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
    }

    @Override
    public void onBufferingUpdate(MediaPlayer mp, int percent) {
        // playSeekBar.setSecondaryProgress(percent);
        Log.i(TAG, "Buffering: " + percent);
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        if (!cancelStart) {
            Log.d(TAG, "starting after prepare");
            mp.start();
        }
        isPreparing = false;
    }

    private void play() {
        cancelStart = false;

        if (mStation == null) {
            throw new IllegalStateException("Select a Station before playing");
        }
        if (isPreparing) {
            // already called prepareAsync
            Log.d(TAG, "already called prepareAsync");
            return;
        }
        if (isPaused) {
            // resume without prepare
            Log.d(TAG, "resume");
            mPlayer.start();
        } else {
            isPreparing = true;
            mPlayer.prepareAsync();
        }

    }

    private void pause() {
        if (mPlayer != null) {
            if (mPlayer.isPlaying()) {
                mPlayer.pause();
                isPaused = true;
            } else {
                // cancel starting after prepareAsync callback
                cancelStart = true;
            }
        }
    }

    private void stop() {
        if (mPlayer != null) {
            // cancel starting after prepareAsync callback
            cancelStart = true;
            mPlayer.release();
        }
        mPlayer = null;
    }

    @Subscribe
    public void handlePlaybackEvent(PlaybackEvent event) {
        Log.d("PlaybackEvent", event.type.toString());
        switch (event.type) {
            case PLAY:
                play();
                break;
            case PAUSE:
                pause();
                break;
            case STOP:
                stop();
                break;
        }
    }

    @Subscribe
    public void handleSelectStationEvent(SelectStationEvent event) {
        Log.d("SelectStationEvent", event.station.name);
        switchStation(event.station);
    }

}
