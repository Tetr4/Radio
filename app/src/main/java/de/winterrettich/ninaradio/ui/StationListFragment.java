package de.winterrettich.ninaradio.ui;

import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.bignerdranch.android.multiselector.SingleSelector;
import com.bignerdranch.android.multiselector.SwappingHolder;
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

public class StationListFragment extends Fragment implements ActionMode.Callback {
    private List<Station> mDatabaseStations;
    private StationAdapter mAdapter;
    private SingleSelector mSelector = new SingleSelector();

    private ActionMode mActionMode;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_station_list, container, false);

        // fill mDatabaseStations
        synchronizeStationsWithDatabase();

        mSelector.setSelectable(true);
        mAdapter = new StationAdapter();
        RecyclerView recyclerView = (RecyclerView) rootView.findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        recyclerView.setAdapter(mAdapter);
        //recyclerView.setHasFixedSize(true);

        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        RadioApplication.sBus.register(this);

        // Playback state and station may have changed while paused
        refreshUi();
    }

    @Override
    public void onPause() {
        super.onPause();
        RadioApplication.sBus.unregister(this);
    }

    private void refreshUi() {
        synchronizeStationsWithDatabase();
        mAdapter.notifyDataSetChanged();
        handlePlaybackEvent(RadioApplication.sDatabase.playbackState);
        handleSelectStationEvent(new SelectStationEvent(RadioApplication.sDatabase.selectedStation));
    }

    private void synchronizeStationsWithDatabase() {
        // shallow copy
        mDatabaseStations = new ArrayList<>(RadioApplication.sDatabase.getStations());
        Collections.sort(mDatabaseStations);
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
        mSelector.clearSelections();
        mActionMode = null;
    }

    @Override
    public boolean onActionItemClicked(ActionMode actionMode, MenuItem menuItem) {
        int positionToAlter = mSelector.getSelectedPositions().get(0);
        Station stationToAlter = mDatabaseStations.get(positionToAlter);

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
                synchronizeStationsWithDatabase();
                int positionToCreate = mDatabaseStations.indexOf(event.station);
                if (positionToCreate > -1) {
                    mAdapter.notifyItemInserted(positionToCreate);
                }
                // TODO smoothscroll to position
                break;

            case DELETE_STATION:
                int positionToDelete = mDatabaseStations.indexOf(event.station);
                if (positionToDelete > -1) {
                    mAdapter.notifyItemRemoved(positionToDelete);
                }
                synchronizeStationsWithDatabase();
                break;

            case UPDATE_STATION:
                int positionToUpdate = mDatabaseStations.indexOf(event.station);
                if (positionToUpdate > -1) {
                    mAdapter.notifyItemChanged(positionToUpdate);
                }
                synchronizeStationsWithDatabase();
                break;
        }

    }

    @Subscribe
    public void handlePlaybackEvent(PlaybackEvent event) {
        if (isInActionMode()) {
            mAdapter.notifyDataSetChanged();
        } else if (!mSelector.getSelectedPositions().isEmpty()) {
            int selectedPosition = mSelector.getSelectedPositions().get(0);
            if (event == PlaybackEvent.STOP) {
                mSelector.clearSelections();
            }
            mAdapter.notifyItemChanged(selectedPosition);
        }
    }

    @Subscribe
    public void handleBufferEvent(BufferEvent event) {
        if (!isInActionMode() && !mSelector.getSelectedPositions().isEmpty()) {
            int selectedPosition = mSelector.getSelectedPositions().get(0);
            mAdapter.notifyItemChanged(selectedPosition);
        }
    }

    @Subscribe
    public void handleSelectStationEvent(SelectStationEvent event) {
        int position = mDatabaseStations.indexOf(event.station);
        if (position >= 0) {
            mSelector.setSelected(position, 0, true);
            // FIXME
//            mListView.smoothScrollToPosition(position);
        } else if (!isInActionMode()) {
            mSelector.clearSelections();
        }
        mAdapter.notifyDataSetChanged();
    }


    private class StationHolder extends SwappingHolder implements View.OnClickListener, View.OnLongClickListener {
        private final ImageView mIcon;
        private final ProgressBar mIconBuffering;
        private final ImageView mIconPlaying;
        private final AnimationDrawable mIconPlayingAnimation;
        private final TextView mNameTextView;
        private final TextView mUrlTextView;

        private Station mStation;

        public StationHolder(View itemView) {
            super(itemView, mSelector);
            Drawable selector = ContextCompat.getDrawable(getActivity(), R.drawable.station_list_selector);
            setSelectionModeBackgroundDrawable(selector);
            setDefaultModeBackgroundDrawable(selector);
//            setSelectionModeStateListAnimator();
//            setDefaultModeStateListAnimator();
            itemView.setOnClickListener(this);
            itemView.setOnLongClickListener(this);

            mIcon = (ImageView) itemView.findViewById(R.id.icon);
            mIconBuffering = (ProgressBar) itemView.findViewById(R.id.icon_buffering);
            mIconPlaying = (ImageView) itemView.findViewById(R.id.icon_playing);
            mIconPlayingAnimation = (AnimationDrawable) mIconPlaying.getDrawable();
            mNameTextView = (TextView) itemView.findViewById(R.id.name);
            mUrlTextView = (TextView) itemView.findViewById(R.id.description);
        }

        public void bindStation(Station station) {
            mStation = station;

            mNameTextView.setText(mStation.name);
            mUrlTextView.setText(mStation.url);

            // show either playback animation, buffering icon or stop icon
            boolean isSelected = mSelector.isSelected(getAdapterPosition(), 0) && !isInActionMode();
            boolean isBuffering = RadioApplication.sDatabase.bufferingState == BufferEvent.BUFFERING;
            boolean isPlaying = RadioApplication.sDatabase.playbackState == PlaybackEvent.PLAY;

            if (isSelected) {
                if (!isPlaying) {
                    // paused icon
                    mIcon.setVisibility(View.INVISIBLE);
                    mIconBuffering.setVisibility(View.INVISIBLE);
                    mIconPlaying.setVisibility(View.VISIBLE);
                    mIconPlayingAnimation.stop();
                } else if (isBuffering) {
                    // buffer icon
                    mIcon.setVisibility(View.INVISIBLE);
                    mIconBuffering.setVisibility(View.VISIBLE);
                    mIconPlaying.setVisibility(View.INVISIBLE);
                } else {
                    // animated icon
                    mIcon.setVisibility(View.INVISIBLE);
                    mIconBuffering.setVisibility(View.INVISIBLE);
                    mIconPlaying.setVisibility(View.VISIBLE);
                    mIconPlayingAnimation.start();
                }
            } else {
                // default/stopped icon
                mIcon.setVisibility(View.VISIBLE);
                mIconBuffering.setVisibility(View.INVISIBLE);
                mIconPlaying.setVisibility(View.INVISIBLE);
            }
        }

        @Override
        public void onClick(View v) {
            // close action mode on normal click
            if (isInActionMode()) {
                mActionMode.finish();
            }

            if (!mSelector.isSelected(getAdapterPosition(), 0)) {
                mSelector.setSelected(this, true);
                RadioApplication.sBus.post(new SelectStationEvent(mStation));
                RadioApplication.sBus.post(PlaybackEvent.PLAY);
            }

        }

        @Override
        public boolean onLongClick(View v) {
            mSelector.setSelected(this, true);
            if (!isInActionMode()) {
                // start action mode to delete/edit item
                AppCompatActivity activity = (AppCompatActivity) getActivity();
                mActionMode = activity.startSupportActionMode(StationListFragment.this);
            }
            return true;
        }
    }


    private class StationAdapter extends RecyclerView.Adapter<StationHolder> {
        @Override
        public StationHolder onCreateViewHolder(ViewGroup parent, int pos) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.list_item_station, parent, false);
            return new StationHolder(view);
        }

        @Override
        public void onBindViewHolder(StationHolder holder, int pos) {
            Station station = mDatabaseStations.get(pos);
            holder.bindStation(station);
        }

        @Override
        public int getItemCount() {
            return mDatabaseStations.size();
        }
    }


}
