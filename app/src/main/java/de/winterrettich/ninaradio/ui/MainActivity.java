package de.winterrettich.ninaradio.ui;

import android.app.Activity;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.Log;

import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

import java.io.IOException;

import javax.inject.Inject;

import dagger.Module;
import dagger.ObjectGraph;
import dagger.Provides;
import de.winterrettich.ninaradio.MediaPlayerHelper;
import de.winterrettich.ninaradio.R;
import de.winterrettich.ninaradio.RadioApplication;
import de.winterrettich.ninaradio.event.PlaybackEvent;

public class MainActivity extends Activity {
    private PlayBackControlsFragment mControlsFragment;
    private StationListFragment mListFragment;

    MediaPlayerHelper mPlayerHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mControlsFragment = (PlayBackControlsFragment) getFragmentManager()
                .findFragmentById(R.id.fragment_playback_controls);

        mListFragment = (StationListFragment) getFragmentManager()
                .findFragmentById(R.id.fragment_radio_list);

        mPlayerHelper = new MediaPlayerHelper();

        RadioApplication.sBus.register(this);
    }

    private void showPlaybackControls() {
        getFragmentManager().beginTransaction()
                .setCustomAnimations(
                        R.animator.slide_in_from_bottom, R.animator.slide_out_to_bottom,
                        R.animator.slide_in_from_bottom, R.animator.slide_out_to_bottom)
                .show(mControlsFragment)
                .commit();
    }

    private void hidePlaybackControls() {
        getFragmentManager().beginTransaction()
                .hide(mControlsFragment)
                .commit();

    }

    @Subscribe
    public void handlePlaybackEvent(PlaybackEvent event) {
        switch (event.type) {
            case PLAY:
                if (mControlsFragment.isHidden()) {
                    showPlaybackControls();
                }
                break;
            case PAUSE:
                break;
            case STOP:
                if (!mControlsFragment.isHidden()) {
                    hidePlaybackControls();
                }
                break;
        }
    }

}
