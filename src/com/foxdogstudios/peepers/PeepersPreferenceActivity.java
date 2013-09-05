/* Copyright 2013 Foxdog Studios Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.foxdogstudios.peepers;

import java.util.List;

import android.hardware.Camera;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
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
        final ListPreference sizePreference = (ListPreference) findPreference("size");

        setSizePreferences(sizePreference);

        sizePreference.setOnPreferenceClickListener(new OnPreferenceClickListener(){
            @Override
            public boolean onPreferenceClick(Preference preference) {
                setSizePreferences(sizePreference);
                return false;
            }
        });
    } // onCreate()

    private void setSizePreferences(final ListPreference sizePreference)
    {
        final Camera camera = Camera.open();
        final Camera.Parameters params = camera.getParameters();
        camera.release();

        final List<Camera.Size> supportedPreviewSizes = params.getSupportedPreviewSizes();
        CharSequence[] entries = new CharSequence[supportedPreviewSizes.size()];
        CharSequence[] entryValues = new CharSequence[supportedPreviewSizes.size()];
        for (int previewSizeIndex = 0; previewSizeIndex < supportedPreviewSizes.size();
             previewSizeIndex++)
        {
            Camera.Size supportedPreviewSize = supportedPreviewSizes.get(previewSizeIndex);
            entries[previewSizeIndex] = supportedPreviewSize.width + "x"
                                        + supportedPreviewSize.height;
            entryValues[previewSizeIndex] = String.valueOf(previewSizeIndex);
        } // for

        sizePreference.setEntries(entries);
        sizePreference.setEntryValues(entryValues);

    } // setSizePreferenceData()

} // class PeepersPreferenceActivity

