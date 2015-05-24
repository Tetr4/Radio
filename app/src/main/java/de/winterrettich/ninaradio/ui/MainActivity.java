package de.winterrettich.ninaradio.ui;

import android.app.Activity;
import android.os.Bundle;

import com.squareup.otto.Subscribe;

import de.winterrettich.ninaradio.R;
import de.winterrettich.ninaradio.RadioApplication;
import de.winterrettich.ninaradio.event.PlaybackEvent;

public class MainActivity extends Activity {
    private PlayBackControlsFragment mControlsFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mControlsFragment = (PlayBackControlsFragment) getFragmentManager()
                .findFragmentById(R.id.fragment_playback_controls);

    }

    @Override
    protected void onResume() {
        super.onResume();
        RadioApplication.sBus.register(this);

        // Playback state may have changed while paused
        refreshUi();
    }

    @Override
    protected void onPause() {
        super.onPause();
        RadioApplication.sBus.unregister(this);
    }

    private void refreshUi() {
        PlaybackEvent currentPlaybackState = RadioApplication.sPlaybackState;
        if (currentPlaybackState != null) {
            handlePlaybackEvent(RadioApplication.sPlaybackState);
        } else {
            hidePlaybackControls();
        }
    }

    private void showPlaybackControls() {
        getFragmentManager().beginTransaction()
                .setCustomAnimations(
                        R.animator.slide_in_from_bottom, R.animator.slide_out_to_bottom)
                .show(mControlsFragment)
                .commit();
    }

    private void hidePlaybackControls() {
        getFragmentManager().beginTransaction()
                .setCustomAnimations(
                        R.animator.slide_in_from_bottom, R.animator.slide_out_to_bottom)
                .hide(mControlsFragment)
                .commit();
    }

    @Subscribe
    public void handlePlaybackEvent(PlaybackEvent event) {
        if (event == PlaybackEvent.STOP) {
            if (!mControlsFragment.isHidden()) {
                hidePlaybackControls();
            }
        } else {
            if (mControlsFragment.isHidden()) {
                showPlaybackControls();
            }
        }
    }
}
