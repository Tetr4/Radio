package de.winterrettich.ninaradio.ui;

import android.media.AudioManager;
import android.os.Bundle;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.squareup.otto.Subscribe;

import de.winterrettich.ninaradio.R;
import de.winterrettich.ninaradio.RadioApplication;
import de.winterrettich.ninaradio.event.DatabaseEvent;
import de.winterrettich.ninaradio.event.PlaybackEvent;
import de.winterrettich.ninaradio.event.PlayerErrorEvent;
import de.winterrettich.ninaradio.model.Station;

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

    @Subscribe
    public void handleDatabaseEvent(DatabaseEvent event) {
        final Station undoStation = new Station(event.station.name, event.station.url);
        if (event.operation == DatabaseEvent.Operation.DELETE_STATION) {
            // create undo snackbar
            final CoordinatorLayout layout = (CoordinatorLayout) findViewById(R.id.root_layout);
            Snackbar snackbar = Snackbar
                    .make(layout, R.string.station_deleted, Snackbar.LENGTH_LONG)
                    .setAction(R.string.undo, new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            // undo by creating the station again
                            DatabaseEvent undoEvent = new DatabaseEvent(DatabaseEvent.Operation.CREATE_STATION, undoStation);
                            RadioApplication.sBus.post(undoEvent);
                        }
                    });

            // change color
            View view = snackbar.getView();
            TextView tv = (TextView) view.findViewById(android.support.design.R.id.snackbar_action);
            int color = ContextCompat.getColor(this, R.color.window_background);
            tv.setTextColor(color);

            snackbar.show();
        }

    }

    @Subscribe
    public void handlePlayerErrorEvent(PlayerErrorEvent event) {
        final CoordinatorLayout layout = (CoordinatorLayout) findViewById(R.id.root_layout);
        Snackbar.make(layout, event.message, Snackbar.LENGTH_SHORT).show();

    }

}
