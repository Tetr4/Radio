package de.winterrettich.ninaradio.event;

import android.support.annotation.Nullable;
import android.text.TextUtils;

import java.util.Map;

public class MetadataEvent {
    public final Map<String, String> metadata;

    public MetadataEvent(Map<String, String> metadata) {
        this.metadata = metadata;
    }

    @Nullable
    public String getSongTitle() {
        return metadata.get("StreamTitle");
    }

    @Nullable
    public String getStationName() {
        return metadata.get("icy-name");
    }

    @Nullable
    public String getStationUrl() {
        return metadata.get("icy-url");
    }

    @Nullable
    public String getGenre() {
        return metadata.get("icy-genre");
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        for (Map.Entry<String, String> entry : metadata.entrySet()) {
            builder.append(entry.getKey());
            builder.append('-');
            builder.append(entry.getValue());
            builder.append('\n');
        }
        return builder.toString();
    }


    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other instanceof MetadataEvent) {
            MetadataEvent otherEvent = (MetadataEvent) other;

            return TextUtils.equals(getSongTitle(), otherEvent.getSongTitle())
                    && TextUtils.equals(getStationName(), otherEvent.getStationName())
                    && TextUtils.equals(getStationUrl(), otherEvent.getStationUrl())
                    && TextUtils.equals(getGenre(), otherEvent.getGenre());
        } else {
            return false;
        }
    }
}
