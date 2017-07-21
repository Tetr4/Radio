package de.winterrettich.ninaradio.discover;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import de.winterrettich.ninaradio.model.Station;

public class RadioTimeDeserializer implements JsonDeserializer<List<Station>> {
    public static final Type STATION_LIST_TYPE = new TypeToken<List<Station>>() {
    }.getType();

    @Override
    public List<Station> deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        List<Station> stations = new ArrayList<>();

        // skip root and body element
        JsonObject root = json.getAsJsonObject();
        JsonArray body = root.getAsJsonArray("body");
        for (JsonElement outlineElement : body) {
            JsonObject outline = outlineElement.getAsJsonObject();
            // use only elements with type="audio"
            JsonPrimitive type = outline.getAsJsonPrimitive("type");
            if (type != null && type.getAsString().equals("audio")) {
                String name = outline.getAsJsonPrimitive("text").getAsString();
                String url = outline.getAsJsonPrimitive("URL").getAsString();
                stations.add(new Station(name, url));
            }
        }

        return stations;
    }

}
