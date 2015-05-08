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

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.preference.PreferenceManager;
import android.text.format.Formatter;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.TextView;

import org.apache.http.conn.util.InetAddressUtils;

public final class StreamCameraActivity extends Activity
        implements SurfaceHolder.Callback
{
    private static final String TAG = StreamCameraActivity.class.getSimpleName();

    private static final String WAKE_LOCK_TAG = "peepers";

    private static final String PREF_CAMERA = "camera";
    private static final int PREF_CAMERA_INDEX_DEF = 0;
    private static final String PREF_FLASH_LIGHT = "flash_light";
    private static final boolean PREF_FLASH_LIGHT_DEF = false;
    private static final String PREF_PORT = "port";
    private static final int PREF_PORT_DEF = 8080;
    private static final String PREF_JPEG_SIZE = "size";
    private static final String PREF_JPEG_QUALITY = "jpeg_quality";
    private static final int PREF_JPEG_QUALITY_DEF = 40;
    // preview sizes will always have at least one element, so this is safe
    private static final int PREF_PREVIEW_SIZE_INDEX_DEF = 0;

    private boolean mRunning = false;
    private boolean mPreviewDisplayCreated = false;
    private SurfaceHolder mPreviewDisplay = null;
    private CameraStreamer mCameraStreamer = null;

    private String mIpAddress = "";
    private int mCameraIndex = PREF_CAMERA_INDEX_DEF;
    private boolean mUseFlashLight = PREF_FLASH_LIGHT_DEF;
    private int mPort = PREF_PORT_DEF;
    private int mJpegQuality = PREF_JPEG_QUALITY_DEF;
    private int mPrevieSizeIndex = PREF_PREVIEW_SIZE_INDEX_DEF;
    private TextView mIpAddressView = null;
    private LoadPreferencesTask mLoadPreferencesTask = null;
    private SharedPreferences mPrefs = null;
    private MenuItem mSettingsMenuItem = null;
    private WakeLock mWakeLock = null;

    public StreamCameraActivity()
    {
        super();
    } // constructor()

    @Override
    protected void onCreate(final Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        new LoadPreferencesTask().execute();

        mPreviewDisplay = ((SurfaceView) findViewById(R.id.camera)).getHolder();
        mPreviewDisplay.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        mPreviewDisplay.addCallback(this);

        mIpAddress = tryGetIpV4Address();
        mIpAddressView = (TextView) findViewById(R.id.ip_address);
        updatePrefCacheAndUi();

        final PowerManager powerManager =
                (PowerManager) getSystemService(POWER_SERVICE);
        mWakeLock = powerManager.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK,
                WAKE_LOCK_TAG);
    } // onCreate(Bundle)

    @Override
    protected void onResume()
    {
        super.onResume();
        mRunning = true;
        if (mPrefs != null)
        {
            mPrefs.registerOnSharedPreferenceChangeListener(
                    mSharedPreferenceListener);
        } // if
        updatePrefCacheAndUi();
        tryStartCameraStreamer();
        mWakeLock.acquire();
    } // onResume()

    @Override
    protected void onPause()
    {
        mWakeLock.release();
        super.onPause();
        mRunning = false;
        if (mPrefs != null)
        {
            mPrefs.unregisterOnSharedPreferenceChangeListener(
                    mSharedPreferenceListener);
        } // if
        ensureCameraStreamerStopped();
    } // onPause()

    @Override
    public void surfaceChanged(final SurfaceHolder holder, final int format,
            final int width, final int height)
    {
        // Ingored
    } // surfaceChanged(SurfaceHolder, int, int, int)

    @Override
    public void surfaceCreated(final SurfaceHolder holder)
    {
        mPreviewDisplayCreated = true;
        tryStartCameraStreamer();
    } // surfaceCreated(SurfaceHolder)

    @Override
    public void surfaceDestroyed(final SurfaceHolder holder)
    {
        mPreviewDisplayCreated = false;
        ensureCameraStreamerStopped();
    } // surfaceDestroyed(SurfaceHolder)

    private void tryStartCameraStreamer()
    {
        if (mRunning && mPreviewDisplayCreated && mPrefs != null)
        {
            mCameraStreamer = new CameraStreamer(mCameraIndex, mUseFlashLight, mPort,
                    mPrevieSizeIndex, mJpegQuality, mPreviewDisplay);
            mCameraStreamer.start();
        } // if
    } // tryStartCameraStreamer()

    private void ensureCameraStreamerStopped()
    {
        if (mCameraStreamer != null)
        {
            mCameraStreamer.stop();
            mCameraStreamer = null;
        } // if
    } // stopCameraStreamer()

    @Override
    public boolean onCreateOptionsMenu(final Menu menu)
    {
        super.onCreateOptionsMenu(menu);
        mSettingsMenuItem = menu.add(R.string.settings);
        mSettingsMenuItem.setIcon(android.R.drawable.ic_menu_manage);
        return true;
    } // onCreateOptionsMenu(Menu)

    @Override
    public boolean onOptionsItemSelected(final MenuItem item)
    {
        if (item != mSettingsMenuItem)
        {
            return super.onOptionsItemSelected(item);
        } // if
        startActivity(new Intent(this, PeepersPreferenceActivity.class));
        return true;
    } // onOptionsItemSelected(MenuItem)

    private final class LoadPreferencesTask
            extends AsyncTask<Void, Void, SharedPreferences>
    {
        private LoadPreferencesTask()
        {
            super();
        } // constructor()

        @Override
        protected SharedPreferences doInBackground(final Void... noParams)
        {
            return PreferenceManager.getDefaultSharedPreferences(
                    StreamCameraActivity.this);
        } // doInBackground()

        @Override
        protected void onPostExecute(final SharedPreferences prefs)
        {
            StreamCameraActivity.this.mPrefs = prefs;
            prefs.registerOnSharedPreferenceChangeListener(
                    mSharedPreferenceListener);
            updatePrefCacheAndUi();
            tryStartCameraStreamer();
        } // onPostExecute(SharedPreferences)


    } // class LoadPreferencesTask

    private final OnSharedPreferenceChangeListener mSharedPreferenceListener =
            new OnSharedPreferenceChangeListener()
    {
        @Override
        public void onSharedPreferenceChanged(final SharedPreferences prefs,
                final String key)
        {
            updatePrefCacheAndUi();
        } // onSharedPreferenceChanged(SharedPreferences, String)

    }; // mSharedPreferencesListener

    private final int getPrefInt(final String key, final int defValue)
    {
        // We can't just call getInt because the preference activity
        // saves everything as a string.
        try
        {
            return Integer.parseInt(mPrefs.getString(key, null /* defValue */));
        } // try
        catch (final NullPointerException e)
        {
            return defValue;
        } // catch
        catch (final NumberFormatException e)
        {
            return defValue;
        } // catch
    } // getPrefInt(String, int)

    private final void updatePrefCacheAndUi()
    {
        mCameraIndex = getPrefInt(PREF_CAMERA, PREF_CAMERA_INDEX_DEF);
        if (hasFlashLight())
        {
            if (mPrefs != null)
            {
                mUseFlashLight = mPrefs.getBoolean(PREF_FLASH_LIGHT,
                        PREF_FLASH_LIGHT_DEF);
            } // if
            else
            {
                mUseFlashLight = PREF_FLASH_LIGHT_DEF;
            } // else
        } //if
        else
        {
            mUseFlashLight = false;
        } // else

        // XXX: This validation should really be in the preferences activity.
        mPort = getPrefInt(PREF_PORT, PREF_PORT_DEF);
        // The port must be in the range [1024 65535]
        if (mPort < 1024)
        {
            mPort = 1024;
        } // if
        else if (mPort > 65535)
        {
            mPort = 65535;
        } // else if

        mPrevieSizeIndex = getPrefInt(PREF_JPEG_SIZE, PREF_PREVIEW_SIZE_INDEX_DEF);
        mJpegQuality = getPrefInt(PREF_JPEG_QUALITY, PREF_JPEG_QUALITY_DEF);
        // The JPEG quality must be in the range [0 100]
        if (mJpegQuality < 0)
        {
            mJpegQuality = 0;
        } // if
        else if (mJpegQuality > 100)
        {
            mJpegQuality = 100;
        } // else if
        mIpAddressView.setText("http://" + mIpAddress + ":" + mPort + "/");
    } // updatePrefCacheAndUi()

    private boolean hasFlashLight()
    {
        return getPackageManager().hasSystemFeature(
                PackageManager.FEATURE_CAMERA_FLASH);
    } // hasFlashLight()

    private static String tryGetIpV4Address()
    {
        try
        {
            final Enumeration<NetworkInterface> en =
                    NetworkInterface.getNetworkInterfaces();
            while (en.hasMoreElements())
            {
                final NetworkInterface intf = en.nextElement();
                final Enumeration<InetAddress> enumIpAddr =
                        intf.getInetAddresses();
                while (enumIpAddr.hasMoreElements())
                {
                    final  InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress())
                    {
                        final String addr = inetAddress.getHostAddress().toUpperCase();
                        if (InetAddressUtils.isIPv4Address(addr))
                        {
                            return addr;
                        }
                    } // if
                } // while
            } // for
        } // try
        catch (final Exception e)
        {
            // Ignore
        } // catch
        return null;
    } // tryGetIpV4Address()

} // class StreamCameraActivity

