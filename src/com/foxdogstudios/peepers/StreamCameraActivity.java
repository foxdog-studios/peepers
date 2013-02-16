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
import java.util.List;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.TextView;

import org.apache.http.conn.util.InetAddressUtils;

public final class StreamCameraActivity extends Activity implements SurfaceHolder.Callback
{
    private static final String TAG = StreamCameraActivity.class.getSimpleName();

    private boolean mPreviewDisplayCreated = false;
    private boolean mRunning = false;
    private SurfaceHolder mPreviewDisplay = null;
    private CameraStreamer mCameraStreamer = null;
    private TextView mIpAddress = null;
    private Preferences mPrefs = null;
    private MenuItem mSettingsMenuItem = null;

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

        mIpAddress = (TextView) findViewById(R.id.ip_address);
        mIpAddress.setText(tryGetIpAddress());
    } // onCreate(Bundle)

    @Override
    protected void onResume()
    {
        super.onResume();
        mRunning = true;
        tryStartCameraStreamer();
    } // onResume()

    @Override
    protected void onPause()
    {
        super.onPause();
        mRunning = false;
        ensureCameraStreamerStopped();
    } // onPause()

    @Override
    public void surfaceChanged(final SurfaceHolder holder, final int format, final int width,
            final int height)
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
            final Preferences prefs = new Preferences(this);
            mCameraStreamer = new CameraStreamer(mPrefs.getPort(), prefs.getJpegQuality(),
                    mPreviewDisplay);
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

    private final class LoadPreferencesTask extends AsyncTask<Void, Void, Preferences>
    {
        private LoadPreferencesTask()
        {
            super();
        } // constructor()

        @Override
        protected Preferences doInBackground(Void... noParams)
        {
            return new Preferences(StreamCameraActivity.this);
        } // doInBackground()

        @Override
        protected void onPostExecute(final Preferences prefs)
        {
            StreamCameraActivity.this.mPrefs = prefs;
            tryStartCameraStreamer();
        } // onPostExecute(Void)


    } // class LoadPreferencesTask

    /**
     *  Try to get the IP address of this device. Base on code from
     *  http://stackoverflow.com/a/13007325
     *
     *  @return the first IP address of the device, or null
     */
    private static String tryGetIpAddress()
    {
        try
        {
            final List<NetworkInterface> interfaces =
                    Collections.list(NetworkInterface.getNetworkInterfaces());
            for (final NetworkInterface intf : interfaces)
            {
                final List<InetAddress> addrs = Collections.list(intf.getInetAddresses());
                for (final InetAddress addr : addrs)
                {
                    if (!addr.isLoopbackAddress())
                    {
                        final String sAddr = addr.getHostAddress().toUpperCase();
                        if (InetAddressUtils.isIPv4Address(sAddr))
                        {
                            return sAddr;
                        } // if
                        else
                        {
                            // Drop IP6 port suffix
                            final int delim = sAddr.indexOf('%');
                            return delim < 0 ? sAddr : sAddr.substring(0, delim);
                        } // else
                    } // if
                } // for
            } // for
        } // try
        catch (final Exception e)
        {
            // Ignore
        } // for now eat exceptions
        return null;
    } // tryGetIpAddress()


} // class StreamCameraActivity

