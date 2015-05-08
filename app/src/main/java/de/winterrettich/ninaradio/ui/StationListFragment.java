package de.winterrettich.ninaradio.ui;

import android.app.Fragment;
import android.content.Context;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import de.winterrettich.ninaradio.R;
import de.winterrettich.ninaradio.RadioApplication;
import de.winterrettich.ninaradio.event.PlaybackEvent;
import de.winterrettich.ninaradio.event.SwitchStationEvent;
import de.winterrettich.ninaradio.model.Station;

public class StationListFragment extends Fragment implements AdapterView.OnItemClickListener {
    public StationListFragment() {}
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_station_list, container, false);

        MediaPlayer mediaPlayer = new MediaPlayer();
        List<Station> stations = new LinkedList<>();
        stations.add(new Station("Rock", "http://197.189.206.172:8000/stream"));
        stations.add(new Station("Blubb", "http://usa8-vn.mixstream.net:8138"));

        ListView listView = (ListView) rootView.findViewById(R.id.list_view);
        //listView.addFooterView();
        listView.setAdapter(new StationsListAdapter(getActivity(), stations));
        listView.setOnItemClickListener(this);

        return rootView;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Station station = (Station) parent.getItemAtPosition(position);
        RadioApplication.sBus.post(new SwitchStationEvent(station));
        RadioApplication.sBus.post(new PlaybackEvent(PlaybackEvent.Type.PLAY));
        Log.d("STATION", station.name);
    }

    private class StationsListAdapter extends ArrayAdapter<Station> {
        Map<String, Integer> mIdMap = new HashMap<String, Integer>();

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
}
