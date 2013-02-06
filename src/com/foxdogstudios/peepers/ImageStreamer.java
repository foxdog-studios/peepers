package com.foxdogstudios.peepers;

import java.io.IOException;
import java.io.OutputStream;

import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.os.SystemClock;
import android.util.Log;
import android.view.SurfaceHolder;

/* package */ final class ImageStreamer extends Object
{
    private static final String TAG = ImageStreamer.class.getSimpleName();

    private static final long OPEN_CAMERA_POLL_INTERVAL_MS = 1000L;

    private final Object mLock = new Object();
    private final SurfaceHolder mPreviewDisplay;

    private boolean mRunning = false;
    private Thread mWorker = null;
    private Camera mCamera = null;
    private int mPreviewFormat = Integer.MIN_VALUE;
    private int mPreviewWidth = Integer.MIN_VALUE;
    private int mPreviewHeight = Integer.MIN_VALUE;
    private Rect mPreviewRect = null;

    /* package */ ImageStreamer(final SurfaceHolder previewDisplay)
    {
        super();

        if (previewDisplay == null)
        {
            throw new IllegalArgumentException("previewDisplay must not be null");
        } // if

        mPreviewDisplay = previewDisplay;
    } // constructor(SurfaceHolder)

    /*
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
                    ImageStreamer.this.tryStartPreview();
                } // try
                catch (final Exception streamingException)
                {
                    Log.w(TAG, "An exception occured while streaming", streamingException);
                } // catch
            } // run()
        };
        mWorker.start();
    } // start()

    /**
     *  Stop the image streamer. The camera will be released during the
     *  execution of stop() or shortly after it returns. stop() should
     *  be called on the main thread.
     */
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

    private void tryStartPreview() throws Exception
    {
        while (true)
        {
            try
            {
                startPreview();
            } //try
            catch (final RuntimeException openCameraFailed)
            {
                Log.d(TAG, "Open camera failed, retying in " + OPEN_CAMERA_POLL_INTERVAL_MS + "ms",
                        openCameraFailed);
                Thread.sleep(OPEN_CAMERA_POLL_INTERVAL_MS);
                continue;
            } // catch
           break;
        } // while
    } // tryStartPreview()

    private void startPreview() throws IOException
    {
        // Throws RuntimeException if the camera is currently opened
        // by another application.
        final Camera camera = Camera.open();

        // Set up callback buffers
        final Camera.Parameters params = camera.getParameters();
        mPreviewFormat = params.getPreviewFormat();
        final Camera.Size previewSize = params.getPreviewSize();
        mPreviewWidth = previewSize.width;
        mPreviewHeight = previewSize.height;
        final int BITS_PER_BYTE = 8;
        final int bytesPerPixel = ImageFormat.getBitsPerPixel(mPreviewFormat) / BITS_PER_BYTE;
        // XXX: According to the documentation the buffer size can be
        // calculated by width * height * bytesPerPixel. However, this
        // returned an error saying it was too small. It always needed
        // to be exactly 1.5 times larger.
        final int bufferSize = mPreviewWidth * mPreviewHeight * bytesPerPixel * 3 / 2 + 1;
        final int NUM_BUFFERS = 10;
        for (int bufferIndex = 0; bufferIndex < NUM_BUFFERS; bufferIndex++)
        {
            camera.addCallbackBuffer(new byte[bufferSize]);
        } // for
        mPreviewRect = new Rect(0, 0, mPreviewWidth, mPreviewHeight);
        camera.setPreviewCallbackWithBuffer(mPreviewCallback);

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
    } // startPreview()

    private long mNumFrames = 0L;
    private long mDuration = 0L;
    private long mLastTimestamp = Long.MIN_VALUE;

    private final Camera.PreviewCallback mPreviewCallback = new Camera.PreviewCallback()
    {
        @Override
        public void onPreviewFrame(final byte[] data, final Camera camera)
        {
            // Calculate and log the number of preview fames frames per
            // second
            mNumFrames++;
            final long MILLI_PER_SECOND = 1000L;
            final long LOGS_PER_FRAME = 10L;
            final long timestamp = SystemClock.elapsedRealtime() / MILLI_PER_SECOND;
            if (mLastTimestamp != Long.MIN_VALUE)
            {
                mDuration += timestamp - mLastTimestamp;
                if (mNumFrames % LOGS_PER_FRAME == LOGS_PER_FRAME - 1)
                {
                    Log.d(TAG, "FPS: " + (mNumFrames / (double)mDuration));
                } // if
            } // else
            mLastTimestamp = timestamp;

            final YuvImage image = new YuvImage(data, mPreviewFormat, mPreviewWidth, mPreviewHeight,
                    null /* strides */);
            image.compressToJpeg(mPreviewRect, 100 /* quality */, new OutputStream(){
                @Override
                public void write(int oneByte) throws IOException {}
            });

            camera.addCallbackBuffer(data);
        } // onPreviewFrame(byte[], Camera)
    }; // mPreviewCallback

} // class ImageStreamer

