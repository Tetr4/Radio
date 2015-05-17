package de.winterrettich.ninaradio.ui;

import android.app.Fragment;
import android.content.Context;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.squareup.otto.Subscribe;

import java.util.LinkedList;
import java.util.List;

import de.winterrettich.ninaradio.R;
import de.winterrettich.ninaradio.RadioApplication;
import de.winterrettich.ninaradio.event.PlaybackEvent;
import de.winterrettich.ninaradio.event.SelectStationEvent;
import de.winterrettich.ninaradio.model.Station;

public class StationListFragment extends Fragment implements AdapterView.OnItemClickListener {
    private ListView mListView;
    private StationsListAdapter mAdapter;
    private LinkedList<Station> mStations = new LinkedList<>();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_station_list, container, false);

        mStations.add(new Station("Rock", "http://197.189.206.172:8000/stream"));
        mStations.add(new Station("Spanisch", "http://usa8-vn.mixstream.net:8138"));
        mStations.add(new Station("FFN", "http://player.ffn.de/ffnstream.mp3"));
        mStations.add(new Station("Antenne Niedersachsen", "http://stream.antenne.com/antenne-nds/mp3-128/radioplayer/"));
        mStations.add(new Station("1Live", "http://gffstream.ic.llnwd.net/stream/gffstream_stream_wdr_einslive_a"));
        mStations.add(new Station("Radio Gütersloh", "http://edge.live.mp3.mdn.newmedia.nacamar.net/radioguetersloh/livestream.mp3"));

        mListView = (ListView) rootView.findViewById(R.id.list_view);
        //mListView.addFooterView();
        mAdapter = new StationsListAdapter(getActivity(), mStations);
        mListView.setAdapter(mAdapter);
        mListView.setOnItemClickListener(this);

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
        if (currentPlaybackState != null) {
            handlePlaybackEvent(RadioApplication.sPlaybackState);
        }

        Station currentStation = RadioApplication.sStation;
        if (currentStation != null) {
            handleSelectStationEvent(new SelectStationEvent(RadioApplication.sStation));
        }
    }

    @Subscribe
    public void handlePlaybackEvent(PlaybackEvent event) {
        if (event == PlaybackEvent.STOP) {
            mListView.clearChoices();
        }
        mAdapter.notifyDataSetChanged();
    }

    @Subscribe
    public void handleSelectStationEvent(SelectStationEvent event) {
        if (mStations.contains(event.station)) {
            int position = mAdapter.getPosition(event.station);
            mListView.smoothScrollToPosition(position);
            mListView.setItemChecked(position, true);
        } else {
            mListView.clearChoices();
        }
        mAdapter.notifyDataSetChanged();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Station station = (Station) parent.getItemAtPosition(position);
        RadioApplication.sBus.post(new SelectStationEvent(station));
        RadioApplication.sBus.post(PlaybackEvent.PLAY);
    }

    private class StationsListAdapter extends ArrayAdapter<Station> {

        public StationsListAdapter(Context context, List<Station> objects) {
            super(context, 0, objects);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            Station station = getItem(position);

            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.station_list_item, parent, false);
            }

            // start icon animation if checked and playing
            int checkedPosition = ((ListView) parent).getCheckedItemPosition();
            convertView.setActivated(position == checkedPosition);
            if (convertView.isActivated() && RadioApplication.sPlaybackState == PlaybackEvent.PLAY) {
                startIconAnimation(convertView);
            } else {
                stopIconAnimation(convertView);
            }

            TextView nameTextView = (TextView) convertView.findViewById(R.id.name);
            TextView descriptionTextView = (TextView) convertView.findViewById(R.id.description);
            nameTextView.setText(station.name);
            descriptionTextView.setText(station.url);

            return convertView;
        }

        private AnimationDrawable findIconAnimationDrawable(View view) {
            ImageView icon = (ImageView) view.findViewById(R.id.icon);
            StateListDrawable stateListDrawable = (StateListDrawable) icon.getDrawable();
            Drawable current = stateListDrawable.getCurrent();

            if (current instanceof AnimationDrawable) {
                return (AnimationDrawable) current;
            }
            return null;
        }

        private void startIconAnimation(View view) {
            AnimationDrawable animation = findIconAnimationDrawable(view);
            if (animation != null && !animation.isRunning()) {
                animation.setVisible(true, true);
                animation.start();
            }
        }

        private void stopIconAnimation(View view) {
            AnimationDrawable animation = findIconAnimationDrawable(view);
            if (animation != null && animation.isRunning()) {
                animation.stop();
            }
        }

    }
}
