package de.winterrettich.ninaradio;

import android.media.AudioManager;
import android.media.MediaPlayer;
import android.util.Log;

import com.squareup.otto.Subscribe;

import java.io.IOException;

import de.winterrettich.ninaradio.event.PlaybackEvent;
import de.winterrettich.ninaradio.event.SwitchStationEvent;

public class MediaPlayerHelper {
    private MediaPlayer mPlayer;

    public MediaPlayerHelper() {
        RadioApplication.sBus.register(this);
    }

    private void initPlayer() {
        mPlayer = new MediaPlayer();
        mPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mPlayer.setOnBufferingUpdateListener(new MediaPlayer.OnBufferingUpdateListener() {

            public void onBufferingUpdate(MediaPlayer mp, int percent) {
                // playSeekBar.setSecondaryProgress(percent);
                Log.i("Buffering", "" + percent);
            }
        });
    }

    private void play() {
        if(mPlayer == null) {
            initPlayer();
        }
        mPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            public void onPrepared(MediaPlayer mp) {
                mp.start();
            }
        });
        mPlayer.prepareAsync();
    }

    private void pause() {
        if (mPlayer != null && mPlayer.isPlaying()) {
            mPlayer.stop();
        }
    }

    private void stop() {
        if (mPlayer != null && mPlayer.isPlaying()) {
            mPlayer.stop();
            mPlayer.release();
        }
        mPlayer = null;
    }

    private void switchChannel(String url) {
        if(mPlayer == null) {
            initPlayer();
        }
        mPlayer.reset();
        try {
            mPlayer.setDataSource(url);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IllegalStateException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
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
    public void handleSwitchChannelEvent(SwitchStationEvent event) {
        Log.d("SwitchStationEvent", event.station.name);
        switchChannel(event.station.url);
    }


}
