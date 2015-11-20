package de.winterrettich.ninaradio.event;

public class PlayerErrorEvent {
    public final String message;

    public PlayerErrorEvent(String message) {
        this.message = message;
    }
}
