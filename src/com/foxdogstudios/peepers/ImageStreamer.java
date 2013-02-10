package com.foxdogstudios.peepers;

import java.io.IOException;
import java.io.OutputStream;

import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Handler;
import android.os.Message;
import android.os.Process;
import android.os.SystemClock;
import android.util.Log;
import android.view.SurfaceHolder;

/* package */ final class ImageStreamer extends Object
{
    private static final String TAG = ImageStreamer.class.getSimpleName();

    private static final int MESSAGE_TRY_START_STREAMING = 0;
    private static final int MESSAGE_SEND_PREVIEW_FRAME = 1;

    private static final long OPEN_CAMERA_POLL_INTERVAL_MS = 1000L;

    private final Object mLock = new Object();
    private final SurfaceHolder mPreviewDisplay;

    private boolean mRunning = false;
    private Looper mLooper = null;
    private Handler mWorkHandler = null;
    private Camera mCamera = null;
    private int mPreviewFormat = Integer.MIN_VALUE;
    private int mPreviewWidth = Integer.MIN_VALUE;
    private int mPreviewHeight = Integer.MIN_VALUE;
    private Rect mPreviewRect = null;
    private int mPreviewBufferSize = Integer.MIN_VALUE;
    private MemoryOutputStream mJpegOutputStream = null;
    private MJpegRtpStreamer mMJpegRtpStreamer = null;

    // Frame rate data
    private long mNumFrames = 0L;
    private long mDuration = 0L;
    private long mLastTimestamp = Long.MIN_VALUE;

    /* package */ ImageStreamer(final SurfaceHolder previewDisplay)
    {
        super();

        if (previewDisplay == null)
        {
            throw new IllegalArgumentException("previewDisplay must not be null");
        } // if

        mPreviewDisplay = previewDisplay;
    } // constructor(SurfaceHolder)

    private final class WorkHandler extends Handler
    {
        private WorkHandler(final Looper looper)
        {
            super(looper);
        } // constructor(Looper)

        @Override
        public void handleMessage(final Message message)
        {
            switch (message.what)
            {
                case MESSAGE_TRY_START_STREAMING:
                    tryStartStreaming();
                    break;
                case MESSAGE_SEND_PREVIEW_FRAME:
                    final Object[] args = (Object[]) message.obj;
                    sendPreviewFrame((byte[]) args[0], (Camera) args[1], (Long) args[2]);
                    break;
                default:
                    throw new IllegalArgumentException("cannot handle message");
            } // switch
        } // handleMessage(Message)
    } // class WorkHandler

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

        final HandlerThread worker = new HandlerThread(TAG, Process.THREAD_PRIORITY_MORE_FAVORABLE);
        worker.setDaemon(true);
        worker.start();
        mLooper = worker.getLooper();
        mWorkHandler = new WorkHandler(mLooper);
        mWorkHandler.obtainMessage(MESSAGE_TRY_START_STREAMING).sendToTarget();
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
            if (mMJpegRtpStreamer != null)
            {
                mMJpegRtpStreamer.close();
            } // if
            if (mCamera != null)
            {
                mCamera.release();
                mCamera = null;
            } // if
        } // synchronized
        mLooper.quit();
    } // stop()

    private void tryStartStreaming()
    {
        try
        {
            while (true)
            {
                try
                {
                    startStreamingIfRunning();
                } //try
                catch (final RuntimeException openCameraFailed)
                {
                    Log.d(TAG, "Open camera failed, retying in " + OPEN_CAMERA_POLL_INTERVAL_MS
                            + "ms", openCameraFailed);
                    Thread.sleep(OPEN_CAMERA_POLL_INTERVAL_MS);
                    continue;
                } // catch
               break;
            } // while
        } // try
        catch (final Exception startPreviewFailed)
        {
            // Captures the IOException from startStreamingIfRunning and
            // the InterruptException from Thread.sleep.
            Log.w(TAG, "Failed to start camera preview", startPreviewFailed);
        } // catch
    } // tryStartStreaming()

    private void startStreamingIfRunning() throws IOException
    {
        final MJpegRtpStreamer streamer = new MJpegRtpStreamer();

        // Throws RuntimeException if the camera is currently opened
        // by another application.
        final Camera camera = Camera.open();

        // Set up callback buffer
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
        mPreviewBufferSize = mPreviewWidth * mPreviewHeight * bytesPerPixel * 3 / 2 + 1;
        camera.addCallbackBuffer(new byte[mPreviewBufferSize]);
        mPreviewRect = new Rect(0, 0, mPreviewWidth, mPreviewHeight);
        camera.setPreviewCallbackWithBuffer(mPreviewCallback);

        // We assumed that the compressed image will be no bigger than
        // the uncompressed image.
        mJpegOutputStream = new MemoryOutputStream(mPreviewBufferSize);

        synchronized (mLock)
        {
            if (!mRunning)
            {
                streamer.close();
                camera.release();
                return;
            } // if

            mMJpegRtpStreamer = streamer;

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
    } // startStreamingIfRunning()

    private final Camera.PreviewCallback mPreviewCallback = new Camera.PreviewCallback()
    {
        @Override
        public void onPreviewFrame(final byte[] data, final Camera camera)
        {
            final Long timestamp = SystemClock.elapsedRealtime();
            final Message message = mWorkHandler.obtainMessage();
            message.what = MESSAGE_SEND_PREVIEW_FRAME;
            message.obj = new Object[]{ data, camera, timestamp };
            message.sendToTarget();
        } // onPreviewFrame(byte[], Camera)
    }; // mPreviewCallback

   private void sendPreviewFrame(final byte[] data, final Camera camera, final long timestamp)
   {
        // Calculate and log the number of preview fames frames per
        // second
        mNumFrames++;
        final long MILLI_PER_SECOND = 1000L;
        final long timestampSeconds = timestamp / MILLI_PER_SECOND;
        final long LOGS_PER_FRAME = 10L;
        if (mLastTimestamp != Long.MIN_VALUE)
        {
            mDuration += timestampSeconds- mLastTimestamp;
            if (mNumFrames % LOGS_PER_FRAME == LOGS_PER_FRAME - 1)
            {
                Log.d(TAG, "FPS: " + (mNumFrames / (double)mDuration));
            } // if
        } // else
        mLastTimestamp = timestampSeconds;

        // Create JPEG
        final YuvImage image = new YuvImage(data, mPreviewFormat, mPreviewWidth, mPreviewHeight,
                null /* strides */);
        image.compressToJpeg(mPreviewRect, 100 /* quality */, mJpegOutputStream);

        try
        {
            mMJpegRtpStreamer.sendJpeg(mJpegOutputStream.getBuffer(), mJpegOutputStream.getLength(),
                    mPreviewWidth, mPreviewHeight, timestamp);
        } // try
        catch (final IOException e)
        {
            Log.w(TAG, "Could not send jpeg", e);
        } // catch

        // Clean up
        mJpegOutputStream.seek(0);
        // XXX: I believe that because we're not calling methods in
        // another thread we're using Camera in a safe way. I might
        // be wrong, the documentation is not clear.
        camera.addCallbackBuffer(data);
   } // sendPreviewFrame(byte[], camera, long)

} // class ImageStreamer

