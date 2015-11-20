package de.winterrettich.ninaradio.service;

import android.os.Handler;
import android.util.Log;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

import de.winterrettich.ninaradio.RadioApplication;
import de.winterrettich.ninaradio.event.MetadataEvent;
import de.winterrettich.ninaradio.metadata.MetaDataDecoder;
import de.winterrettich.ninaradio.metadata.MissingDataException;
import de.winterrettich.ninaradio.model.Station;

public class MetadataRetriever {
    private static final String TAG = MetadataRetriever.class.getSimpleName();
    private static final int DELAY = 2000;

    private boolean mIsStopped = true;
    private Station mStation;

    private Handler mUiThreadHandler = new Handler();
    private Thread mThread;
    private Runnable mUpdateLooper;

    public void switchStation(final Station station) {
        mStation = station;

        // stop previous thread
        if (mThread != null && !mThread.isInterrupted()) {
            mThread.interrupt();
        }

        final URL url;
        try {
            url = new URL(station.url);
        } catch (MalformedURLException e) {
            // should not happen, and even then it is handled by the media player
            Log.e(TAG, "Malformed Url");
            return;
        }

        mUpdateLooper = new Runnable() {
            @Override
            public void run() {
                MetaDataDecoder streamMeta = new MetaDataDecoder(url);
                while (!Thread.currentThread().isInterrupted()) {
                    // get metadata
                    MetadataEvent event = null;
                    try {
                        Map<String, String> metadata = streamMeta.retrieveMetadata();
                        if (metadata.containsKey("StreamTitle") && !metadata.get("StreamTitle").isEmpty()) {
                            event = new MetadataEvent(metadata.get("StreamTitle"));
                        }
                    } catch (IOException e) {
                        Log.v(TAG, "Could not get metadata");
                    } catch (MissingDataException e) {
                        e.printStackTrace();
                    }
                    if (event != null) {
                        postUpdateOnUiThread(event, station);
                    }

                    try {
                        Thread.sleep(DELAY);
                    } catch (InterruptedException e) {
                        // stop looping
                        return;
                    }
                }
            }
        };

        // resume
        if (!mIsStopped) {
            mThread = new Thread(mUpdateLooper);
            mThread.start();
        }

    }

    private void postUpdateOnUiThread(final MetadataEvent event, final Station station) {
        mUiThreadHandler.post(new Runnable() {
            @Override
            public void run() {
                if (!mIsStopped && mStation == station) {
                    RadioApplication.sBus.post(event);
                }
            }
        });
    }

    public void start() {
        mIsStopped = false;

        // stop previous thread
        if (mThread != null && !mThread.isInterrupted()) {
            mThread.interrupt();
        }

        // check if source was set
        if (mUpdateLooper == null) {
            throw new IllegalStateException("Source has not been set.");
        }

        // start new thread
        mThread = new Thread(mUpdateLooper);
        mThread.start();
    }

    public void stop() {
        mIsStopped = true;

        if (mThread != null && !mThread.isInterrupted()) {
            mThread.interrupt();
            mThread = null;
        }
    }

}
