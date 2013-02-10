package com.foxdogstudios.peepers;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Random;

import android.util.Log;

/* package */ final class MJpegRtpStreamer
{
    private static final String TAG = MJpegRtpStreamer.class.getSimpleName();

    private static final int MTU = 15000;
    private static final int RTP_HEADER_SIZE = 12;
    private static final int JPG_HEADER_SIZE = 8;

    private final byte[] mData = new byte[MTU];
    private final DatagramSocket mSocket;
    private final DatagramPacket mPacket;

    private int mSequenceNumber = 0;

    /* package */ MJpegRtpStreamer() throws IOException
    {
        super();
        final InetAddress address = InetAddress.getByName("hopper");
        mSocket = new DatagramSocket();
        mPacket = new DatagramPacket(mData, 0 /* length */);
        mPacket.setAddress(address);
        mPacket.setPort(1024);

        Random rng = new Random();
        // Version 2
        // Padding 0
        // Extras  0
        // CC      0
        mData[0] = (byte) 0x80;

        // sequence number
        mSequenceNumber = rng.nextInt(Integer.MAX_VALUE);
        setData(mSequenceNumber, 2, 4);

        // SSRC
        setData(rng.nextInt(), 8, 12);

        mData[12] = 2;
        // jpeg type YUV 420
        mData[16] = 1;
    } // constructor()

    /* package */ void close()
    {
        mSocket.close();
    } // close()

    /* package */ void sendJpeg(final byte[] jpeg, final int length, final int width,
            final int height, final long timestamp) throws IOException
    {
        // mark          0
        // payload type 26
        mData[1] = 26;

        // 90kHz timestamp
        setData(timestamp * 90, 5, 8);

        int jpegOffset = 0;
        mData[18] = (byte) (width / 8);
        mData[19] = (byte) (height / 8);


        while (jpegOffset < length)
        {
            setData(jpegOffset, 13, 16);

            int dataEnd = RTP_HEADER_SIZE + JPG_HEADER_SIZE;

            int roomInBuffer = MTU - dataEnd;
            int bytesRemaining = length - jpegOffset;
            int copyLength;
            if (bytesRemaining < roomInBuffer)
            {
                copyLength = bytesRemaining;
                mData[1] += 128;
            } // if
            else
            {
                copyLength = roomInBuffer;
            } // else

            System.arraycopy(jpeg, jpegOffset, mData, dataEnd, copyLength);
            jpegOffset += copyLength;
            dataEnd += copyLength;

            mPacket.setLength(dataEnd);
            mSocket.send(mPacket);

            updateSequenceNumber();
        } // while

    } // sendJpeg(byte[], int, int, long)

    private void updateSequenceNumber()
    {
        mSequenceNumber++;
        setData(mSequenceNumber, 2, 4);
    } // updateSequenceNumber()

    private void setData(long bytes, final int start, int end)
    {
        for (end--; end >= start; end--)
        {
            mData[end] = (byte)(bytes % 256);
            bytes >>= 8;
        } // for
    } // setData(long, int, int)


} // class MJpegRtpStreamer

