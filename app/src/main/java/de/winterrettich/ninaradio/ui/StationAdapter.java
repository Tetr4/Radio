package de.winterrettich.ninaradio.ui;

import android.content.Context;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Space;
import android.widget.TextView;

import com.bignerdranch.android.multiselector.SingleSelector;
import com.bignerdranch.android.multiselector.SwappingHolder;
import com.github.ivbaranov.mfb.MaterialFavoriteButton;

import java.util.Collections;
import java.util.List;

import de.winterrettich.ninaradio.R;
import de.winterrettich.ninaradio.RadioApplication;
import de.winterrettich.ninaradio.event.BufferEvent;
import de.winterrettich.ninaradio.event.PlaybackEvent;
import de.winterrettich.ninaradio.model.Station;

/**
 * Adapter that handles a list of stations for a RecyclerView.
 * Stations can be selected and show their current status, e.g. paused, playing, buffering.
 * Possible action on stations are click, longclick and favorite.
 * The favorite button can be disabled.
 */
public class StationAdapter extends RecyclerView.Adapter<StationAdapter.StationHolder> {
    private SingleSelector mSelector = new SingleSelector();
    private List<Station> mStations = Collections.emptyList();
    private StationClickListener mClickListener;
    private boolean mShowFavorites = true;

    public StationAdapter(StationClickListener listener) {
        mClickListener = listener;
        mSelector.setSelectable(true);
    }

    public void showFavorites(boolean show) {
        mShowFavorites = show;
    }

    @Override
    public StationHolder onCreateViewHolder(ViewGroup parent, int pos) {
        Context context = parent.getContext();
        View view = LayoutInflater.from(context).inflate(R.layout.list_item_station, parent, false);
        Drawable selectorDrawable = ContextCompat.getDrawable(context, R.drawable.station_list_selector);
        return new StationHolder(view, selectorDrawable);
    }

    @Override
    public void onBindViewHolder(StationHolder holder, int pos) {
        Station station = mStations.get(pos);
        holder.bindStation(station);
    }

    @Override
    public int getItemCount() {
        return mStations.size();
    }

    public void setStations(List<Station> stations) {
        mStations = stations;
        notifyDataSetChanged();
    }

    public void insertStation(Station station, int position) {
        mStations.add(position, station);
        notifyItemInserted(position);
    }

    public void deleteStation(Station station) {
        int positionToDelete = mStations.indexOf(station);
        if (positionToDelete > -1) { // is not deleted
            mStations.remove(station);
            notifyItemRemoved(positionToDelete);
        }
    }

    public void updateStation(Station station) {
        int positionToUpdate = mStations.indexOf(station);
        if (positionToUpdate > -1) { // exists
            mStations.set(positionToUpdate, station);
            notifyItemChanged(positionToUpdate);
        }
    }

    public void setSelection(Station station) {
        int position = mStations.indexOf(station);
        if (position >= 0) {
            mSelector.setSelected(position, 0, true);
            // update all stations, as previously selected station must be redrawn
            notifyDataSetChanged();
        } else {
            // not in list
            clearSelection();
        }
    }

    public Station getSelection() {
        if (mSelector.getSelectedPositions().isEmpty()) {
            return null;
        }
        int position = mSelector.getSelectedPositions().get(0);
        return mStations.get(position);
    }

    public void clearSelection() {
        mSelector.clearSelections();
        notifyDataSetChanged();
    }

    protected class StationHolder extends SwappingHolder implements View.OnClickListener, View.OnLongClickListener, MaterialFavoriteButton.OnFavoriteChangeListener {
        private final ImageView mIcon;
        private final ProgressBar mIconBuffering;
        private final ImageView mIconPlaying;
        private final AnimationDrawable mIconPlayingAnimation;
        private final TextView mNameTextView;
        private final TextView mUrlTextView;
        private final MaterialFavoriteButton mFavoriteButton;
        private final Space mFavoriteGonePadding;

        private Station mStation;

        public StationHolder(View itemView, Drawable selectorDrawable) {
            super(itemView, mSelector);
            setSelectionModeBackgroundDrawable(selectorDrawable);
            setDefaultModeBackgroundDrawable(selectorDrawable);
            // setSelectionModeStateListAnimator();
            // setDefaultModeStateListAnimator();
            itemView.setOnClickListener(this);
            itemView.setOnLongClickListener(this);

            mIcon = (ImageView) itemView.findViewById(R.id.icon);
            mIconBuffering = (ProgressBar) itemView.findViewById(R.id.icon_buffering);
            mIconPlaying = (ImageView) itemView.findViewById(R.id.icon_playing);
            mIconPlayingAnimation = (AnimationDrawable) mIconPlaying.getDrawable();
            mNameTextView = (TextView) itemView.findViewById(R.id.name);
            mUrlTextView = (TextView) itemView.findViewById(R.id.description);
            mFavoriteButton = (MaterialFavoriteButton) itemView.findViewById(R.id.favorite_button);
            mFavoriteGonePadding = (Space) itemView.findViewById(R.id.favorite_gone_padding);
        }

        public void bindStation(Station station) {
            mStation = station;

            mNameTextView.setText(mStation.name);
            mUrlTextView.setText(mStation.url);

            // show either playback animation, buffering icon or stop icon
            boolean isSelected = mStation.equals(RadioApplication.sDatabase.selectedStation);
            boolean isBuffering = RadioApplication.sDatabase.bufferingState == BufferEvent.BUFFERING;
            boolean isPaused = RadioApplication.sDatabase.playbackState == PlaybackEvent.PAUSE;
            boolean isStopped = RadioApplication.sDatabase.playbackState == PlaybackEvent.STOP;
            boolean isFavorite = RadioApplication.sDatabase.getStations().contains(mStation); // TODO HashSet?

            if (isStopped || !isSelected) {
                // default/stopped icon
                mIcon.setVisibility(View.VISIBLE);
                mIconBuffering.setVisibility(View.INVISIBLE);
                mIconPlaying.setVisibility(View.INVISIBLE);
            } else if (isPaused) {
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
                // playing/animated icon
                mIcon.setVisibility(View.INVISIBLE);
                mIconBuffering.setVisibility(View.INVISIBLE);
                mIconPlaying.setVisibility(View.VISIBLE);
                mIconPlayingAnimation.start();
            }

            if (!mShowFavorites) {
                mFavoriteButton.setVisibility(View.GONE);
                mFavoriteGonePadding.setVisibility(View.VISIBLE);
            } else {
                // don't receive callback for initial setting
                mFavoriteButton.setOnFavoriteChangeListener(null);
                mFavoriteButton.setFavorite(isFavorite, false);
                mFavoriteButton.setOnFavoriteChangeListener(this);
            }

        }

        @Override
        public void onClick(View v) {
            // delegate
            if (mClickListener != null) {
                mClickListener.onClick(mStation);
            }
        }

        @Override
        public boolean onLongClick(View v) {
            // delegate
            return mClickListener.onLongClick(mStation);
        }

        @Override
        public void onFavoriteChanged(MaterialFavoriteButton buttonView, boolean favorite) {
            // delegate
            mClickListener.onFavoriteChanged(mStation, favorite);
        }
    }

    public interface StationClickListener {
        void onClick(Station station);
        boolean onLongClick(Station station);
        void onFavoriteChanged(Station station, boolean favorite);
    }

}
