package de.winterrettich.ninaradio;

import android.app.Application;
import android.content.Intent;
import android.content.SharedPreferences;

import com.activeandroid.ActiveAndroid;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;
import com.squareup.otto.ThreadEnforcer;

import de.winterrettich.ninaradio.event.EventLogger;
import de.winterrettich.ninaradio.event.PlaybackEvent;
import de.winterrettich.ninaradio.model.RadioDatabase;
import de.winterrettich.ninaradio.service.RadioPlayerService;

public class RadioApplication extends Application {
    private static final String PREF_FIRST_LAUNCH = "PREF_FIRST_LAUNCH";
    public static Bus sBus = new Bus(ThreadEnforcer.MAIN);
    public static RadioDatabase sDatabase;
    public static boolean sIsFirstLaunch;
    private EventLogger mLogger;

    @Override
    public void onCreate() {
        super.onCreate();
        sBus.register(this);

        // logger
        mLogger = new EventLogger();
        sBus.register(mLogger);

        // check first launch
        SharedPreferences prefs = getSharedPreferences(BuildConfig.APPLICATION_ID, MODE_PRIVATE);
        sIsFirstLaunch = prefs.getBoolean(PREF_FIRST_LAUNCH, true);
        // set to false next time
        prefs.edit().putBoolean(PREF_FIRST_LAUNCH, false).apply();

        // database
        ActiveAndroid.initialize(this);
        sDatabase = new RadioDatabase();
        sBus.register(sDatabase);
    }

    @Subscribe
    public void handlePlaybackEvent(PlaybackEvent event) {
        switch (event) {
            case PLAY:
                if (sDatabase.selectedStation == null) {
                    throw new IllegalStateException("Select a Station before playing");
                }
                // start service
                Intent serviceIntent = new Intent(this, RadioPlayerService.class);
                startService(serviceIntent);
                break;

            case STOP:
                // stop service
                Intent intent = new Intent(getApplicationContext(), RadioPlayerService.class);
                stopService(intent);
                break;
        }
    }

}
