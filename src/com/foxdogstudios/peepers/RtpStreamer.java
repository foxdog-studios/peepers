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

    private static final String HOST_NAME = "hopper";
    // First unprivileged UDP port
    private static final int HOST_PORT = 1024;

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

// COPY PASTE


	private final static int MAXPACKETSIZE = 1400;
	private Statistics stats = new Statistics();

	public void stream() {
		long time, duration = 0, ts = 0;
		int i = 0, j = 0, tr;
		boolean firstFragment = true;


		// This will skip the MPEG4 header if this step fails we can't stream anything :(
		try {
            initSocket();
            setDestination(InetAddress.getByName(HOST_NAME), HOST_PORT);
			skipHeader();
		} catch (IOException e) {
			Log.e(TAG,"Couldn't skip mp4 header :/");
			return;
		}

		// Each packet we send has a two byte long header (See section 5.1 of RFC 4629)
		buffer[rtphl] = 0;
		buffer[rtphl+1] = 0;

		try {
			while (running) {
				time = SystemClock.elapsedRealtime();
				if (fill(rtphl+j+2,MAXPACKETSIZE-rtphl-j-2)<0) return;
				duration += SystemClock.elapsedRealtime() - time;
				j = 0;
				// Each h263 frame starts with: 0000 0000 0000 0000 1000 00??
				// Here we search where the next frame begins in the bit stream
				for (i=rtphl+2;i<MAXPACKETSIZE-1;i++) {
					if (buffer[i]==0 && buffer[i+1]==0 && (buffer[i+2]&0xFC)==0x80) {
						j=i;
						break;
					}
				}
				// Parse temporal reference
				tr = (buffer[i+2]&0x03)<<6 | (buffer[i+3]&0xFF)>>2;
				//Log.d(TAG,"j: "+j+" buffer: "+printBuffer(rtphl, rtphl+5)+" tr: "+tr);
				if (firstFragment) {
					// This is the first fragment of the frame -> header is set to 0x0400
					buffer[rtphl] = 4;
					firstFragment = false;
				} else {
					buffer[rtphl] = 0;
				}
				if (j>0) {
					// We have found the end of the frame
					stats.push(duration);
					ts+= stats.average(); duration = 0;
					//Log.d(TAG,"End of frame ! duration: "+stats.average());
					// The last fragment of a frame has to be marked
					markNextPacket();
					send(j);
					updateTimestamp(ts*90);
					System.arraycopy(buffer,j+2,buffer,rtphl+2,MAXPACKETSIZE-j-2);
					j = MAXPACKETSIZE-j-2;
					firstFragment = true;
				} else {
					// We have not found the beginning of another frame
					// The whole packet is a fragment of a frame
					send(MAXPACKETSIZE);
				}
			}
		} catch (IOException e) {
			running = false;
			Log.e(TAG,"IOException: "+e.getMessage());
			e.printStackTrace();
		}

		Log.d(TAG,"H263 Packetizer stopped !");

	}

	private int fill(int offset,int length) throws IOException {

		int sum = 0, len;

		while (sum<length) {
			len = mVideoStream.read(buffer, offset+sum, length-sum);
			if (len<0) {
				throw new IOException("End of stream");
			}
			else sum+=len;
		}

		return sum;

	}

	// The InputStream may start with a header that we need to skip
	private void skipHeader() throws IOException {
		// Skip all atoms preceding mdat atom
		while (true) {
			while (mVideoStream.read() != 'm');
			mVideoStream.read(buffer,rtphl,3);
			if (buffer[rtphl] == 'd' && buffer[rtphl+1] == 'a' && buffer[rtphl+2] == 't') break;
		}
	}

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
// AbstractPacketizer

	protected boolean running = true;

    protected static String printBuffer(byte[] buffer, int start,int end) {
    	String str = "";
    	for (int i=start;i<end;i++) str+=","+Integer.toHexString(buffer[i]&0xFF);
    	return str;
    }
// RTP socket
	private MulticastSocket usock;
	private DatagramPacket upack;

	private byte[] buffer = new byte[MTU];
	private int seq = 0;
	private boolean upts = false;
	private int ssrc;
	private int port = -1;

	public static final int RTP_HEADER_LENGTH = 12;
	protected static final int rtphl = RTP_HEADER_LENGTH;
	public static final int MTU = 1500;

	public void initSocket() throws IOException {

		/*							     Version(2)  Padding(0)					 					*/
		/*									 ^		  ^			Extension(0)						*/
		/*									 |		  |				^								*/
		/*									 | --------				|								*/
		/*									 | |---------------------								*/
		/*									 | ||  -----------------------> Source Identifier(0)	*/
		/*									 | ||  |												*/
		buffer[0] = (byte) Integer.parseInt("10000000",2);

		/* Payload Type */
		buffer[1] = (byte) 96;

		/* Byte 2,3        ->  Sequence Number                   */
		/* Byte 4,5,6,7    ->  Timestamp                         */
		/* Byte 8,9,10,11  ->  Sync Source Identifier            */
		setLong((ssrc=(new Random()).nextInt()),8,12);

		usock = new MulticastSocket();
		upack = new DatagramPacket(buffer, 1);

	}

	public void close() {
		usock.close();
	}

	public void setSSRC(int ssrc) {
		this.ssrc = ssrc;
		setLong(ssrc,8,12);
	}

	public int getSSRC() {
		return ssrc;
	}

	public void setTimeToLive(int ttl) throws IOException {
		usock.setTimeToLive(ttl);
	}

	public void setDestination(InetAddress dest, int dport) {
		port = dport;
		upack.setPort(dport);
		upack.setAddress(dest);
	}

	public byte[] getBuffer() {
		return buffer;
	}

	public int getPort() {
		return port;
	}

	public int getLocalPort() {
		return usock.getLocalPort();
	}

	/* Send RTP packet over the network */
	public void send(int length) throws IOException {

		updateSequence();
		upack.setLength(length);
		usock.send(upack);

		if (upts) {
			upts = false;
			buffer[1] -= 0x80;
		}

	}

	private void updateSequence() {
		setLong(++seq, 2, 4);
	}

	public void updateTimestamp(long timestamp) {
		setLong(timestamp, 4, 8);
	}

	public void markNextPacket() {
		upts = true;
		buffer[1] += 0x80; // Mark next packet
	}

	private void setLong(long n, int begin, int end) {
		for (end--; end >= begin; end--) {
			buffer[end] = (byte) (n % 256);
			n >>= 8;
		}
	}

} // class RtpStreamer

