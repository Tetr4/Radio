package de.winterrettich.ninaradio.ui;

import android.app.Fragment;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import com.squareup.otto.Subscribe;

import de.winterrettich.ninaradio.R;
import de.winterrettich.ninaradio.RadioApplication;
import de.winterrettich.ninaradio.event.PlaybackEvent;
import de.winterrettich.ninaradio.event.SelectStationEvent;
import de.winterrettich.ninaradio.model.Station;

public class PlayBackControlsFragment extends Fragment implements View.OnClickListener {

    private ImageButton mPlayPauseButton;
    private Drawable mPlayDrawable;
    private Drawable mPauseDrawable;
    private TextView mStationNameTextView;
    private TextView mExtraInfoTextView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_playback_controls, container, false);

        mPlayPauseButton = (ImageButton) rootView.findViewById(R.id.play_pause);
        mPlayPauseButton.setOnClickListener(this);

        mStationNameTextView = (TextView) rootView.findViewById(R.id.title);
        mExtraInfoTextView = (TextView) rootView.findViewById(R.id.extra_info);

        mPlayDrawable = ContextCompat.getDrawable(getActivity(), R.drawable.ic_play);
        mPauseDrawable = ContextCompat.getDrawable(getActivity(), R.drawable.ic_pause);

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
        PlaybackEvent currentPlaybackState = RadioApplication.sPlaybackState;
        Station currentStation = RadioApplication.sStation;
        if (currentPlaybackState != null) {
            handlePlaybackEvent(RadioApplication.sPlaybackState);
        }
        if (currentStation != null) {
            handleSelectStationEvent(new SelectStationEvent(RadioApplication.sStation));
        }
    }

    @Override
    public void onClick(View v) {
        if (RadioApplication.sPlaybackState == PlaybackEvent.PLAY) {
            RadioApplication.sBus.post(PlaybackEvent.PAUSE);
        } else {
            RadioApplication.sBus.post(PlaybackEvent.PLAY);
        }
    }

    @Subscribe
    public void handlePlaybackEvent(PlaybackEvent event) {
        switch (event) {
            case PLAY:
                mPlayPauseButton.setImageDrawable(mPauseDrawable);
                break;
            case PAUSE:
                mPlayPauseButton.setImageDrawable(mPlayDrawable);
                break;
            case STOP:
                mPlayPauseButton.setImageDrawable(mPlayDrawable);
                break;
        }
    }

    @Subscribe
    public void handleSelectStationEvent(SelectStationEvent event) {
        mStationNameTextView.setText(event.station.name);
        mExtraInfoTextView.setText(event.station.url);
    }

}
