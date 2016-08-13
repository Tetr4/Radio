package de.winterrettich.ninaradio;


import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;

import com.activeandroid.ActiveAndroid;
import com.activeandroid.Configuration;

import de.winterrettich.ninaradio.event.DatabaseEvent;
import de.winterrettich.ninaradio.model.RadioDatabase;
import de.winterrettich.ninaradio.model.Station;

import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MockApplication extends RadioApplication {

    public static final String TEST_DB_NAME = "RadioTest.db";

    @SuppressLint("CommitPrefEdits")
    @Override
    protected void setupDatabase() {
        // mock shared preferences
        SharedPreferences emptyPrefs = mock(SharedPreferences.class);
        SharedPreferences.Editor mockedEditor = mock(SharedPreferences.Editor.class);
        when(emptyPrefs.getBoolean(RadioDatabase.PREF_FIRST_LAUNCH, true)).thenReturn(false);
        when(mockedEditor.putBoolean(anyString(), anyBoolean())).thenReturn(mockedEditor);
        when(mockedEditor.putLong(anyString(), anyLong())).thenReturn(mockedEditor);
        when(mockedEditor.putString(anyString(), anyString())).thenReturn(mockedEditor);
        when(mockedEditor.remove(anyString())).thenReturn(mockedEditor);
        when(emptyPrefs.edit()).thenReturn(mockedEditor);

        // mock database
        deleteDatabase(TEST_DB_NAME); // empty at first
        Configuration dbConfiguration = new Configuration.Builder(this)
                .setDatabaseName(TEST_DB_NAME)
                .create();
        ActiveAndroid.initialize(dbConfiguration);
        sDatabase = new RadioDatabase(emptyPrefs);
        sBus.register(sDatabase);
    }

    public void clearDatabase() {
        // run on main thread
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(new Runnable() {
            @Override
            public void run() {
                for (Station station: sDatabase.getStations()) {
                    sBus.post(new DatabaseEvent(DatabaseEvent.Operation.DELETE_STATION, station));
                }
            }
        });
    }

}
