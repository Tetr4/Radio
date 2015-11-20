package de.winterrettich.ninaradio.event;

import de.winterrettich.ninaradio.model.Station;

public class DatabaseEvent {
    public final Station station;
    public enum Operation {
        CREATE_STATION, DELETE_STATION, UPDATE_STATION
    }
    public final Operation operation;

    public DatabaseEvent(Operation operation, Station station) {
        this.operation = operation;
        this.station = station;
    }
}
