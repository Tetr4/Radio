package de.winterrettich.ninaradio.service;

import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.net.wifi.WifiManager;
import android.os.IBinder;
import android.os.SystemClock;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;

import com.squareup.otto.Subscribe;

import de.winterrettich.ninaradio.BuildConfig;
import de.winterrettich.ninaradio.RadioApplication;
import de.winterrettich.ninaradio.event.AdjustVolumeEvent;
import de.winterrettich.ninaradio.event.AudioFocusEvent;
import de.winterrettich.ninaradio.event.DismissNotificationEvent;
import de.winterrettich.ninaradio.event.HeadphoneDisconnectEvent;
import de.winterrettich.ninaradio.event.PlaybackEvent;
import de.winterrettich.ninaradio.event.SelectStationEvent;
import de.winterrettich.ninaradio.event.adapter.AudioFocusCallbackToEventAdapter;
import de.winterrettich.ninaradio.event.adapter.BroadcastToEventAdapter;
import de.winterrettich.ninaradio.event.adapter.MediaSessionCallbackToEventAdapter;
import de.winterrettich.ninaradio.model.Station;

/**
 * Service which controls a {@link RadioNotificationManager} and a {@link RadioPlayerManager} via various events:
 * <ul>
 * <li>AudioFocus gain and loss</li>
 * <li>WifiLock</li>
 * <li>Media Buttons</li>
 * <li>Transport Controls (play, pause, play from media uri, etc.)</li>
 * <li>Headphone disconnects</li>
 * <li>Notification callbacks (dismiss, play, pause)</li>
 * </ul>
 */
public class RadioPlayerService extends Service {
    public static final String TAG = RadioPlayerService.class.getSimpleName();
    public static final String EXTRA_STATION = Station.class.getSimpleName();
    private static final float DUCK_VOLUME = 0.15f;
    private float mLastVolume;
    private boolean mResumeAfterGain;
    private boolean mAdjustVolumeAfterGain;
    private boolean mSkipOverwrite;

    private MediaSessionCompat mMediaSession;
    private WifiManager.WifiLock mWifiLock;
    private BroadcastToEventAdapter mReceiver;
    private AudioFocusCallbackToEventAdapter mAudioFocusCallback;

    private RadioNotificationManager mRadioNotificationManager;
    private RadioPlayerManager mRadioPlayerManager;
    private AudioManager mAudioManager;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);

        if (mMediaSession == null) {
            initWifiLock();
            initAudioFocus();
            initMediaSession();
            initBroadCastReceiver();

            mRadioNotificationManager = new RadioNotificationManager(this, mMediaSession);
            mRadioPlayerManager = new RadioPlayerManager(this);


            RadioApplication.sBus.register(this);

            handleIntent(intent);
        }

        // restart with last station after being killed because of low memory
        return START_REDELIVER_INTENT;
    }

    private void handleIntent(Intent intent) {
        if (intent == null || intent.getExtras() == null || !intent.getExtras().containsKey(EXTRA_STATION)) {
            Log.i(TAG, "Service started without station");
            return;
        }

        Station station = intent.getExtras().getParcelable(EXTRA_STATION);
        Log.i(TAG, "Service started with station: " + station.name);
        mRadioNotificationManager.setStation(station);
        mRadioNotificationManager.setPlaybackState(PlaybackEvent.PLAY);
        mRadioPlayerManager.switchStation(station);
        mRadioPlayerManager.play();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        RadioApplication.sBus.unregister(this);
        unregisterReceiver(mReceiver);
        mAudioManager.abandonAudioFocus(mAudioFocusCallback);

        mRadioNotificationManager.hideNotification();
        mRadioPlayerManager.stop();
        mWifiLock.release();
        mMediaSession.release();

        Log.i(TAG, "Service destroyed");
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        stopSelf();
    }

    private void initBroadCastReceiver() {
        mReceiver = new BroadcastToEventAdapter();

        IntentFilter filter = new IntentFilter();
        filter.addAction(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
        filter.addAction(RadioNotificationManager.ACTION_NOTIFICATION_DISMISS);
        filter.addAction(RadioNotificationManager.ACTION_NOTIFICATION_PLAY);
        filter.addAction(RadioNotificationManager.ACTION_NOTIFICATION_PAUSE);

        registerReceiver(mReceiver, filter);
    }

    private void initWifiLock() {
        mWifiLock = ((WifiManager) getSystemService(WIFI_SERVICE))
                .createWifiLock(WifiManager.WIFI_MODE_FULL, BuildConfig.APPLICATION_ID);
        mWifiLock.acquire();
    }

    private void initAudioFocus() {
        // request AudioFocus + callbacks
        mAudioFocusCallback = new AudioFocusCallbackToEventAdapter();

        mAudioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        int result = mAudioManager.requestAudioFocus(mAudioFocusCallback, AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN);

        if (result != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            Log.w(TAG, "Could not get audio focus");
        }

        mLastVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
    }

    private void initMediaSession() {
        // receive all kinds of playback events
        ComponentName eventReceiver = new ComponentName(this, RadioPlayerService.class);
        PendingIntent buttonReceiverIntent = PendingIntent.getBroadcast(
                this,
                0,
                new Intent(Intent.ACTION_MEDIA_BUTTON),
                PendingIntent.FLAG_UPDATE_CURRENT
        );

        mMediaSession = new MediaSessionCompat(this, BuildConfig.APPLICATION_ID, eventReceiver, buttonReceiverIntent);
        mMediaSession.setActive(true);

        mMediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS | MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS);
        PlaybackStateCompat state = new PlaybackStateCompat.Builder()
                .setActions(PlaybackStateCompat.ACTION_PLAY
                        | PlaybackStateCompat.ACTION_PLAY_PAUSE
                        | PlaybackStateCompat.ACTION_PAUSE
                        | PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID)
                .setState(PlaybackStateCompat.STATE_STOPPED, 0, 1, SystemClock.elapsedRealtime())
                .build();
        mMediaSession.setPlaybackState(state);

        mMediaSession.setCallback(new MediaSessionCallbackToEventAdapter());
    }

    @Subscribe
    public void handlePlaybackEvent(PlaybackEvent event) {
        // TODO refactor to something better
        if (mSkipOverwrite) {
            // this event pauses playback, just after focus loss
            mSkipOverwrite = false;
        } else {
            // external event (e.g. user input while during focus loss)
            mResumeAfterGain = false;  // overwrite autoresume after focus loss
        }

        switch (event) {
            case PLAY:
                mRadioPlayerManager.play();
                mRadioNotificationManager.setPlaybackState(PlaybackEvent.PLAY);
                break;
            case PAUSE:
                mRadioPlayerManager.pause();
                mRadioNotificationManager.setPlaybackState(PlaybackEvent.PAUSE);
                break;
            case STOP:
                //stopSelf();
                // service will be stopped in application for now
                // player stop and notification dismiss is handled in onDestroy
                // TODO restart at some point?
                break;
        }
    }

    @Subscribe
    public void handleSelectStationEvent(SelectStationEvent event) {
        mRadioPlayerManager.switchStation(event.station);
        mRadioNotificationManager.setStation(event.station);
    }

    @Subscribe
    public void handleHeadphoneDisconnectEvent(HeadphoneDisconnectEvent event) {
        // pause playback when user accidentally disconnects headphones
        RadioApplication.sBus.post(PlaybackEvent.PAUSE);
    }

    @Subscribe
    public void handleDismissNotificationEvent(DismissNotificationEvent event) {
        // same as stop
        RadioApplication.sBus.post(PlaybackEvent.STOP);
    }

    @Subscribe
    public void handleAdjustVolumeEvent(AdjustVolumeEvent event) {
        // TODO refactor to something better
        if (mSkipOverwrite) {
            // this event sets the duck volume, just after focus loss
            mSkipOverwrite = false;
        } else {
            // external event (e.g. user input while during focus loss)
            mAdjustVolumeAfterGain = false; // overwrite duck volume
        }

        mRadioPlayerManager.setVolume(event.volume);
    }

    @Subscribe
    public void handleAudioFocusEvent(AudioFocusEvent event) {
        switch (event) {
            case AUDIOFOCUS_GAIN:
                if (mResumeAfterGain) {
                    // resume playback
                    RadioApplication.sBus.post(PlaybackEvent.PLAY);
                } else if (mAdjustVolumeAfterGain) {
                    // raise volume
                    RadioApplication.sBus.post(new AdjustVolumeEvent(mLastVolume));
                }
                break;

            case AUDIOFOCUS_LOSS:
                RadioApplication.sBus.post(PlaybackEvent.STOP);
                break;

            case AUDIOFOCUS_LOSS_TRANSIENT:
                // Lost focus for a short time
                PlaybackEvent currentPlaybackState = RadioApplication.sDatabase.playbackState;
                mResumeAfterGain = currentPlaybackState == PlaybackEvent.PLAY;
                mSkipOverwrite = true;
                RadioApplication.sBus.post(PlaybackEvent.PAUSE);
                break;

            case AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                // Lost focus for a short time but can play at low volume
                mLastVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
                mAdjustVolumeAfterGain = true;
                mSkipOverwrite = true;
                RadioApplication.sBus.post(new AdjustVolumeEvent(Math.min(DUCK_VOLUME, mLastVolume)));
                break;
        }
    }

}