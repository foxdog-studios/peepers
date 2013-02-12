package com.foxdogstudios.peepers;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

public class LocalStreamer
{
    public static void main(final String[] args) throws InterruptedException
    {
        if (args.length != 3)
        {
            System.err.println("Usage: localstreamer <jpeg> <width> <height");
            return;
        } // if

        final String fileName = args[0];
        final int jpegWidth = Integer.parseInt(args[1]);
        final int jpegHeight = Integer.parseInt(args[2]);

        final RandomAccessFile jpegFile;
        try
        {
            jpegFile = new RandomAccessFile(args[0], "r");
        } // try
        catch (FileNotFoundException e)
        {
            System.err.println("Could not find file '" + fileName + "'");
            return;
        } // catch

        final int jpegLength;
        try
        {
            jpegLength = (int) jpegFile.length();
        } // try
        catch (IOException e)
        {
            System.err.println("Could not get length of file '" + fileName + "'");
            return;
        } // catch

        final byte[] jpegBuffer = new byte[jpegLength];
        try
        {
            jpegFile.read(jpegBuffer);
        } // try
        catch (IOException e)
        {
            System.err.println("Could not read file '" + fileName + "'");
            return;
        } // catch

        final MJpegRtpStreamer mJpegRtpStreamer;
        try
        {
            mJpegRtpStreamer = new MJpegRtpStreamer();
        } // try
        catch (IOException e)
        {
            System.err.println("Could not create MJpegRtpStreamer()");
            return;
        } // catch

        System.out.println("Beginning MJPEG stream, Ctrl-C to stop");

        while (true)
        {
            final long timeStamp = System.currentTimeMillis();
            try
            {
                mJpegRtpStreamer.sendJpeg(jpegBuffer, jpegLength, jpegWidth, jpegHeight,
                        timeStamp);
            } // try
            catch (IOException e)
            {
                System.err.println("Could not send jpeg");
            } // catch
            Thread.sleep(1000);
        } // while

    } // main(String[])
} // class LocalStreamer

