package com.foxdogstudios.peepers;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public final class StreamCameraActivity extends Activity implements SurfaceHolder.Callback
{
    private static final String TAG = StreamCameraActivity.class.getSimpleName();

    private boolean mPreviewDisplayCreated = false;
    private boolean mRunning = false;
    private SurfaceHolder mPreviewDisplay = null;
    private CameraRtpStreamer mCameraRtpStreamer = null;

    public StreamCameraActivity()
    {
        super();
    } // constructor()

    @Override
    protected void onCreate(final Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

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
        if (mRunning && mPreviewDisplayCreated)
        {
            mCameraRtpStreamer = new CameraRtpStreamer(mPreviewDisplay);
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


} // class StreamCameraActivity

