package de.winterrettich.ninaradio.service;

import android.os.Handler;
import android.util.Log;

import java.io.IOException;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import de.winterrettich.ninaradio.RadioApplication;
import de.winterrettich.ninaradio.event.MetadataEvent;
import de.winterrettich.ninaradio.metadata.MetaDataDecoder;
import de.winterrettich.ninaradio.metadata.MissingMetadataException;
import de.winterrettich.ninaradio.model.Station;

public class MetadataRetriever {
    private static final String TAG = MetadataRetriever.class.getSimpleName();
    private static final int DELAY = 5000;

    private boolean mIsStopped = true;
    private Station mStation;
    private MetadataEvent mLastEvent;

    private Handler mUiThreadHandler = new Handler(); // communicate with ui thread
    private Runnable mUpdateMetadata;
    private ScheduledExecutorService mExecutor = Executors.newSingleThreadScheduledExecutor();
    private Future<?> mRunningTask; // used to cancel running task

    public void switchStation(final Station station) {
        mStation = station;
        mLastEvent = null;

        // cancel previous task
        if (mRunningTask != null) {
            mRunningTask.cancel(true);
        }


        mUpdateMetadata = new Runnable() {
            @Override
            public void run() {

                // get metadata
                Map<String, String> metadata = null;
                try {
                    URL url = new URL(station.url);
                    metadata = MetaDataDecoder.retrieveMetadata(url);
                } catch (MissingMetadataException | IOException e) {
                    // try again on next loop
                    Log.v(TAG, "Could not get metadata");
                }

                // post on ui thread
                if (metadata != null && !Thread.currentThread().isInterrupted()) {
                    postUpdateOnUiThread(metadata, station.url);
                }
            }
        };

        // resume
        if (!mIsStopped) {
            mRunningTask = mExecutor.scheduleWithFixedDelay(mUpdateMetadata, 0, DELAY, TimeUnit.MILLISECONDS);
        }

    }

    private void postUpdateOnUiThread(final Map<String, String> metadata, final String url) {
        mUiThreadHandler.post(new Runnable() {

            @Override
            public void run() {
                MetadataEvent event = new MetadataEvent(metadata);
                // only post if metadata has song title, matches current station, data has changed
                if (!mIsStopped && event.getSongTitle() != null && mStation.url.equals(url) && !event.equals(mLastEvent)) {
                    RadioApplication.sBus.post(event);
                    mLastEvent = event;
                }
            }

        });
    }

    public void start() {
        mIsStopped = false;

        // cancel previous task
        if (mRunningTask != null) {
            mRunningTask.cancel(true);
        }

        // check if source was set
        if (mUpdateMetadata == null) {
            throw new IllegalStateException("Source has not been set.");
        }

        // start new thread
        mRunningTask = mExecutor.scheduleWithFixedDelay(mUpdateMetadata, 0, DELAY, TimeUnit.MILLISECONDS);
    }

    public void stop() {
        mIsStopped = true;

        if (mRunningTask != null) {
            mRunningTask.cancel(true);
        }
    }

}
