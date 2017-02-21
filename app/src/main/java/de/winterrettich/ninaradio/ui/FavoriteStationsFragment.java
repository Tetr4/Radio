package de.winterrettich.ninaradio.ui;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.squareup.otto.Subscribe;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import de.winterrettich.ninaradio.R;
import de.winterrettich.ninaradio.RadioApplication;
import de.winterrettich.ninaradio.event.BufferEvent;
import de.winterrettich.ninaradio.event.DatabaseEvent;
import de.winterrettich.ninaradio.event.PlaybackEvent;
import de.winterrettich.ninaradio.event.SelectStationEvent;
import de.winterrettich.ninaradio.model.Station;

/**
 * Enhances its stations list with an action menu, which opens on long press and can be used to edit/delete favorited stations.
 */
public class FavoriteStationsFragment extends Fragment implements ActionMode.Callback, StationAdapter.StationClickListener {
    private StationAdapter mAdapter;
    private ActionMode mActionMode;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_station_list, container, false);

        mAdapter = new StationAdapter(this);
        mAdapter.showFavorites(false);

        RecyclerView favoritesList = (RecyclerView) rootView.findViewById(R.id.favorites_list);
        favoritesList.setLayoutManager(new LinearLayoutManager(getActivity()));
        favoritesList.setAdapter(mAdapter);
        favoritesList.setHasFixedSize(true);

        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        RadioApplication.sBus.register(this);

        // Playback state and stations may have changed while paused
        refreshList();
    }

    @Override
    public void onPause() {
        super.onPause();
        RadioApplication.sBus.unregister(this);
    }

    private void refreshList() {
        // shallow copy and sort
        List<Station> stations = new ArrayList<>(RadioApplication.sDatabase.getStations());
        Collections.sort(stations);
        mAdapter.setStations(stations);
        mAdapter.setSelection(RadioApplication.sDatabase.selectedStation);
    }

    private boolean isInActionMode() {
        return mActionMode != null;
    }

    @Override
    public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
        // stop playback and show menu
        mActionMode = actionMode;
        RadioApplication.sBus.post(PlaybackEvent.STOP);
        getActivity().getMenuInflater().inflate(R.menu.menu_selection, menu);
        return true;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        return false;
    }

    @Override
    public void onDestroyActionMode(ActionMode actionMode) {
        mAdapter.clearSelection();
        mActionMode = null;
    }

    @Override
    public boolean onActionItemClicked(ActionMode actionMode, MenuItem menuItem) {
        Station stationToAlter = mAdapter.getSelection();

        switch (menuItem.getItemId()) {
            case R.id.action_delete:
                DatabaseEvent deleteEvent =
                        new DatabaseEvent(DatabaseEvent.Operation.DELETE_STATION, stationToAlter);
                RadioApplication.sBus.post(deleteEvent);
                actionMode.finish();
                return true;

            case R.id.action_edit:
                EditStationDialogFragment fragment = EditStationDialogFragment.newInstance(stationToAlter);
                fragment.show(getFragmentManager(), "EditStationDialog");
                break;
        }
        return false;
    }

    @Subscribe
    public void handleDatabaseEvent(DatabaseEvent event) {
        switch (event.operation) {
            case CREATE_STATION:
                List<Station> stations = new ArrayList<>(RadioApplication.sDatabase.getStations());
                Collections.sort(stations);
                int positionToCreate = stations.indexOf(event.station);
                mAdapter.insertStation(event.station, positionToCreate);
                if (mAdapter.getSelection() == null) {
                    // the new station may already be playing
                    mAdapter.setSelection(RadioApplication.sDatabase.selectedStation);
                }
                break;

            case DELETE_STATION:
                if (mAdapter.getSelection().equals(event.station)) {
                    mAdapter.clearSelection();
                }
                mAdapter.deleteStation(event.station);
                break;

            case UPDATE_STATION:
                mAdapter.updateStation(event.station);
                break;
        }

    }

    @Subscribe
    public void handlePlaybackEvent(PlaybackEvent event) {
        if (!isInActionMode() && event == PlaybackEvent.STOP) {
            mAdapter.clearSelection();
        }
        mAdapter.updateStation(RadioApplication.sDatabase.selectedStation);
    }

    @Subscribe
    public void handleBufferEvent(BufferEvent event) {
        mAdapter.updateStation(RadioApplication.sDatabase.selectedStation);
    }

    @Subscribe
    public void handleSelectStationEvent(SelectStationEvent event) {
        if (!isInActionMode()) {
            mAdapter.setSelection(event.station);
        }
    }

    @Override
    public void onClick(Station station) {
        if (isInActionMode()) {
            // finish action mode and start playing
            mActionMode.finish();
        }

        if (!station.equals(RadioApplication.sDatabase.selectedStation)) {
            // play
            RadioApplication.sBus.post(new SelectStationEvent(station));
            RadioApplication.sBus.post(PlaybackEvent.PLAY);
        }
    }

    @Override
    public boolean onLongClick(Station station) {
        if (!isInActionMode()) {
            // start action mode to delete/edit item
            AppCompatActivity activity = (AppCompatActivity) getActivity();
            mActionMode = activity.startSupportActionMode(this);
        }
        mAdapter.setSelection(station);
        return true;
    }

    @Override
    public void onFavoriteChanged(Station station, boolean favorite) {
        if (favorite) {
            RadioApplication.sBus.post(new DatabaseEvent(DatabaseEvent.Operation.CREATE_STATION, station));
        } else {
            RadioApplication.sBus.post(new DatabaseEvent(DatabaseEvent.Operation.DELETE_STATION, station));
        }
    }

}
