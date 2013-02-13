package com.foxdogstudios.peepers;

import java.io.IOException;

public class LocalStreamer
{
    public static void main(final String[] args)
    {
        final WebcamStreamer webcamStreamer;
        try
        {
            webcamStreamer = new WebcamStreamer();
        } // try
        catch (IOException e)
        {
            System.err.println("Could not create GrabberShow");
            return;
        } // catch

        new Thread(webcamStreamer).start();

    } // main(String[])
} // class LocalStreamer

