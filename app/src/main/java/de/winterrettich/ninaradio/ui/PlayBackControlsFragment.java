package de.winterrettich.ninaradio.ui;

import android.app.Fragment;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.squareup.otto.Subscribe;

import de.winterrettich.ninaradio.R;
import de.winterrettich.ninaradio.RadioApplication;
import de.winterrettich.ninaradio.event.PlaybackEvent;

public class PlayBackControlsFragment extends Fragment implements View.OnClickListener {

    private Drawable mPlayDrawable;
    private Drawable mPauseDrawable;
    private FloatingActionButton mFloatingActionButton;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_playback_controls, container, false);

        mFloatingActionButton = (FloatingActionButton) rootView.findViewById(R.id.fab);
        mFloatingActionButton.setOnClickListener(this);

//        Snackbar
//                .make(findViewById(R.id.root_layout),
//                        "This is Snackbar",
//                        Snackbar.LENGTH_LONG)
//                .setAction("Action", this)
//                .show(); // Do not forget to show!

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
        handlePlaybackEvent(RadioApplication.sPlaybackState);
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
                mFloatingActionButton.setImageDrawable(mPauseDrawable);
                break;
            case PAUSE:
                mFloatingActionButton.setImageDrawable(mPlayDrawable);
                break;
            case STOP:
                mFloatingActionButton.setImageDrawable(mPlayDrawable);
                break;
        }
    }

}
