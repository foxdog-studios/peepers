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

public final class WebcamStreamer
{
    private static final int PORT = 8080;
    // Assume that webcam will be no larger than 1 MiB
    private static final int BUFFER_SIZE = 1048576;
    private static final String CAPTURE_FILE = "/tmp/peepers.jpg";

    private WebcamStreamer()
    {
        super();
    } // constructor()

    private void stream()
    {
        final FrameGrabber grabber = new OpenCVFrameGrabber(0 /* deviceNum */);

        final MJpegHttpStreamer streamer = new MJpegHttpStreamer(PORT, BUFFER_SIZE);
        streamer.start();

        final CanvasFrame canvas = new CanvasFrame("Webcam");
        canvas.setDefaultCloseOperation(javax.swing.JFrame.EXIT_ON_CLOSE);

        try
        {
            grabber.start();

            while (true)
            {
                final IplImage frame = tryStreamFrame(grabber, streamer);
                if (frame != null)
                {
                    canvas.showImage(frame);
                } // if
            } // while
        } // try
        catch (final com.googlecode.javacv.FrameGrabber.Exception e)
        {
            System.err.println("Could not start frame grabber");
        } // catch
        finally
        {
            streamer.stop();

            try
            {
                grabber.stop();
            } // try
            catch(final com.googlecode.javacv.FrameGrabber.Exception e)
            {
                System.err.println("Could not stop frame grabber");
            } // catch
        } // finally
    } // stream()

    private IplImage tryStreamFrame(final FrameGrabber grabber, final MJpegHttpStreamer streamer)
    {
        IplImage frame = null;

        try
        {
            frame = grabber.grab();
        } // try
        catch (com.googlecode.javacv.FrameGrabber.Exception e)
        {
            System.err.println("Could not get frame from webcam");
            return null;
        } // catch

        final long timestamp = System.currentTimeMillis();

        // 90 degrees rotation anti clockwise
        cvFlip(frame, frame, 1);
        cvSaveImage(CAPTURE_FILE, frame);

        RandomAccessFile jpegFile = null;
        int jpegLength = Integer.MIN_VALUE;
        byte[] jpegBuffer = null;
        boolean error = false;

        try
        {
            jpegFile = new RandomAccessFile(CAPTURE_FILE, "r");
            jpegLength = (int) jpegFile.length();
            jpegBuffer = new byte[jpegLength];
            jpegFile.read(jpegBuffer);
        } // try
        catch (final FileNotFoundException e)
        {
            System.err.println("Could not find file '" + CAPTURE_FILE + "'");
            error = true;
        } // catch
        catch (final IOException e)
        {
            System.err.println("Could not read file '" + CAPTURE_FILE + "'");
            error = true;
        } // catch
        finally
        {
            if (jpegFile != null)
            {
                try
                {
                    jpegFile.close();
                } // try
                catch (final IOException e)
                {
                    System.err.println("Could not close file '" + CAPTURE_FILE + "'");
                } // catch
            } // if
        } // finally

        if (!error)
        {
            streamer.streamJpeg(jpegBuffer, jpegLength, timestamp);
        } // if

        return frame;
    } // tryStreamFrame()

    public static void main(final String[] args)
    {
        final WebcamStreamer webcamStreamer = new WebcamStreamer();
        webcamStreamer.stream();
    } // main(String[])


} //  class WebcamStreamer

