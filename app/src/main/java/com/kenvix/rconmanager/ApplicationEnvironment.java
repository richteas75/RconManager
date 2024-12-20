package com.kenvix.rconmanager;

/*import android.support.v7.app.AppCompatActivity;*/

import androidx.appcompat.app.AppCompatActivity;

import com.kenvix.rconmanager.ui.base.view.IconManager;
import com.kenvix.rconmanager.utils.Invoker;

public final class ApplicationEnvironment {
    private static final int pausedResponseInterval = 50;
    private static final String RconURINameParamKey = "name";

    public static String getRconURINameParamKey() {
        return RconURINameParamKey;
    }

    public static class NotificationChannelID {
        public static String RconConnection = "rcon_connection";
    }

    public static void initializeApplication(AppCompatActivity baseActivity) {
        if(Invoker.getBaseActivityInvocation() == null) {
            IconManager.initialize(baseActivity);
            Invoker.setBaseActivityInvocation(baseActivity);
        }
    }

    public static int getPausedResponseInterval() {
        return pausedResponseInterval;
    }
}
