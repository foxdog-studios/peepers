package com.foxdogstudios.peepers;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

/* package */ final class MJpegRtpStreamer
{
    private static final int MTU = 1500;

    final byte[] mData = new byte[MTU];
    final DatagramSocket mSocket;
    final DatagramPacket mPacket;

    /* package */ MJpegRtpStreamer() throws IOException
    {
        super();
        final InetAddress address = InetAddress.getByName("kilburn");
        mSocket = new DatagramSocket();
        mPacket = new DatagramPacket(mData, 0 /* length */);
        mPacket.setAddress(address);
        mPacket.setPort(1024);
    } // constructor()

    /* package */ void close()
    {
        mSocket.close();
    } // close()

    /* package */ void sendJpeg(final byte[] jpeg, final int width, final int height,
            final long timestamp)
    {

    } // sendJpeg(byte[], int, int, long)


} // class MJpegRtpStreamer

