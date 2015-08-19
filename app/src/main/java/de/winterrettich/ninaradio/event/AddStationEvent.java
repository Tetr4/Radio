package de.winterrettich.ninaradio.event;

import de.winterrettich.ninaradio.model.Station;

public class AddStationEvent {
    public Station station;

    public AddStationEvent(Station station) {
        this.station = station;
    }
}
