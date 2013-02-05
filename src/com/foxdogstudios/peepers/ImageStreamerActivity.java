package com.foxdogstudios.peepers;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public final class ImageStreamerActivity extends Activity implements SurfaceHolder.Callback
{
    private static final String TAG = ImageStreamerActivity.class.getSimpleName();

    private boolean mPreviewDisplayCreated = false;
    private boolean mRunning = false;
    private SurfaceHolder mPreviewDisplay = null;
    private ImageStreamer mImageStreamer = null;

    public ImageStreamerActivity()
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
        tryStartImageStreamer();
    } // onResume()

    @Override
    protected void onPause()
    {
        super.onPause();
        mRunning = false;
        ensureImageStreamerStopped();
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
        tryStartImageStreamer();
    } // surfaceCreated(SurfaceHolder)

    @Override
    public void surfaceDestroyed(final SurfaceHolder holder)
    {
        mPreviewDisplayCreated = false;
        ensureImageStreamerStopped();
    } // surfaceDestroyed(SurfaceHolder)

    private void tryStartImageStreamer()
    {
        if (mRunning && mPreviewDisplayCreated)
        {
            mImageStreamer = new ImageStreamer(mPreviewDisplay);
            mImageStreamer.start();
        } // if
    } // tryStartImageStreamer()

    private void ensureImageStreamerStopped()
    {
        if (mImageStreamer != null)
        {
            mImageStreamer.stop();
            mImageStreamer = null;
        } // if
    } // stopImageStreamer()


} // class ImageStreamerActivity

