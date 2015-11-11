package de.winterrettich.ninaradio.ui;

import android.media.AudioManager;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;

import com.squareup.otto.Subscribe;

import de.winterrettich.ninaradio.R;
import de.winterrettich.ninaradio.RadioApplication;
import de.winterrettich.ninaradio.event.PlaybackEvent;

public class MainActivity extends AppCompatActivity {
    private PlayBackControlsFragment mControlsFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setLogo(R.mipmap.ic_launcher);
            actionBar.setDisplayUseLogoEnabled(true);
            actionBar.setDisplayShowHomeEnabled(true);
        }

        mControlsFragment = (PlayBackControlsFragment) getFragmentManager()
                .findFragmentById(R.id.fragment_playback_controls);

        // change music stream volume while activity is running
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_add_station:
                showAddStationDialog();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showAddStationDialog() {
        EditStationDialogFragment fragment = EditStationDialogFragment.newInstance();
        fragment.show(getFragmentManager(), "AddStationDialog");
    }

    private void refreshUi() {
        handlePlaybackEvent(RadioApplication.sDatabase.playbackState);
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
