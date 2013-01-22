package net.majorkernelpanic.streaming;

import java.io.IOException;
import java.net.InetAddress;

import net.majorkernelpanic.rtp.AbstractPacketizer;
import android.media.MediaRecorder;
import android.net.LocalServerSocket;
import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import android.util.Log;

/**
 *  A MediaRecorder that streams what it records using a packetizer from the rtp package.
 *  You can't use this class directly !
 */
public abstract class MediaStream extends MediaRecorder implements Stream {

	protected static final String TAG = "MediaStream";

	// If you mode==MODE_DEFAULT the MediaStream will just act as a regular MediaRecorder
	// By default: mode = MODE_STREAMING and MediaStream forwards data to the packetizer
	public static final int MODE_STREAMING = 0;
	public static final int MODE_DEFAULT = 1;

	private static int id = 0;
	private int socketId;
	private LocalServerSocket lss = null;
	private LocalSocket receiver, sender = null;
	protected AbstractPacketizer packetizer = null;
	protected boolean streaming = false, modeDefaultWasUsed = false;
	protected String sdpDescriptor;
	protected int mode = MODE_STREAMING;

	public MediaStream() {
		super();

		try {
			lss = new LocalServerSocket("net.majorkernelpanic.librtp-"+id);
		} catch (IOException e1) {
			//throw new IOException("Can't create local socket !");
		}
		socketId = id;
		id++;

	}

	/** The stream will be sent to the address specified by this function **/
	public void setDestination(InetAddress dest, int dport) {
		this.packetizer.setDestination(dest, dport);
	}

	/** Set the Time To Live of the underlying RtpSocket
	 * @throws IOException **/
	public void setTimeToLive(int ttl) throws IOException {
		this.packetizer.setTimeToLive(ttl);
	}

	public int getDestinationPort() {
		return this.packetizer.getRtpSocket().getPort();
	}

	public int getLocalPort() {
		return this.packetizer.getRtpSocket().getLocalPort();
	}

	public void setMode(int mode) throws IllegalStateException {
		if (!streaming) {
			this.mode = mode;
			if (mode == MODE_DEFAULT) modeDefaultWasUsed = true;
		}
		else {
			throw new IllegalStateException("You can't call setMode() while streaming !");
		}
	}

	public AbstractPacketizer getPacketizer() {
		return packetizer;
	}

	public boolean isStreaming() {
		return streaming;
	}

	public void prepare() throws IllegalStateException,IOException {
		if (mode==MODE_STREAMING) {
			createSockets();
			// We write the ouput of the camera in a local socket instead of a file !
			// This one little trick makes streaming feasible quiet simply: data from the camera
			// can then be manipulated at the other end of the socket
			setOutputFile(sender.getFileDescriptor());
		}
		super.prepare();
	}

	public void start() throws IllegalStateException {
		super.start();
		try {
			if (mode==MODE_STREAMING) {
				// receiver.getInputStream contains the data from the camera
				// the packetizer encapsulates this stream in an RTP stream and send it over the network
				packetizer.setInputStream(receiver.getInputStream());
				packetizer.start();
			}
			streaming = true;
		} catch (IOException e) {
			super.stop();
			throw new IllegalStateException("Something happened with the local sockets :/ Start failed");
		} catch (NullPointerException e) {
			super.stop();
			throw new IllegalStateException("setPacketizer() should be called before start(). Start failed");
		}
	}

	public void stop() {
		if (streaming) {
			try {
				super.stop();
			}
			catch (Exception ignore) {}
			finally {
				super.reset();
				closeSockets();
				if (mode==MODE_STREAMING) packetizer.stop();
				streaming = false;
			}
		}
	}

	public int getSSRC() {
		return getPacketizer().getRtpSocket().getSSRC();
	}

	public abstract String generateSessionDescriptor()  throws IllegalStateException, IOException;

	private void createSockets() throws IOException {
		receiver = new LocalSocket();
		receiver.connect( new LocalSocketAddress("net.majorkernelpanic.librtp-" + socketId ) );
		receiver.setReceiveBufferSize(500000);
		sender = lss.accept();
		sender.setSendBufferSize(500000);
	}

	private void closeSockets() {
		try {
			sender.close();
			receiver.close();
		} catch (Exception ignore) {}
	}

	public void release() {
		stop();
		try {
			lss.close();
		}
		catch (Exception ignore) {}
		super.release();
	}

}
