package de.winterrettich.ninaradio.ui;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.squareup.otto.Subscribe;

import java.util.ArrayList;
import java.util.List;

import de.winterrettich.ninaradio.R;
import de.winterrettich.ninaradio.RadioApplication;
import de.winterrettich.ninaradio.event.BufferEvent;
import de.winterrettich.ninaradio.event.DatabaseEvent;
import de.winterrettich.ninaradio.event.PlaybackEvent;
import de.winterrettich.ninaradio.event.SelectStationEvent;
import de.winterrettich.ninaradio.model.Station;

public class DiscoverFragment extends Fragment implements StationAdapter.StationClickListener {

    private StationAdapter mAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_discover, container, false);

        mAdapter = new StationAdapter(this);
        mAdapter.showFavorites(true);

        RecyclerView favoritesList = (RecyclerView) rootView.findViewById(R.id.result_list);
        favoritesList.setLayoutManager(new LinearLayoutManager(getActivity()));
        favoritesList.setAdapter(mAdapter);
        //favoritesList.setHasFixedSize(true);

        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        RadioApplication.sBus.register(this);

        // Playback state and stations may have changed while paused
        handlePlaybackEvent(RadioApplication.sDatabase.playbackState);
        handleSelectStationEvent(new SelectStationEvent(RadioApplication.sDatabase.selectedStation));
    }

    @Override
    public void onPause() {
        super.onPause();
        RadioApplication.sBus.unregister(this);
    }

    @Override
    public void onClick(Station station) {
        if (!station.equals(RadioApplication.sDatabase.selectedStation)) {
            // play
            RadioApplication.sBus.post(new SelectStationEvent(station));
            RadioApplication.sBus.post(PlaybackEvent.PLAY);
        }
    }

    @Override
    public boolean onLongClick(Station station) {
        return false;
    }

    @Override
    public void onFavoriteChanged(Station station, boolean favorite) {
        if (favorite) {
            RadioApplication.sBus.post(new DatabaseEvent(DatabaseEvent.Operation.CREATE_STATION, station));
        } else {
            RadioApplication.sBus.post(new DatabaseEvent(DatabaseEvent.Operation.DELETE_STATION, station));
        }
    }

    @Subscribe
    public void handlePlaybackEvent(PlaybackEvent event) {
        if (event == PlaybackEvent.STOP) {
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
        mAdapter.setSelection(event.station);
    }
}
