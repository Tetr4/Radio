package de.winterrettich.ninaradio;

import android.app.Application;

import com.squareup.otto.Bus;
import com.squareup.otto.ThreadEnforcer;

/**
 * Created by mike on 08.05.15.
 */
public class RadioApplication extends Application {
    public static Bus sBus = new Bus(ThreadEnforcer.MAIN);

}
