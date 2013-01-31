package com.foxdogstudios.peepers;

import java.io.InputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.Arrays;
import java.util.Random;

import android.os.SystemClock;
import android.util.Log;

/* package */ final class RtpStreamer extends Object
{
    private static final String TAG = RtpStreamer.class.getSimpleName();

    private static final String HOST_NAME = "kilburn";
    // First unprivileged UDP port
    private static final int HOST_PORT = 1024;

	private static final int MTU = 1400;
	private static final int RTP_HEADER_LENGTH = 12;
    private static final int H263_HEADER_OFFSET = RTP_HEADER_LENGTH;
    private static final int H263_HEADER_LENGTH = 2;
    private static final int H263_PAYLOAD_OFFSET = H263_HEADER_OFFSET + H263_HEADER_LENGTH;

    private final byte[] mBuffer = new byte[MTU];
    private final InputStream mVideoStream;
    private Thread mStreamerThread = null;
	private MulticastSocket mSocket = null;
	private DatagramPacket mPacket = null;
    private int mSequenceNumber = Integer.MIN_VALUE;

    // XXX: What's going on here?
    private static final int MAX_Q = 50;
    private double mMeanPictureDuration = 0.0;
    private int mQ = 0;

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
                try
                {
                    stream();
                } // try
                catch (final IOException e)
                {
                    Log.w(TAG, "An exception occured while streaming", e);
                } // catch
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

	private void stream() throws IOException
    {
		mSocket = new MulticastSocket();
		mPacket = new DatagramPacket(mBuffer, 0 /* length */);
        mPacket.setAddress(InetAddress.getByName(HOST_NAME));
        mPacket.setPort(HOST_PORT);

		mBuffer[0] = (byte) 0x80;

		// RTP payload type: dyanamic
		mBuffer[1] = (byte) 96;

        final Random random = new Random();

		// Initial sequence number (bytes 2 and 3 of the RTP header),
        // set by send().
        mSequenceNumber = random.nextInt();

		// Initial timestamp (bytes 4, 5, 6 and 7 of the RTP header),
        // updated by streamLoop() on picture start.
        setBuffer(0L, 4, 8);

		// Sync source identifier (bytes 8, 9, 10 and 11 of the RTP
        // header).
		setBuffer(random.nextInt(), 8, 12);

        // H263+ header (see section 5.1 of RFC 4629)
		mBuffer[H263_HEADER_OFFSET] = 0x00;
		mBuffer[H263_HEADER_OFFSET + 1] = 0x00;

        skipToMdatData();
        streamLoop();
	} // stream()

	private void setBuffer(long bytes, final int start, int end)
    {
		for (end--; end >= start; end--)
        {
			mBuffer[end] = (byte) (bytes % 256);
			bytes >>= 8;
		} // for
	} // setBuffer(long, int, int)

	private void skipToMdatData() throws IOException
    {
        final byte[] mdat = { 'm', 'd', 'a', 't' };
        final byte[] buffer = new byte[mdat.length];

		do
        {
            fillBuffer(buffer, 0 /* offset */);
		} while (!Arrays.equals(buffer, mdat));
	} // skipToMdatData()

    private void fillBuffer(final byte[] buffer, final int offset) throws IOException
    {
        int length = buffer.length - offset;
        while (length > 0)
        {
            final int bytesRead = mVideoStream.read(buffer, buffer.length - length, length);
            if (bytesRead == -1)
            {
                throw new IOException("Video stream ended");
            } // if
            length -= bytesRead;
        } // while
    } // fillBuffer(byte[], int)

	private void streamLoop() throws IOException
    {
		boolean isPictureStart = true;
        int payloadLength = 0;
        long pictureDuration = 0L;
        long timestamp = 0L;

        while (mIsRunning)
        {
            if (isPictureStart)
            {
                // Set the P bit in the H263+ payload header
                mBuffer[H263_HEADER_OFFSET] = 0x04;
                isPictureStart = false;
            } // if
            else
            {
                // Clear the P bit
                mBuffer[H263_HEADER_OFFSET] = 0x00;
            } // else

            // Fill the H263+ payload
            final long beforeFill = SystemClock.elapsedRealtime();
            payloadLength = fillH263Payload(payloadLength);
            pictureDuration += SystemClock.elapsedRealtime() - beforeFill;

            final int pictureStart = findPictureStartInH263Payload();
            if (pictureStart != -1)
            {
                // As the packet contains the end of a picture, set the
                // Mark bit in the RTP header and then clear it after
                // the packet has been sent.
		        mBuffer[1] += 0x80;
                send(pictureStart);
			    mBuffer[1] -= 0x80;

                payloadLength = shiftPictureStartToH263PayloadOffset(pictureStart);
                isPictureStart = true;

                // The next packet starts a new picture, so update the
                // timestamp.
                // XXX: What's going on here?
                updateMeanPictureDuration(pictureDuration);
                pictureDuration = 0L;
                timestamp += mMeanPictureDuration;
                setBuffer(timestamp * 90, 4, 8);

            } // if
            else
            {
                send(MTU);
                payloadLength = 0;
            } // else
        } // while
	} // streamLoop()

	private int fillH263Payload(final int payloadLength) throws IOException
    {
        fillBuffer(mBuffer, H263_PAYLOAD_OFFSET + payloadLength);
        return MTU - H263_PAYLOAD_OFFSET;
	} // fillH263Payload(int)

    private int findPictureStartInH263Payload()
    {
        // H263+ pictures start with 0000 0000 0000 0000 1000 00??
        for (int i = H263_PAYLOAD_OFFSET; i < MTU - 2; i++)
        {
            if (mBuffer[i] == 0 && mBuffer[i + 1] == 0 && (mBuffer[i + 2] & 0xfc) == 0x80)
            {
                return i;
            } // if
        } // for
        return -1;
    } // findPictureStartInH263Payload()

    // XXX: What's going on here?
    private void updateMeanPictureDuration(final long duration)
    {
        mMeanPictureDuration = (mMeanPictureDuration * mQ + duration) / (mQ + 1);
        if (mQ < MAX_Q)
        {
            mQ++;
        } // if
    } // updateMeanPictureDuration(duraiton)

	private void send(final int length) throws IOException
    {
		setBuffer(mSequenceNumber++, 2, 4);
		mPacket.setLength(length);
		mSocket.send(mPacket);
	} // send(int)

    private int shiftPictureStartToH263PayloadOffset(final int pictureStart)
    {
        final int IMPLIED_PICTURE_START_BYTES = 2;
        final int srcPos = pictureStart + IMPLIED_PICTURE_START_BYTES;
        // After the copy, length is also the payloadLength;
        final int length = MTU - pictureStart - IMPLIED_PICTURE_START_BYTES;
        System.arraycopy(mBuffer, srcPos, mBuffer, H263_PAYLOAD_OFFSET, length);
        return length;
    } // shiftPictureStartToH263PayloadOffset(int)


} // class RtpStreamer

