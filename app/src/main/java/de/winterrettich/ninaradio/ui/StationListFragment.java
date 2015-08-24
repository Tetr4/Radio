package de.winterrettich.ninaradio.ui;

import android.app.Fragment;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
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

import com.bignerdranch.android.multiselector.ModalMultiSelectorCallback;
import com.bignerdranch.android.multiselector.SingleSelector;
import com.bignerdranch.android.multiselector.SwappingHolder;
import com.squareup.otto.Subscribe;

import java.util.List;

import de.winterrettich.ninaradio.R;
import de.winterrettich.ninaradio.RadioApplication;
import de.winterrettich.ninaradio.event.BufferEvent;
import de.winterrettich.ninaradio.event.DatabaseEvent;
import de.winterrettich.ninaradio.event.PlaybackEvent;
import de.winterrettich.ninaradio.event.SelectStationEvent;
import de.winterrettich.ninaradio.model.Station;

public class StationListFragment extends Fragment {
    private StationAdapter mAdapter;
    private List<Station> mDatabaseStations;
    private RecyclerView mRecyclerView;
    private SingleSelector mSelector = new SingleSelector();
    private ActionMode.Callback mDeleteMode = new ModalMultiSelectorCallback(mSelector) {

        @Override
        public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
            RadioApplication.sBus.post(PlaybackEvent.STOP);
            getActivity().getMenuInflater().inflate(R.menu.menu_selection, menu);
            return true;
        }

        @Override
        public void onDestroyActionMode(ActionMode actionMode) {
            mSelector.clearSelections();
        }

        @Override
        public boolean onActionItemClicked(ActionMode actionMode, MenuItem menuItem) {
            switch (menuItem.getItemId()) {
                case R.id.action_delete:
                    int positionToDelete = mSelector.getSelectedPositions().get(0);
                    Station stationToDelete = mDatabaseStations.get(positionToDelete);
                    DatabaseEvent deleteEvent =
                            new DatabaseEvent(DatabaseEvent.Operation.DELETE_STATION, stationToDelete);
                    RadioApplication.sBus.post(deleteEvent);
                    actionMode.finish();
                    return true;

                case R.id.action_edit:
                    // TODO
                    break;
            }
            return false;
        }
    };
    private ActionMode mActionMode;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_station_list, container, false);

        // fill mDatabaseStations
        synchronizeStationsWithDatabase();

        mSelector.setSelectable(true);
        mAdapter = new StationAdapter();
        mRecyclerView = (RecyclerView) rootView.findViewById(R.id.recycler_view);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        mRecyclerView.setAdapter(new StationAdapter());
        //mRecyclerView.setHasFixedSize(true);

        return rootView;
    }

    private void synchronizeStationsWithDatabase() {
        mDatabaseStations = RadioApplication.sDatabase.getStations();
    }

    @Subscribe
    public void handleDatabaseEvent(DatabaseEvent event) {
        switch (event.operation) {
            case CREATE_STATION:
                synchronizeStationsWithDatabase();
                int lastPosition = mDatabaseStations.size();
                mRecyclerView.getAdapter().notifyItemInserted(lastPosition);
                //mAdapter.notifyDataSetChanged();
                break;
            case DELETE_STATION:
                int positionToDelete = mDatabaseStations.indexOf(event.station);
                mRecyclerView.getAdapter().notifyItemRemoved(positionToDelete);
                mSelector.clearSelections();
                synchronizeStationsWithDatabase();
                break;
            case UPDATE_STATION:
                // FIXME
                synchronizeStationsWithDatabase();
                break;
        }

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
        handlePlaybackEvent(RadioApplication.sDatabase.playbackState);
        handleSelectStationEvent(new SelectStationEvent(RadioApplication.sDatabase.selectedStation));
    }

    @Subscribe
    public void handlePlaybackEvent(PlaybackEvent event) {
        if (event == PlaybackEvent.STOP) {
            mSelector.clearSelections();
        }
        // FIXME
        mAdapter.notifyDataSetChanged();
    }

    @Subscribe
    public void handleBufferEvent(BufferEvent event) {
        // FIXME
        mAdapter.notifyDataSetChanged();
    }

    @Subscribe
    public void handleSelectStationEvent(SelectStationEvent event) {
        int position = mDatabaseStations.indexOf(event.station);
        if (position >= 0) {
            // FIXME
//            mListView.smoothScrollToPosition(position);
        } else {
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
            //itemView.setLongClickable(true);
            mIcon = (ImageView) itemView.findViewById(R.id.icon);
            mIconBuffering = (ProgressBar) itemView.findViewById(R.id.icon_buffering);
            mIconPlaying = (ImageView) itemView.findViewById(R.id.icon_playing);
            mIconPlayingAnimation = (AnimationDrawable) mIconPlaying.getDrawable();
            mNameTextView = (TextView) itemView.findViewById(R.id.name);
            mUrlTextView = (TextView) itemView.findViewById(R.id.description);
        }

        @Override
        public void onClick(View v) {
            // close action mode on normal click
            if (mActionMode != null) {
                mActionMode.finish();
            }

            mSelector.tapSelection(this);
            mStation.setState(Station.State.PAUSED);

            RadioApplication.sBus.post(new SelectStationEvent(mStation));
            RadioApplication.sBus.post(PlaybackEvent.PLAY);
        }

        public void bindStation(Station station) {
            mStation = station;
            updateLayout();
        }

        private void updateLayout() {
            mNameTextView.setText(mStation.name);
            mUrlTextView.setText(mStation.url);

            // show either playback animation, buffering icon or stop icon
            mIcon.setVisibility(View.INVISIBLE);
            mIconBuffering.setVisibility(View.INVISIBLE);
            mIconPlaying.setVisibility(View.INVISIBLE);
            mIconPlayingAnimation.stop();  // TODO maybe not needed

            switch (mStation.getState()) {
                case STOPPED:
                    mIcon.setVisibility(View.VISIBLE);
                    break;
                case BUFFERING:
                    mIconBuffering.setVisibility(View.VISIBLE);
                    break;
                case PAUSED:
                    mIconPlaying.setVisibility(View.VISIBLE);
                    break;
                case PLAYING:
                    mIconPlaying.setVisibility(View.VISIBLE);
                    mIconPlayingAnimation.start();
                    break;
            }
        }

        @Override
        public boolean onLongClick(View v) {
            // start action mode to delete/edit item
            AppCompatActivity activity = (AppCompatActivity) getActivity();
            mActionMode = activity.startSupportActionMode(mDeleteMode);
            mSelector.setSelected(this, true);
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
