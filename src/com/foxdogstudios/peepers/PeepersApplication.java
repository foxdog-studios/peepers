package com.foxdogstudios.peepers;

import android.app.Application;
import android.os.StrictMode;

public final class PeepersApplication extends Application
{
    public PeepersApplication()
    {
        super();
        StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                .detectDiskReads()
                .detectDiskWrites()
                .detectNetwork()
                .penaltyLog()
                .build());
        StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                .detectLeakedSqlLiteObjects()
                .penaltyLog()
                .build());
    } // constructor()


} // PeepersApplication

