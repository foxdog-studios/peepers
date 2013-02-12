package com.foxdogstudios.peepers;

import android.os.Bundle;
import android.preference.PreferenceActivity;

public final class PeepersPreferenceActivity extends PreferenceActivity
{
    public PeepersPreferenceActivity()
    {
        super();
    } // constructor()

    @Override
    protected void onCreate(final Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
    } // onCreate()


} // class PeepersPreferenceActivity

