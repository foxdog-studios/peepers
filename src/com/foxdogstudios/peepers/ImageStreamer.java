package com.foxdogstudios.peepers;

import java.io.IOException;

import android.hardware.Camera;
import android.util.Log;
import android.view.SurfaceHolder;

/* package */ final class ImageStreamer extends Object
{
    private static final String TAG = ImageStreamer.class.getSimpleName();

    private final Object mLock = new Object();
    private final SurfaceHolder mPreviewDisplay;

    private boolean mRunning = false;
    private Thread mWorker = null;
    private Camera mCamera = null;

    /* package */ ImageStreamer(final SurfaceHolder previewDisplay)
    {
        super();

        if (previewDisplay == null)
        {
            throw new IllegalArgumentException("previewDisplay must not be null");
        } // if

        mPreviewDisplay = previewDisplay;
    } // constructor(SurfaceHolder)

    /* package */ void start()
    {
        synchronized (mLock)
        {
            if (mRunning)
            {
                throw new IllegalStateException("ImageStreamer is already running");
            } // if
            mRunning = true;
        } // synchronized

        mWorker = new Thread()
        {
            @Override
            public void run()
            {
                try
                {
                    ImageStreamer.this.run();
                } // try
                catch (final Exception streamingException)
                {
                    Log.w(TAG, "An exception occured while streaming", streamingException);
                } // catch
            } // run()
        };
        mWorker.start();
    } // start()

    /* package */ void stop()
    {
        synchronized (mLock)
        {
            if (!mRunning)
            {
                throw new IllegalStateException("ImageStreamer is already stopped");
            } // if

            mRunning = false;
            if (mCamera != null)
            {
                mCamera.release();
                mCamera = null;
            } // if
        } // synchronized
    } // stop()

    private void run() throws IOException
    {
        // Throws RuntimeException if the camera is currently opened
        // by another application.
        final Camera camera = Camera.open();

        // Wait for the preview display
        synchronized (mLock)
        {
            if (!mRunning)
            {
                camera.release();
                return;
            } // if

            try
            {
                camera.setPreviewDisplay(mPreviewDisplay);
            } // try
            catch (final IOException e)
            {
                camera.release();
                throw e;
            } // catch

            camera.startPreview();
            mCamera = camera;
        } // synchronized
    } // run()


} // class ImageStreamer

