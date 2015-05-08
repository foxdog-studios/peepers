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

        // Camera preference
        final ListPreference cameraPreference = (ListPreference) findPreference("camera");

        setCameraPreferences(cameraPreference);

        cameraPreference.setOnPreferenceClickListener(new OnPreferenceClickListener(){
            @Override
            public boolean onPreferenceClick(Preference preference) {
                setCameraPreferences(cameraPreference);
                return false;
            }
        });

        // JPEG size preference
        final ListPreference sizePreference = (ListPreference) findPreference("size");

        setSizePreferences(sizePreference, cameraPreference);

        sizePreference.setOnPreferenceClickListener(new OnPreferenceClickListener(){
            @Override
            public boolean onPreferenceClick(Preference preference) {
                setSizePreferences(sizePreference, cameraPreference);
                return false;
            }
        });
    } // onCreate(Bundle)

    private void setCameraPreferences(final ListPreference cameraPreference)
    {
        final int numberOfCameras = Camera.getNumberOfCameras();
        final CharSequence[] entries = new CharSequence[numberOfCameras];
        final CharSequence[] entryValues = new CharSequence[numberOfCameras];
        final Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        for (int cameraIndex = 0; cameraIndex < numberOfCameras; cameraIndex++)
        {
            Camera.getCameraInfo(cameraIndex, cameraInfo);
            String cameraFacing;
            switch (cameraInfo.facing)
            {
                case Camera.CameraInfo.CAMERA_FACING_BACK:
                    cameraFacing = "back";
                    break;
                case Camera.CameraInfo.CAMERA_FACING_FRONT:
                    cameraFacing = "front";
                    break;
                default:
                    cameraFacing = "unknown";
            } // switch

            entries[cameraIndex] = "Camara " + cameraIndex + " " + cameraFacing;
            entryValues[cameraIndex] = String.valueOf(cameraIndex);
        } //for

        cameraPreference.setEntries(entries);
        cameraPreference.setEntryValues(entryValues);

    } // setCameraPreferences(ListPreference)

    private void setSizePreferences(final ListPreference sizePreference,
                                    final ListPreference cameraPreference)
    {
        final String cameraPreferenceValue = cameraPreference.getValue();
        final int cameraIndex;
        if (cameraPreferenceValue != null)
        {
            cameraIndex = Integer.parseInt(cameraPreferenceValue);
        } // if
        else
        {
            cameraIndex = 0;
        } // else
        final Camera camera = Camera.open(cameraIndex);
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

    } // setSizePreferenceData(ListPreference)

} // class PeepersPreferenceActivity

