package de.winterrettich.ninaradio.ui;

import android.app.Fragment;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.drawable.AnimationDrawable;
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

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import de.winterrettich.ninaradio.R;
import de.winterrettich.ninaradio.RadioApplication;
import de.winterrettich.ninaradio.event.AddStationEvent;
import de.winterrettich.ninaradio.event.BufferEvent;
import de.winterrettich.ninaradio.event.PlaybackEvent;
import de.winterrettich.ninaradio.event.SelectStationEvent;
import de.winterrettich.ninaradio.model.Station;

public class StationListFragment extends Fragment implements AdapterView.OnItemClickListener {
    public static final String STATIONS_PREFERENCE = "STATIONS_PREFERENCE";
    private ListView mListView;
    private StationsListAdapter mAdapter;
    private LinkedList<Station> mStations = new LinkedList<>();
    private SharedPreferences mSharedPreferences;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_station_list, container, false);

        mSharedPreferences = getActivity().getSharedPreferences(STATIONS_PREFERENCE, Context.MODE_PRIVATE);
        if (mSharedPreferences.getAll().isEmpty()) {
            loadDefaultStations();
        } else {
            loadSavedStations();
        }


        mListView = (ListView) rootView.findViewById(R.id.list_view);

        mAdapter = new StationsListAdapter(getActivity(), mStations);
        mListView.setAdapter(mAdapter);
        mListView.setOnItemClickListener(this);

        return rootView;
    }

    private void loadDefaultStations() {
        mStations.add(new Station("Rock", "http://197.189.206.172:8000/stream"));
        mStations.add(new Station("Spanisch", "http://usa8-vn.mixstream.net:8138"));
        mStations.add(new Station("FFN", "http://player.ffn.de/ffnstream.mp3"));
        mStations.add(new Station("Antenne Niedersachsen", "http://stream.antenne.com/antenne-nds/mp3-128/radioplayer/"));
        mStations.add(new Station("1Live", "http://gffstream.ic.llnwd.net/stream/gffstream_stream_wdr_einslive_a"));
        mStations.add(new Station("Radio Gütersloh", "http://edge.live.mp3.mdn.newmedia.nacamar.net/radioguetersloh/livestream.mp3"));
        mStations.add(new Station("Rock2", "http://197.189.206.172:8000/stream"));
        mStations.add(new Station("Rock3", "http://197.189.206.172:8000/stream"));
        mStations.add(new Station("Rock4", "http://197.189.206.172:8000/stream"));
        mStations.add(new Station("Rock5", "http://197.189.206.172:8000/stream"));
        Collections.sort(mStations);
    }

    private void loadSavedStations() {
        for (Map.Entry<String, ?> entry : mSharedPreferences.getAll().entrySet()) {
            String name = entry.getKey();
            String url = (String) entry.getValue();
            mStations.add(new Station(name, url));
        }
        Collections.sort(mStations);
    }

    private void saveStations() {
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        for (Station station : mStations) {
            editor.putString(station.name, station.url);
        }
        editor.commit();
    }

    @Subscribe
    public void handleAddStationEvent(AddStationEvent event) {
        mStations.add(event.station);
        Collections.sort(mStations);
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
        saveStations();
    }

    private void refreshUi() {
        handlePlaybackEvent(RadioApplication.sPlaybackState);
        handleSelectStationEvent(new SelectStationEvent(RadioApplication.sStation));
    }

    @Subscribe
    public void handlePlaybackEvent(PlaybackEvent event) {
        if (event == PlaybackEvent.STOP) {
            mListView.clearChoices();
        }
        mAdapter.notifyDataSetChanged();
    }

    @Subscribe
    public void handleBufferEvent(BufferEvent event) {
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

            // set activated if checked in list
            int checkedPosition = ((ListView) parent).getCheckedItemPosition();
            convertView.setActivated(position == checkedPosition);

            // hide all icons and stop animation
            View icon = convertView.findViewById(R.id.icon);
            ImageView icon_playing = (ImageView) convertView.findViewById(R.id.icon_playing);
            View icon_buffering = convertView.findViewById(R.id.icon_buffering);
            icon.setVisibility(View.INVISIBLE);
            icon_playing.setVisibility(View.INVISIBLE);
            icon_buffering.setVisibility(View.INVISIBLE);
            AnimationDrawable animation = (AnimationDrawable) icon_playing.getDrawable();
            animation.stop();

            // show either playback animation, buffering icon or stop icon
            if (convertView.isActivated()) {
                if (RadioApplication.sBufferingState == BufferEvent.BUFFERING) {
                    icon_buffering.setVisibility(View.VISIBLE);
                } else if (RadioApplication.sPlaybackState == PlaybackEvent.PLAY) {
                    icon_playing.setVisibility(View.VISIBLE);
                    animation.start();
                } else {
                    icon_playing.setVisibility(View.VISIBLE);
                }
            } else {
                icon.setVisibility(View.VISIBLE);
            }

            TextView nameTextView = (TextView) convertView.findViewById(R.id.name);
            TextView descriptionTextView = (TextView) convertView.findViewById(R.id.description);
            nameTextView.setText(station.name);
            descriptionTextView.setText(station.url);

            return convertView;
        }

    }
}
