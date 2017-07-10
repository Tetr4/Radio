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
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

import de.winterrettich.ninaradio.RadioApplication;
import de.winterrettich.ninaradio.model.Station;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class RadioTimeDeserializer implements JsonDeserializer<List<Station>> {
    public static final Type STATION_LIST_TYPE = new TypeToken<List<Station>>() {
    }.getType();
    private static final String TAG = RadioTimeDeserializer.class.getSimpleName();
    private static final String NEWLINE = System.getProperty("line.separator");
    private OkHttpClient mClient;

    public RadioTimeDeserializer(OkHttpClient client) {
        mClient = client;
    }

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

                String url = null;
                try {
                    url = getStreamUrl(intermediateUrl);
                } catch (IOException e) {
                    Log.w(TAG, e);
                }
                if (url == null) {
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
        // get stream url list from intemediateUrl
        Request request = new Request.Builder()
                .url(intermediateUrl)
                .build();
        Response response = mClient.newCall(request).execute();
        if (!response.isSuccessful()) {
            throw new IOException("Unexpected code " + response);
        }

        String[] urls = response.body().string().split(NEWLINE);
        for (String subUrl : urls) {
            // check content type of urls to find one with a supported format
            String contentType;
            if (subUrl.contains("?")) {
                // guess content type without query params
                String subUrlWithoutQueries = subUrl.substring(0, subUrl.indexOf("?"));
                contentType = URLConnection.guessContentTypeFromName(subUrlWithoutQueries);
            } else {
                contentType = URLConnection.guessContentTypeFromName(subUrl);
            }
            contentType = contentType == null ? "" : contentType;
            // TODO parse playlist format files (.m3u/.pls/.wax) on playback?
            // .m3u: audio/mpegurl
            // .pls: audio/x-scpls
            // .wax: audio/x-ms-wax
            // page: text/html
            // .mp3: audio/mpeg
            // unknown: <empty string>
            switch (contentType) {
                case "audio/mpeg":
                case "":
                    return subUrl;
            }
        }

        // no matching format found
        return null;
    }
}
