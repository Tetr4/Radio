package de.winterrettich.ninaradio.discover;


import android.util.Log;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import de.winterrettich.ninaradio.RadioApplication;
import de.winterrettich.ninaradio.model.Station;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class RadioTimeDeserializer implements JsonDeserializer<List<Station>> {
    private static final String TAG = RadioTimeDeserializer.class.getSimpleName();
    public static final Type STATION_LIST_TYPE = new TypeToken<List<Station>>(){}.getType();

    private OkHttpClient mClient = new OkHttpClient();

    @Override
    public List<Station> deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        List<Station> stations = new ArrayList<>();

        JsonObject root = json.getAsJsonObject();
        JsonArray body = root.getAsJsonArray("body");
        for (JsonElement outlineElement : body) {
            JsonObject outline = outlineElement.getAsJsonObject();
            JsonPrimitive type = outline.getAsJsonPrimitive("type");
            if (type != null && type.getAsString().equals("audio")) {
                String name = outline.getAsJsonPrimitive("text").getAsString();
                String intermediateUrl = outline.getAsJsonPrimitive("URL").getAsString();

                String url;
                try {
                    url = getStreamUrl(intermediateUrl);
                } catch (IOException e) {
                    Log.w(TAG, e);
                    // skip
                    continue;
                }

                // find existing station
                Station station = RadioApplication.sDatabase.findMatchingStation(name, url);
                if (station == null) {
                    // create new station
                    station = new Station(name, url);
                }
                stations.add(station);
            }
        }

        return stations;
    }

    private String getStreamUrl(String intermediateUrl) throws IOException {
        Request request = new Request.Builder()
                .url(intermediateUrl)
                .build();

        Response response = mClient.newCall(request).execute();
        if (!response.isSuccessful()) {
            throw new IOException("Unexpected code " + response);
        }

        String newline = System.getProperty("line.separator");
        String[] urls = response.body().string().split(newline);
        return urls[0];
    }
}
