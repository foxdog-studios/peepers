package com.foxdogstudios.peepers;

import java.io.InputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

import android.util.Log;

/* package */ final class RtpStreamer extends Object
{
    private static final String TAG = RtpStreamer.class.getSimpleName();

    private final InputStream mVideoStream;

    private Thread mStreamerThread = null;

    private volatile boolean mIsRunning = false;

    /* package */ RtpStreamer(final InputStream videoStream)
    {
        super();
        mVideoStream = videoStream;
    } // constructor(InputStream)

    /* package */ void start()
    {
        if (mIsRunning)
        {
            throw new IllegalStateException("RtpStreamer is already running");
        } // if
        mIsRunning = true;
        mStreamerThread = new Thread()
        {
            @Override
            public void run()
            {
                stream();
            } // run()
        };
        mStreamerThread.start();
    } // start()

    /* package */ void stop()
    {
        if (!mIsRunning)
        {
            throw new IllegalStateException("RtpStreamer is already stopped");
        } // if
        mIsRunning = false;
        mStreamerThread.interrupt();
    } // stop()

    private void stream()
    {
        byte[] buffer = new byte[2048];

        try
        {
            final DatagramSocket socket = new DatagramSocket();
            final DatagramPacket packet = new DatagramPacket(buffer, 3, InetAddress.getByName("kilburn"), 9000);
            int numBytes;
            while ((numBytes = mVideoStream.read(buffer)) != -1)
            {
                packet.setData(buffer, 0, numBytes);
                socket.send(packet);
            }

        } // try
        catch (final IOException e)
        {
            Log.e(TAG, "An exception occured while streaming", e);
        } // catch
    } // stream()


} // class RtpStreamer

