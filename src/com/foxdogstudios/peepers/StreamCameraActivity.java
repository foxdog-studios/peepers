package com.foxdogstudios.peepers;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public final class StreamCameraActivity extends Activity implements SurfaceHolder.Callback
{
    private static final String TAG = StreamCameraActivity.class.getSimpleName();

    private boolean mPreviewDisplayCreated = false;
    private boolean mRunning = false;
    private SurfaceHolder mPreviewDisplay = null;
    private CameraRtpStreamer mCameraRtpStreamer = null;
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
    } // onCreate(Bundle)

    @Override
    protected void onResume()
    {
        super.onResume();
        mRunning = true;
        tryStartCameraRtpStreamer();
    } // onResume()

    @Override
    protected void onPause()
    {
        super.onPause();
        mRunning = false;
        ensureCameraRtpStreamerStopped();
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
        tryStartCameraRtpStreamer();
    } // surfaceCreated(SurfaceHolder)

    @Override
    public void surfaceDestroyed(final SurfaceHolder holder)
    {
        mPreviewDisplayCreated = false;
        ensureCameraRtpStreamerStopped();
    } // surfaceDestroyed(SurfaceHolder)

    private void tryStartCameraRtpStreamer()
    {
        if (mRunning && mPreviewDisplayCreated && mPrefs != null)
        {
            final Preferences prefs = new Preferences(this);
            mCameraRtpStreamer = new CameraRtpStreamer(prefs.getHostName(), prefs.getPort(),
                    prefs.getJpegQuality(), mPreviewDisplay);
            mCameraRtpStreamer.start();
        } // if
    } // tryStartCameraRtpStreamer()

    private void ensureCameraRtpStreamerStopped()
    {
        if (mCameraRtpStreamer != null)
        {
            mCameraRtpStreamer.stop();
            mCameraRtpStreamer = null;
        } // if
    } // stopCameraRtpStreamer()

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
            tryStartCameraRtpStreamer();
        } // onPostExecute(Void)


    } // class LoadPreferencesTask


} // class StreamCameraActivity

