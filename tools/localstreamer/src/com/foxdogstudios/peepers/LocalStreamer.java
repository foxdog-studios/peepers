package com.foxdogstudios.peepers;

import java.io.IOException;

public class LocalStreamer
{
    public static void main(final String[] args)
    {
        String hostName = null;
        int port = 0;
        final boolean useHttp = args.length != 2;

        if (!useHttp)
        {
            hostName = args[0];
            port = Integer.parseInt(args[1]);
        }

        final WebcamStreamer webcamStreamer;
        try
        {
            webcamStreamer = new WebcamStreamer(hostName, port, useHttp);
        } // try
        catch (IOException e)
        {
            System.err.println("Could not create WebcamStreamer");
            return;
        } // catch

        new Thread(webcamStreamer).start();
    } // main(String[])
} // class LocalStreamer

