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

    private static final int H263_HEADER_OFFSET = 12;
    private static final int H263_FRAME_START_LENGTH = 2;
    private static final int H263_PAYLOAD_OFFSET = H263_HEADER_OFFSET + H263_FRAME_START_LENGTH;

	private static final int MAXPACKETSIZE = 1400;
	private static final int RTP_HEADER_LENGTH = 12;
	private static final int rtphl = RTP_HEADER_LENGTH;
	private static final int MTU = 1500;

    private final InputStream mVideoStream;
    private final byte[] mBuffer = new byte[MTU];
    private int mBufferEnd = 0;
    private int mSequenceNumber = Integer.MIN_VALUE;
    private Thread mStreamerThread = null;
	private MulticastSocket mSocket;
	private DatagramPacket mPacket;

	private int seq = 0;
	private boolean upts = false;
	private int ssrc;

	private Statistics stats = new Statistics();
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
		setBuffer((ssrc=(new Random()).nextInt()),8,12);

        // H263+ Header
		// Each packet we send has a two byte long header (See section 5.1 of RFC 4629)
		mBuffer[rtphl] = 0;
		mBuffer[rtphl+1] = 0;

		mSocket = new MulticastSocket();
		mPacket = new DatagramPacket(mBuffer, 1);
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
		long time;
        long duration = 0;
        long ts = 0;
		int i = 0;
        int j = 0;
		boolean firstFragment = true;

        while (mIsRunning)
        {
            time = SystemClock.elapsedRealtime();
            fill(rtphl + j + 2, MAXPACKETSIZE - rtphl - j - 2);
            duration += SystemClock.elapsedRealtime() - time;
            j = 0;
            // Each h263 frame starts with: 0000 0000 0000 0000 1000 00??
            // Here we search where the next frame begins in the bit stream
            for (i=rtphl+2;i<MAXPACKETSIZE-1;i++) {
                if (mBuffer[i]==0 && mBuffer[i+1]==0 && (mBuffer[i+2]&0xFC)==0x80) {
                    j=i;
                    break;
                }
            }
            if (firstFragment) {
                // This is the first fragment of the frame -> header is set to 0x0400
                mBuffer[rtphl] = 4;
                firstFragment = false;
            } else {
                mBuffer[rtphl] = 0;
            }
            if (j>0) {
                // We have found the end of the frame
                stats.push(duration);
                ts+= stats.average(); duration = 0;
                //Log.d(TAG,"End of frame ! duration: "+stats.average());
                // The last fragment of a frame has to be marked
                markNextPacket();
                send(j);
                setBuffer(ts * 90, 4, 8); // Update timestamp
                System.arraycopy(mBuffer,j+2,mBuffer,rtphl+2,MAXPACKETSIZE-j-2);
                j = MAXPACKETSIZE-j-2;
                firstFragment = true;
            } else {
                // We have not found the beginning of another frame
                // The whole packet is a fragment of a frame
                send(MAXPACKETSIZE);
            }
        }

	}

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
		setBuffer(++seq, 2, 4);
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

