package de.winterrettich.ninaradio.ui;

import android.media.AudioManager;
import android.os.Bundle;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.squareup.otto.Subscribe;

import de.winterrettich.ninaradio.R;
import de.winterrettich.ninaradio.RadioApplication;
import de.winterrettich.ninaradio.event.DatabaseEvent;
import de.winterrettich.ninaradio.event.DiscoverErrorEvent;
import de.winterrettich.ninaradio.event.PlayerErrorEvent;
import de.winterrettich.ninaradio.model.Station;

public class MainActivity extends AppCompatActivity {
    private CoordinatorLayout mCoordinatorLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mCoordinatorLayout = (CoordinatorLayout) findViewById(R.id.root_layout);

        initToolbar();
        initTabs();

        // change music stream volume while activity is running
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
    }

    private void initToolbar() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
    }

    private void initTabs() {
        RadioPagerAdapter adapter = new RadioPagerAdapter(getSupportFragmentManager(), getResources());

        ViewPager viewPager = (ViewPager) findViewById(R.id.viewpager);
        viewPager.setAdapter(adapter);

        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(viewPager);
    }

    @Override
    protected void onResume() {
        super.onResume();
        RadioApplication.sBus.register(this);
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
        fragment.show(getSupportFragmentManager(), "AddStationDialog");
    }

    @Subscribe
    public void handleDatabaseEvent(DatabaseEvent event) {
        if (event.operation == DatabaseEvent.Operation.DELETE_STATION) {
            showUndoSnackbar(event.station);
        }
    }

    private void showUndoSnackbar(final Station station) {
        // create undo snackbar
        Snackbar snackbar = Snackbar
                .make(mCoordinatorLayout, R.string.station_deleted, Snackbar.LENGTH_LONG)
                .setAction(R.string.undo, new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        // undo by creating the station again
                        if (!RadioApplication.sDatabase.getStations().contains(station)) {
                            DatabaseEvent undoEvent = new DatabaseEvent(DatabaseEvent.Operation.CREATE_STATION, station);
                            RadioApplication.sBus.post(undoEvent);
                        }
                    }
                });

        // change snackbar text color
        View view = snackbar.getView();
        TextView tv = (TextView) view.findViewById(android.support.design.R.id.snackbar_action);
        int color = ContextCompat.getColor(this, R.color.window_background);
        tv.setTextColor(color);

        snackbar.show();
    }

    @Subscribe
    public void handlePlayerErrorEvent(PlayerErrorEvent event) {
        // show error
        Snackbar.make(mCoordinatorLayout, event.message, Snackbar.LENGTH_SHORT).show();
    }

    @Subscribe
    public void handleDiscoverErrorEvent(DiscoverErrorEvent event) {
        // show error
        Snackbar.make(mCoordinatorLayout, event.message, Snackbar.LENGTH_SHORT).show();
    }

}
