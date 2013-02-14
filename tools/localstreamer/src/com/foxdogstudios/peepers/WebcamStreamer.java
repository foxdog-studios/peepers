package com.foxdogstudios.peepers;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

import static com.googlecode.javacv.cpp.opencv_core.cvFlip;
import static com.googlecode.javacv.cpp.opencv_highgui.cvSaveImage;

import com.googlecode.javacv.CanvasFrame;
import com.googlecode.javacv.FrameGrabber;
import com.googlecode.javacv.OpenCVFrameGrabber;
import com.googlecode.javacv.cpp.opencv_core.IplImage;

public class WebcamStreamer implements Runnable
{
    private static final String CAPTURE_FILE = System.getProperty("java.io.tmpdir")
            + "/capture.jpg";

    IplImage image;
    CanvasFrame canvas = new CanvasFrame("Web Cam");
    final MJpegRtpStreamer mJpegRtpStreamer;
    final HttpStreamer httpStreamer;
    boolean mUseHttp;

    public WebcamStreamer(final String hostName, final int port, final boolean http)
        throws IOException
    {
        canvas.setDefaultCloseOperation(javax.swing.JFrame.EXIT_ON_CLOSE);
        mJpegRtpStreamer = new MJpegRtpStreamer(hostName, port);
        httpStreamer = new HttpStreamer();
        mUseHttp = http;
    } // constructor

    @Override
    public void run()
    {
        FrameGrabber grabber = new OpenCVFrameGrabber("");
        try
        {
            grabber.start();
        } // try
        catch (com.googlecode.javacv.FrameGrabber.Exception e)
        {
            System.err.println("Could not start the webcam grabber");
            return;
        } //catch

        if (mUseHttp)
        {
            try
            {
                System.out.println("Waiting for connection");
                httpStreamer.acceptConnection();
            } // try
            catch (IOException e)
            {
                System.err.println("Could not accept HTTP connection");
                return;
            } // catch
        } // if

        IplImage img;
        while (true)
        {
            /* We grab a frame from the camera and save
             * it to a file in jpeg format. We then read this file to
             * a buffer which we send using the jpeg streamer.
             */
            try
            {
                img = grabber.grab();
            } // try
            catch (com.googlecode.javacv.FrameGrabber.Exception e)
            {
                System.err.println("Could not get a video frame from the webcam");
                return;
            } // catch
            if (img == null)
            {
                continue;
            } // if

            // 90 degrees rotation anti clockwise
            cvFlip(img, img, 1);
            cvSaveImage(CAPTURE_FILE, img);

            RandomAccessFile jpegFile = null;
            final int jpegLength;
            byte[] jpegBuffer = null;
            try
            {
                jpegFile = new RandomAccessFile(CAPTURE_FILE, "r");
                jpegLength = (int) jpegFile.length();
                jpegBuffer = new byte[jpegLength];
                jpegFile.read(jpegBuffer);
            } // try
            catch (FileNotFoundException e)
            {
                System.err.println("Could not find file '" + CAPTURE_FILE + "'");
                return;
            } // catch
            catch (IOException e)
            {
                System.err.println("Could not read file '" + CAPTURE_FILE + "'");
                return;
            } // catch
            finally
            {
                if (jpegFile != null)
                {
                    try
                    {
                        jpegFile.close();
                    } // try
                    catch (IOException e)
                    {
                        System.err.println("Could not close jpeg file");
                    } // catch
                } // if
            } // finally

            try
            {
                long timestamp = System.currentTimeMillis();
                if (mUseHttp)
                {
                    httpStreamer.writeJpeg(jpegBuffer, jpegLength, timestamp);
                } // if
                else
                {
                    mJpegRtpStreamer.sendJpeg(jpegBuffer, jpegLength, img.width(), img.height(),
                            timestamp);
                } // else
            } // try
            catch (IOException e)
            {
                System.err.println("Could not send jpeg through socket");
                return;
            } // catch

            canvas.showImage(img);
        } // while
    } // run
}

