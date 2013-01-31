package com.foxdogstudios.peepers;

import java.io.InputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
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
	private final Statistics stats = new Statistics();

    private int mBufferEnd = 0;
    private Thread mStreamerThread = null;
	private MulticastSocket mSocket = null;
	private DatagramPacket mPacket = null;
    private int mSequenceNumber = Integer.MIN_VALUE;
	private boolean upts = false;

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
		mBuffer[0] = (byte) 0x80;

		// Payload Type
		mBuffer[1] = (byte) 96;

		// Byte 2,3        ->  Sequence Number
		// Byte 4,5,6,7    ->  Timestamp
		// Byte 8,9,10,11  ->  Sync Source Identifier
		setBuffer(new Random().nextInt(), 8, 12);

        // H263+ Header
		// Each packet we send has a two byte long header (See section 5.1 of RFC 4629)
		mBuffer[H263_HEADER_OFFSET] = 0x00;
		mBuffer[H263_HEADER_OFFSET + 1] = 0x00;

		mSocket = new MulticastSocket();
		mPacket = new DatagramPacket(mBuffer, 0);
        mPacket.setAddress(InetAddress.getByName(HOST_NAME));
        mPacket.setPort(HOST_PORT);

        skipToMdatData();
        streamLoop();
	} // stream()

	// The InputStream may start with a header that we need to skip
	private void skipToMdatData() throws IOException
    {
        final byte[] buffer = new byte[3];
        // XXX: This is flakely
		// Skip all atoms preceding mdat atom
		while (true) {
			while (mVideoStream.read() != 'm');
			mVideoStream.read(buffer);
			if (buffer[0] == 'd' && buffer[1] == 'a' && buffer[2] == 't') break;
		}
	} // skipToMdatData()

	private void streamLoop() throws IOException
    {
        long duration = 0;
        long timestamp = 0;
		boolean firstFragment = true;

        int payloadLength = 0;

        while (mIsRunning)
        {
            // Fill the RTP payload
            final long beforeFill = SystemClock.elapsedRealtime();
            fill(H263_PAYLOAD_OFFSET + payloadLength, MTU - H263_PAYLOAD_OFFSET - payloadLength);
            duration += SystemClock.elapsedRealtime() - beforeFill;


            if (firstFragment)
            {
                // This is the first fragment of the frame -> header is set to 0x0400
                mBuffer[RTP_HEADER_LENGTH] = 0x04;
                firstFragment = false;
            }
            else
            {
                mBuffer[RTP_HEADER_LENGTH] = 0x00;
            }

            final int pictureStart = findPictureStartInH263Payload();

            if (pictureStart != -1)
            {
                // We have found the end of the frame
                stats.push(duration);
                timestamp += stats.average();
                duration = 0;
                // The last fragment of a frame has to be marked
                markNextPacket();
                send(pictureStart);
                setBuffer(timestamp * 90, 4, 8); // Update timestamp
                System.arraycopy(mBuffer, pictureStart + 2, mBuffer, H263_PAYLOAD_OFFSET, MTU - pictureStart - 2);
                payloadLength = MTU - pictureStart - H263_HEADER_LENGTH;
                firstFragment = true;
            } // if
            else
            {
                // We have not found the beginning of another frame
                // The whole packet is a fragment of a frame
                send(MTU);
                payloadLength = 0;
            } // else
        } // while
	} // streamLoop()

	private void fill(final int offset, final int length) throws IOException
    {
		int totalBytesRead = 0;
		while (totalBytesRead < length)
        {
			final int bytesRead = mVideoStream.read(mBuffer, offset + totalBytesRead,
                    length - totalBytesRead);
			if (bytesRead == -1)
            {
				throw new IOException("Video stream ended");
			} // if
			totalBytesRead += bytesRead;
		} // while
	} // fill(int, int)

    private int findPictureStartInH263Payload()
    {
        // Each h263 frame starts with: 0000 0000 0000 0000 1000 00??
        // Here we search where the next frame begins in the bit stream
        for (int i = H263_PAYLOAD_OFFSET; i < MTU - 2; i++)
        {
            if (mBuffer[i] == 0 && mBuffer[i + 1] == 0 && (mBuffer[i + 2] & 0xfc) == 0x80)
            {
                return i;
            } // if
        } // for
        return -1;
    } // findPictureStartInH263Payload()

	private static class Statistics {

		public final static int COUNT=50;
		private float m = 0, q = 0;

		public void push(long duration) {
			m = (m*q+duration)/(q+1);
			if (q<COUNT) q++;
		}

		public long average() {
			return (long)m;
		}

	}

	private void send(final int length) throws IOException
    {
		setBuffer(++mSequenceNumber, 2, 4);
		mPacket.setLength(length);
		mSocket.send(mPacket);

		if (upts)
        {
			upts = false;
			mBuffer[1] -= 0x80;
		}
	} // send(int)

	private void markNextPacket() {
		upts = true;
		mBuffer[1] += 0x80; // Mark next packet
	}

	private void setBuffer(long bytes, final int start, int end)
    {
		for (end--; end >= start; end--)
        {
			mBuffer[end] = (byte) (bytes % 256);
			bytes >>= 8;
		} // for
	} // setBuffer(long, int, int)


} // class RtpStreamer

