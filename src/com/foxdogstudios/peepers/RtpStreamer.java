package com.foxdogstudios.peepers;

import java.io.InputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.Random;

import android.os.SystemClock;
import android.util.Log;

/* package */ final class RtpStreamer extends Object
{
    private static final String TAG = RtpStreamer.class.getSimpleName();

    private static final String HOST_NAME = "hopper";
    // First unprivileged UDP port
    private static final int HOST_PORT = 1024;

    private static final int MTU = 5000;

    private static final int H263_HEADER_OFFSET = 12;
    private static final int H263_FRAME_START_LENGTH = 2;
    private static final int H263_PAYLOAD_OFFSET = H263_HEADER_OFFSET + H263_FRAME_START_LENGTH;

    private final InputStream mVideoStream;
    private final byte[] mBuffer = new byte[MTU];
    private int mBufferEnd = 0;
    private DatagramSocket mSocket = null;
    private DatagramPacket mPacket = null;
    private int mSequenceNumber = Integer.MIN_VALUE;
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
        initialiseBuffer();
        final InetAddress host;

        try
        {
            mSocket = new DatagramSocket();
            host = InetAddress.getByName(HOST_NAME);
        } // try
        catch (final IOException e)
        {
            Log.e(TAG, "Could not set up networking", e);
            return;
        } // catch

        mPacket = new DatagramPacket(mBuffer, MTU, host, HOST_PORT);

        try
        {
            skipToMdatBoxData();
            streamLoop();
        } // try
        catch (final IOException e)
        {
            Log.e(TAG, "Exception while streaming", e);
        } // catch
    } // stream()

    private void skipToMdatBoxData() throws IOException
    {
        // We assume that the 'mdat' box is the last box.
        // See http://atomicparsley.sourceforge.net/mpeg-4files.html
        final byte[] mdatBoxName = { 109, 100, 97, 116 };
        final int nameLength = mdatBoxName.length;
        final byte[] buffer = new byte[nameLength];

        do
        {
            int totalBytesRead = 0;
            while (totalBytesRead != nameLength)
            {
                final int bytesRead = mVideoStream.read(buffer, totalBytesRead,
                        nameLength - totalBytesRead);
                if (bytesRead == -1)
                {
                    throw new IOException("Stream ended before mdat box was found");
                } // if
                totalBytesRead += bytesRead;
            } // while
        } while (!Arrays.equals(buffer, mdatBoxName));
    } // skipToMdatBoxData()

    private void initialiseBuffer()
    {
        // Byte 0
        // Bit pattern 1000 0000
        // Sets the version to be 2 with the padding and extension bits unset and the
        // Contributing source (CSRC) count as zero.
        mBuffer[0] = (byte) 0x80;

        // Byte 1
        // Bit pattern 0110 0000
        // The Marker bit is unset and the payload type is set to 96 which is defined
        // as dynamic (see http://tools.ietf.org/html/rfc3551#page-34).
        mBuffer[1] = 0x60;

        final Random random = new Random();

        // Bytes 2, 3
        // Sequence number, set in streamLoop()
        // It does not matter if this is negative as long as you only
        // perform addition.
        mSequenceNumber = random.nextInt();

        // Bytes 4, 5, 6, 7
        // Timestamp is set to zero
        setBuffer(0, 4, 8);

        // Bytes 8, 9, 10, 11
        // Synchronization source (SSRC) identifier is set to a random value.
        setBuffer(random.nextInt(), 8, 12);

        mBufferEnd = H263_PAYLOAD_OFFSET;

        for (int i = mBufferEnd; i < MTU; i++)
        {
            mBuffer[i] = 0;
        } // for
    } // initialiseBuffer()

    private void setBuffer(long bytes, final int start, int end)
    {
        for (end--; end >= start; end--)
        {
            mBuffer[end] = (byte) (bytes % 256);
            bytes >>= 8;
        } // for
    } // setBuffer(long, int, int)

    private void streamLoop() throws IOException
    {
        boolean firstFragment = true;
        long duration = 0L;

        long ts=0;

        while (mIsRunning)
        {
            if (firstFragment)
            {
                // Set the P bit indicating the packet contains a
                // picture start.
                mBuffer[H263_HEADER_OFFSET] = 0x04;
                firstFragment = false;
            } // if
            else
            {
                mBuffer[H263_HEADER_OFFSET] = 0x00;
            } // else

            final long timeBeforeFill = SystemClock.elapsedRealtime();
            fillBuffer();
            duration += SystemClock.elapsedRealtime() - timeBeforeFill;

            final int frameStart = findFrameStart();
            if (frameStart != -1)
            {
                push(duration);
                ts += average();
                duration=0;

                setMarkBit();
                send(frameStart);
                shiftFragmentToRtpPayloadStart(frameStart);

                // Set the timestamp of the *NEXT* packet
                setBuffer(ts* 90, 4, 8);
                firstFragment = true;
            } // if
            else
            {
                clearMarkBit();
                send(MTU);
                mBufferEnd = H263_PAYLOAD_OFFSET;
            } // else
        } // while
    } // streamLoop()

    private void fillBuffer() throws IOException
    {
        while (mBufferEnd < MTU)
        {
            final int bytesRead = mVideoStream.read(mBuffer, mBufferEnd, MTU - mBufferEnd);
            if (bytesRead == -1)
            {
                throw new IOException("Reached end of video stream while filling buffer");
            } // if
            mBufferEnd += bytesRead;
        } // while
    } // fillBuffer()

    private int findFrameStart()
    {
        final int FRAME_HEADER_LENGTH = 2;

        for (int i = H263_PAYLOAD_OFFSET; i < MTU - FRAME_HEADER_LENGTH; i++)
        {
            if (mBuffer[i] == 0x00 && mBuffer[i + 1] == 0x00 && (mBuffer[i + 2] & 0xfc) == 0x80)
            {
                return i;
            } // if
        } // for

        return -1;
    } // findFrameStart()

    private void setMarkBit()
    {
        mBuffer[1] += 0x80;
    } // setMarkBit()

    private void clearMarkBit()
    {
        mBuffer[1] -= 0x80;
    } // clearMarkBit()

    private void send(final int length) throws IOException
    {
        setBuffer(mSequenceNumber++, 2, 4);
        mPacket.setLength(length);
        mSocket.send(mPacket);
    } // send(int)

    private void shiftFragmentToRtpPayloadStart(final int fragmentStart)
    {
        final int fragmentLength = MTU - fragmentStart;
        System.arraycopy(mBuffer, fragmentStart, mBuffer, H263_HEADER_OFFSET, fragmentLength);
        mBufferEnd = H263_HEADER_OFFSET + fragmentLength;
    } // shiftFragmentToRtpPayloadStart(int)


    private float m = 0, q = 0;
    private void push(long d){m=(m*q+d)/(q+1);if(q<50)q++;}
    private long average(){return(long)m;}

} // class RtpStreamer

