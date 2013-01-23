package net.majorkernelpanic.networking;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.Iterator;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;

import android.hardware.Camera.CameraInfo;

/**
 * Parse a URI and configure a Session accordingly
 */
public class UriParser {

	/**
	 * Configure a Session according to the given URI
	 * Here are some examples of URIs that can be used to configure a Session:
	 * <ul><li>rtsp://xxx.xxx.xxx.xxx:8086?h264&flash=on</li>
	 * <li>rtsp://xxx.xxx.xxx.xxx:8086?h263&camera=front&flash=on</li>
	 * <li>rtsp://xxx.xxx.xxx.xxx:8086?h264=200-20-320-240</li>
	 * <li>rtsp://xxx.xxx.xxx.xxx:8086?aac</li></ul>
	 * @param uri The URI
	 * @param session The Session that will be configured
	 * @throws IllegalStateException
	 * @throws IOException
	 */
	public static void parse(String uri, Session session) throws IllegalStateException, IOException {
		int camera = CameraInfo.CAMERA_FACING_BACK;

		List<NameValuePair> params = URLEncodedUtils.parse(URI.create(uri),"UTF-8");
		if (params.size()>0) {

			// Those parameters must be parsed first or else they won't necessarily be taken into account
			for (Iterator<NameValuePair> it = params.iterator();it.hasNext();) {
				NameValuePair param = it.next();

				// CAMERA -> the client can choose between the front facing camera and the back facing camera
				if (param.getName().equals("camera")) {
					if (param.getValue().equals("back")) camera = CameraInfo.CAMERA_FACING_BACK;
					else if (param.getValue().equals("front")) camera = CameraInfo.CAMERA_FACING_FRONT;
				}

				// MULTICAST -> the stream will be sent to a multicast group
				// The default mutlicast address is 228.5.6.7, but the client can specify one
				else if (param.getName().equals("multicast")) {
					session.setRoutingScheme(Session.MULTICAST);
					if (param.getValue()!=null) {
						try {
							InetAddress addr = InetAddress.getByName(param.getValue());
							if (!addr.isMulticastAddress()) {
								throw new IllegalStateException("Invalid multicast address !");
							}
							session.setDestination(addr);
						} catch (UnknownHostException e) {
							throw new IllegalStateException("Invalid multicast address !");
						}
					}
					else {
						// Default multicast address
						session.setDestination(InetAddress.getByName("228.5.6.7"));
					}
				}

				// UNICAST -> the client can use this so specify where he wants the stream to be sent
				else if (param.getName().equals("unicast")) {
					if (param.getValue()!=null) {
						try {
							InetAddress addr = InetAddress.getByName(param.getValue());
							session.setDestination(addr);
						} catch (UnknownHostException e) {
							throw new IllegalStateException("Invalid destination address !");
						}
					}
				}

				// TTL -> the client can modify the time to live of packets
				// By default ttl=64
				else if (param.getName().equals("ttl")) {
					if (param.getValue()!=null) {
						try {
							int ttl = Integer.parseInt(param.getValue());
							if (ttl<0) throw new IllegalStateException("The TTL must be a positive integer !");
							session.setTimeToLive(ttl);
						} catch (Exception e) {
							throw new IllegalStateException("The TTL must be a positive integer !");
						}
					}
				}

				// No tracks will be added to the session
				else if (param.getName().equals("stop")) {
					return;
				}

			}

			for (Iterator<NameValuePair> it = params.iterator();it.hasNext();) {
				NameValuePair param = it.next();
				if (param.getName().equals("h263")) {
					session.addVideoTrack(camera);
				}
			}

			// The default behavior is to only add one video track
			if (session.getTrackCount()==0) {
				session.addVideoTrack();
			}

		}
		// Uri has no parameters: the default behavior is to only add one video track
		else {
			session.addVideoTrack();
		}
	}

}
