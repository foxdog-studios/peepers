package net.majorkernelpanic.spydroid;

import java.io.IOException;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import net.majorkernelpanic.networking.RtspServer;

public final class SpydroidActivity extends Activity
{
    private static final String TAG = SpydroidActivity.class.getSimpleName();

    private RtspServer mRtspServer = null;
    private Surface mPreviewDisplay = null;

    public void onCreate(final Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        final SurfaceHolder holder = ((SurfaceView) findViewById(R.id.camera)).getHolder();
        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        mPreviewDisplay = holder.getSurface();
    } // onCreate(Bundle)

    public void onResume()
    {
        super.onResume();
        try
        {
            mRtspServer = new RtspServer(8086, mPreviewDisplay);
            mRtspServer.start();
        } //  try
        catch (final IOException e)
        {
            Log.e(TAG, "Cannot start RTSP server", e);
            mRtspServer = null;
        } // catch
    } // onResume()

    public void onPause()
    {
        if (mRtspServer != null)
        {
            mRtspServer.stop();
            mRtspServer = null;
        } // if
        super.onPause();
    } // onPause()

} // class SpydroidActivity

