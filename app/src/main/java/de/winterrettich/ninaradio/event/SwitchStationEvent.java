package de.winterrettich.ninaradio.event;

import de.winterrettich.ninaradio.model.Station;

public class SwitchStationEvent {
    public Station station;
    public SwitchStationEvent(Station station) {
        this.station = station;
    }
}
