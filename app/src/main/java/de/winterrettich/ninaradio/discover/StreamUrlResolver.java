package de.winterrettich.ninaradio.discover;

import java.io.IOException;
import java.net.URLConnection;

import io.reactivex.Observable;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class StreamUrlResolver {
    private static final String NEWLINE = System.getProperty("line.separator");
    private OkHttpClient mClient;

    public StreamUrlResolver(OkHttpClient client) {
        mClient = client;
    }

    /*
     * Get stream url from intermediate (API) url.
     * http://opml.radiotime.com/Tune.ashx?id=s6924 -> http://tx.whatson.com/icecast.php?i=rocklow.mp3
     */
    public Observable<String> resolve(String intermediateUrl) {
        return Observable.defer(() -> {
            // get stream urls (separated by newlines)
            String[] urls;
            try {
                Request request = new Request.Builder().url(intermediateUrl).build();
                Response response = mClient.newCall(request).execute();
                ResponseBody body = response.body();
                if (!response.isSuccessful() || body == null) {
                    throw new IOException("Could not resolve url " + intermediateUrl);
                }
                urls = body.string().split(NEWLINE);
            } catch (IOException e) {
                return Observable.error(e);
            }

            // check content type of urls to find one with a supported format
            for (String subUrl : urls) {
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
                        return Observable.just(subUrl);
                }
            }

            Exception e = new IOException("No supported format found for url " + intermediateUrl);
            return Observable.error(e);
        });
    }
}


