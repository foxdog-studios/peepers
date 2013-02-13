package com.foxdogstudios.peepers;

import java.io.IOException;

public class LocalStreamer
{
    public static void main(final String[] args)
    {
        if (args.length != 2)
        {
            System.err.println("Usage: localstreamer host_name port jpeg width height");
            return;
        } // if

        final String hostName = args[0];
        final int port = Integer.parseInt(args[1]);

        final WebcamStreamer webcamStreamer;
        try
        {
            webcamStreamer = new WebcamStreamer(hostName, port);
        } // try
        catch (IOException e)
        {
            System.err.println("Could not create WebcamStreamer");
            return;
        } // catch

        new Thread(webcamStreamer).start();
    } // main(String[])
} // class LocalStreamer

