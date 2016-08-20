package de.winterrettich.ninaradio.ui;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SearchView;

import com.squareup.otto.Subscribe;

import java.util.Collections;
import java.util.List;

import de.winterrettich.ninaradio.R;
import de.winterrettich.ninaradio.RadioApplication;
import de.winterrettich.ninaradio.event.BufferEvent;
import de.winterrettich.ninaradio.event.DatabaseEvent;
import de.winterrettich.ninaradio.event.DiscoverErrorEvent;
import de.winterrettich.ninaradio.event.PlaybackEvent;
import de.winterrettich.ninaradio.event.SelectStationEvent;
import de.winterrettich.ninaradio.model.Station;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DiscoverFragment extends Fragment implements StationAdapter.StationClickListener, SearchView.OnQueryTextListener {
    private static final String TAG = DiscoverFragment.class.getSimpleName();
    private StationAdapter mAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_discover, container, false);

        mAdapter = new StationAdapter(this);
        mAdapter.showFavorites(true);

        RecyclerView favoritesList = (RecyclerView) rootView.findViewById(R.id.result_list);
        favoritesList.setLayoutManager(new LinearLayoutManager(getActivity()));
        favoritesList.setAdapter(mAdapter);
        favoritesList.setHasFixedSize(true);

        SearchView searchView = (SearchView) rootView.findViewById(R.id.search_view);
        searchView.setOnQueryTextListener(this);

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

    @Subscribe
    public void handleDatabaseEvent(DatabaseEvent event) {
        mAdapter.updateStation(event.station);
        mAdapter.setSelection(RadioApplication.sDatabase.selectedStation);
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        Call<List<Station>> call = RadioApplication.sDiscovererService.search(query);
        call.enqueue(new Callback<List<Station>>() {
            @Override
            public void onResponse(Call<List<Station>> call, Response<List<Station>> response) {
                List<Station> stations = response.body();
                mAdapter.setStations(stations);
                if (stations.isEmpty()) {
                    String message = getString(R.string.no_stations_discovered);
                    RadioApplication.sBus.post(new DiscoverErrorEvent(message));
                }
            }

            @Override
            public void onFailure(Call<List<Station>> call, Throwable t) {
                String message = getString(R.string.error_discovering_stations);
                RadioApplication.sBus.post(new DiscoverErrorEvent(message));
                Log.e(TAG, message, t);
            }
        });
        return true;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        if (newText.length() == 0) {
            mAdapter.setStations(Collections.<Station>emptyList());
            return true;
        }
        return false;
    }

}
