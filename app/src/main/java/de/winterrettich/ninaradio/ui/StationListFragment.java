package de.winterrettich.ninaradio.ui;

import android.app.Fragment;
import android.content.Context;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.StateListDrawable;
import android.media.MediaPlayer;
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
    private AnimationDrawable mSelectedItemIconAnimation;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_station_list, container, false);

        MediaPlayer mediaPlayer = new MediaPlayer();
        mStations.add(new Station("Rock", "http://197.189.206.172:8000/stream"));
        mStations.add(new Station("Blubb", "http://usa8-vn.mixstream.net:8138"));

        mListView = (ListView) rootView.findViewById(R.id.list_view);
        //mListView.addFooterView();
        mAdapter = new StationsListAdapter(getActivity(), mStations);
        mListView.setAdapter(mAdapter);
        mListView.setOnItemClickListener(this);

        RadioApplication.sBus.register(this);

        return rootView;
    }

    @Subscribe
    public void handlePlaybackEvent(PlaybackEvent event) {
        switch (event.type) {
            case PLAY:
                break;
            case PAUSE:
                break;
            case STOP:
                mListView.clearChoices();
                mListView.setAdapter(mAdapter);
                break;
        }
    }

    @Subscribe
    public void handleSelectStationEvent(SelectStationEvent event) {
        if(mStations.contains(event.station)) {
            int position = mAdapter.getPosition(event.station);
            mListView.setItemChecked(position, true);
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {


        // Animation
        if(mSelectedItemIconAnimation != null) {
            // cancel animation from previous item
            mSelectedItemIconAnimation.stop();
        }
        ImageView icon = (ImageView) view.findViewById(R.id.icon);
        StateListDrawable stateListDrawable = (StateListDrawable) icon.getDrawable();
        mSelectedItemIconAnimation = (AnimationDrawable) stateListDrawable.getCurrent();
        mSelectedItemIconAnimation.setVisible(true, true);
        mSelectedItemIconAnimation.start();

        Station station = (Station) parent.getItemAtPosition(position);

        RadioApplication.sBus.post(new SelectStationEvent(station));
        RadioApplication.sBus.post(new PlaybackEvent(PlaybackEvent.Type.PLAY));
    }

    private static class StationsListAdapter extends ArrayAdapter<Station> {

        public StationsListAdapter(Context context, List<Station> objects) {
            super(context, 0, objects);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            Station station = getItem(position);

            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.station_list_item, parent, false);
            }

            TextView nameTextView = (TextView) convertView.findViewById(R.id.name);
            TextView descriptionTextView = (TextView) convertView.findViewById(R.id.description);
            nameTextView.setText(station.name);
            descriptionTextView.setText(station.url);

            return convertView;
        }

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        RadioApplication.sBus.unregister(this);
    }
}
