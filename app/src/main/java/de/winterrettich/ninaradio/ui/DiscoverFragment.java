package de.winterrettich.ninaradio.ui;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.SearchView;

import com.squareup.otto.Subscribe;

import java.util.ArrayList;

import de.winterrettich.ninaradio.R;
import de.winterrettich.ninaradio.RadioApplication;
import de.winterrettich.ninaradio.event.BufferEvent;
import de.winterrettich.ninaradio.event.DatabaseEvent;
import de.winterrettich.ninaradio.event.DiscoverErrorEvent;
import de.winterrettich.ninaradio.event.PlaybackEvent;
import de.winterrettich.ninaradio.event.SelectStationEvent;
import de.winterrettich.ninaradio.model.Station;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class DiscoverFragment extends Fragment implements StationAdapter.StationClickListener, SearchView.OnQueryTextListener {
    private static final String TAG = DiscoverFragment.class.getSimpleName();
    private StationAdapter mAdapter;
    private SearchView mSearchView;
    private View mProgressIndicator;
    private Disposable mSearchSubscription;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_discover, container, false);

        mAdapter = new StationAdapter(this);
        mAdapter.showFavorites(true);

        mProgressIndicator = rootView.findViewById(R.id.progress_indicator);

        RecyclerView searchResultList = (RecyclerView) rootView.findViewById(R.id.result_list);
        searchResultList.setLayoutManager(new LinearLayoutManager(getActivity()));
        searchResultList.setAdapter(mAdapter);
        searchResultList.setHasFixedSize(true);

        mSearchView = (SearchView) rootView.findViewById(R.id.search_view);
        mSearchView.setOnQueryTextListener(this);

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

        // cancel running searches
        if (mSearchSubscription != null) {
            mSearchSubscription.dispose();
        }
        mProgressIndicator.setVisibility(View.GONE);
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
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);

        // focus/unfocus the Searchview when scrolling to/from the fragment
        if (mSearchView == null) {
            return;
        }
        if (isVisibleToUser) {
            mSearchView.requestFocus();
            InputMethodManager inputMethodManager = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            inputMethodManager.showSoftInput(mSearchView.findFocus(), InputMethodManager.SHOW_IMPLICIT);
        } else {
            mSearchView.clearFocus();
        }
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        if (newText.length() == 0) {
            resetSearch();
            return true;
        }
        return false;
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        mSearchView.clearFocus();
        resetSearch();
        searchStations(query);
        return true;
    }

    private void searchStations(String query) {
        // show progressbar
        mProgressIndicator.setVisibility(View.VISIBLE);

        // search for stations
        mSearchSubscription = RadioApplication.sDiscovererService.search(query)
                .subscribeOn(Schedulers.io())
                .flatMapIterable(list -> list) // retrieve each station from list
                .flatMap(this::resolveStreamUrl)
                .subscribeOn(AndroidSchedulers.mainThread())
                .map(station -> {
                    // try using existing station (e.g. with existing database id)
                    Station existingStation = RadioApplication.sDatabase.findMatchingStation(station);
                    return existingStation != null ? existingStation : station;
                })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(station -> {
                            // add this station to search result list
                            mProgressIndicator.setVisibility(View.GONE);
                            mAdapter.insertStation(station, mAdapter.getItemCount());
                            mAdapter.setSelection(RadioApplication.sDatabase.selectedStation);
                        },
                        error -> {
                            // show error
                            mProgressIndicator.setVisibility(View.GONE);
                            String message = getString(R.string.error_discovering_stations);
                            message += " (" + error.getMessage() + ")";
                            Log.e(TAG, message, error);
                            RadioApplication.sBus.post(new DiscoverErrorEvent(message));
                        },
                        () -> {
                            // show error when no stations were found after completion
                            mProgressIndicator.setVisibility(View.GONE);
                            if (mAdapter.getItemCount() == 0) {
                                String message = getString(R.string.no_stations_discovered);
                                RadioApplication.sBus.post(new DiscoverErrorEvent(message));
                            }
                        });
    }

    private Observable<Station> resolveStreamUrl(Station stationWithIntermediateUrl) {
        return Observable.just(stationWithIntermediateUrl) // resolve stream urls in parallel
                .subscribeOn(Schedulers.io())
                .flatMap(station -> RadioApplication.sStreamUrlResolver
                                .resolve(station.url)
                                .onExceptionResumeNext(Observable.empty()), // skip invalid urls
                        (station, newUrl) -> new Station(station.name, newUrl));
    }

    private void resetSearch() {
        if (mSearchSubscription != null) {
            mSearchSubscription.dispose();
        }
        mProgressIndicator.setVisibility(View.GONE);
        mAdapter.clearSelection();
        mAdapter.setStations(new ArrayList<>());
    }

}
