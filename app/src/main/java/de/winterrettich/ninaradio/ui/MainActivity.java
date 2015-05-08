package de.winterrettich.ninaradio.ui;

import android.app.Activity;
import android.os.Bundle;

import com.squareup.otto.Subscribe;

import de.winterrettich.ninaradio.RadioPlayer;
import de.winterrettich.ninaradio.R;
import de.winterrettich.ninaradio.RadioApplication;
import de.winterrettich.ninaradio.event.PlaybackEvent;

public class MainActivity extends Activity {
    private PlayBackControlsFragment mControlsFragment;

    RadioPlayer mPlayerHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mControlsFragment = (PlayBackControlsFragment) getFragmentManager()
                .findFragmentById(R.id.fragment_playback_controls);

        hidePlaybackControls();

        mPlayerHelper = new RadioPlayer();

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
