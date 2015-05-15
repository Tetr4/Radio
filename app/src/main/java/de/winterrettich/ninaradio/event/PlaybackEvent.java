package de.winterrettich.ninaradio.event;

public class PlaybackEvent {
    public Type type;

    // TODO PlaybackStateCompat
    public enum Type {
        PLAY, PAUSE, STOP
    }

    public PlaybackEvent(Type type) {
        this.type = type;
    }
}
