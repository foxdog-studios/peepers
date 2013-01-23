package net.majorkernelpanic.streaming.video;

import java.io.IOException;

import net.majorkernelpanic.rtp.H263Packetizer;
import net.majorkernelpanic.streaming.MediaStream;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.Parameters;
import android.media.MediaRecorder;
import android.util.Log;
import android.view.SurfaceView;
import android.view.SurfaceHolder;

public final class VideoStream extends MediaStream {
    private static final String TAG = "VideoStream";

    private SurfaceHolder.Callback surfaceHolderCallback = null;
    private int videoEncoder;
    private int cameraId;
    private Camera camera;

    /**
     * Don't use this class directly
     * @param cameraId Can be either CameraInfo.CAMERA_FACING_BACK or CameraInfo.CAMERA_FACING_FRONT
     */
    public VideoStream(int camera) throws IOException {
        super();
        setVideoEncoder(MediaRecorder.VideoEncoder.H263);
        this.packetizer = new H263Packetizer();

        CameraInfo cameraInfo = new CameraInfo();
        int numberOfCameras = Camera.getNumberOfCameras();
        for (int i=0;i<numberOfCameras;i++) {
            Camera.getCameraInfo(i, cameraInfo);
            if (cameraInfo.facing == camera) {
                this.cameraId = i;
                break;
            }
        }
    }

    /**
     * Modifies the videoEncoder of the stream. You can call this method at any time
     * and changes will take effect next time you call prepare()
     * @param videoEncoder Encoder of the stream
     */
    public void setVideoEncoder(int videoEncoder) {
        this.videoEncoder = videoEncoder;
    }

    /**
     * Stops streaming
     */
    public synchronized void stop() {
        super.stop();
        if (camera != null) {
            try {
                camera.reconnect();
            } catch (Exception e) {
                Log.e(TAG,e.getMessage()!=null?e.getMessage():"unknown error");
            }
            camera.stopPreview();
            try {
                camera.release();
            } catch (Exception e) {
                Log.e(TAG,e.getMessage()!=null?e.getMessage():"unknown error");
            }
            camera = null;
        }
    }

    /**
     * Prepare the VideoStream, you can then call start()
     * The underlying Camera will be opened and configured whaen you call this method so don't forget to deal with the RuntimeExceptions !
     * Camera.open, Camera.setParameter, Camera.unlock may throw one !
     */
    public void prepare() throws IllegalStateException, IOException, RuntimeException {
        if (camera == null) {
            camera = Camera.open(cameraId);
            camera.setErrorCallback(new Camera.ErrorCallback() {
                @Override
                public void onError(int error, Camera camera) {
                    // On some phones when trying to use the camera facing front the media server will die
                    // Whether or not this callback may be called really depends on the phone
                    if (error == Camera.CAMERA_ERROR_SERVER_DIED) {
                        // In this case the application must release the camera and instantiate a new one
                        Log.e(TAG,"Media server died !");
                        // We don't know in what thread we are so stop needs to be synchronized
                        stop();
                    }
                }
            });
        }

        // If an exception is thrown after the camera was open, we must absolutly release it !
        try {
            camera.setDisplayOrientation(90);
            camera.unlock();
            super.setCamera(camera);

            // MediaRecorder should have been like this according to me:
            // all configuration methods can be called at any time and
            // changes take effects when prepare() is called
            super.setVideoSource(MediaRecorder.VideoSource.CAMERA);
            super.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            if (mode==MODE_DEFAULT) {
                super.setMaxDuration(1000);
                super.setMaxFileSize(Integer.MAX_VALUE);
                // On some phones a RuntimeException might be thrown :/
                try {
                    super.setMaxDuration(0);
                    super.setMaxFileSize(Integer.MAX_VALUE);
                } catch (RuntimeException e) {
                    Log.e(TAG,"setMaxDuration or setMaxFileSize failed !");
                }
            }

            super.setVideoEncoder(videoEncoder);
            super.setVideoSize(640, 480);
            super.setVideoFrameRate(15);
            super.setVideoEncodingBitRate(500000);

            super.prepare();

        } catch (IOException e) {
            camera.release();
            camera = null;
            throw e;
        }

    }

    public String generateSessionDescriptor() throws IllegalStateException, IOException {
        return "m=video "+String.valueOf(getDestinationPort())+" RTP/AVP 96\r\n" +
                   "b=RR:0\r\n" +
                   "a=rtpmap:96 H263-1998/90000\r\n";

    }

    public void release() {
        stop();
        super.release();
    }

}
