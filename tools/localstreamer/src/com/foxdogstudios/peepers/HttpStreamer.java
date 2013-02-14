package com.foxdogstudios.peepers;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class HttpStreamer extends Object
{
    private static final String boundary = "--gc0p4Jq0M2Yt08jU534c0p--";

    ServerSocket mServer = null;
    Socket mSocket = null;
    DataOutputStream mStream = null;

    public HttpStreamer()
    {
        super();
    } // constructor

    public void acceptConnection() throws IOException
    {
        ServerSocket mServer = new ServerSocket(8080);
        mSocket = mServer.accept();
        mServer.close();
        mStream = new DataOutputStream(mSocket.getOutputStream());
        mStream.write(("HTTP/1.0 200 OK\r\n" +
                          "Server: iRecon\r\n" +
                          "Connection: close\r\n" +
                          "Max-Age: 0\r\n" +
                          "Expires: 0\r\n" +
                          "Cache-Control: no-store, no-cache, must-revalidate, pre-check=0, " +
                          "post-check=0, max-age=0\r\n" +
                          "Pragma: no-cache\r\n" +
                          "Content-Type: multipart/x-mixed-replace; " +
                          "boundary=" + boundary + "\r\n" +
                          "\r\n" +
                          "--" + boundary + "\r\n").getBytes());

        mStream.flush();
    } // acceptConnection()

    public void writeJpeg(byte[] bytes, int length, long timestamp) throws IOException
    {
        mStream.write(("Content-type: image/jpeg\r\n" +
                              "Content-Length: " + length + "\r\n" +
                              "X-Timestamp:" + timestamp + "\r\n" +
                              "\r\n").getBytes());

        mStream.write(bytes, 0, length);
        mStream.write(("\r\n--" + boundary + "\r\n").getBytes());
    } // writeJpeg(byte[], int, long)
}
