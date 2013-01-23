package net.majorkernelpanic.spydroid;

import java.io.IOException;

import net.majorkernelpanic.networking.RtspServer;
import net.majorkernelpanic.networking.Session;
import net.majorkernelpanic.streaming.video.VideoStream;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public final class SpydroidActivity extends Activity
{
    private static final String TAG = SpydroidActivity.class.getSimpleName();

    /** The HttpServer will use those variables to send reports about the state of the app to the http interface **/
    public static boolean activityPaused = true;
    public static Exception lastCaughtException;

    static private RtspServer rtspServer = null;

    private SurfaceHolder holder;
    private SurfaceView camera;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.main);

        camera = (SurfaceView)findViewById(R.id.smallcameraview);

        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);

        camera.getHolder().setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        holder = camera.getHolder();

        Session.setSurfaceHolder(holder);

        if (rtspServer == null) {
            rtspServer = new RtspServer(8086);
        } // if
    }

    public void onResume() {
        super.onResume();
        activityPaused = true;
        if (rtspServer != null) {
            try {
                rtspServer.start();
            } catch (IOException e) {
                Log.w(TAG, "RtspServer could not be started : "+(e.getMessage()!=null?e.getMessage():"Unknown error"));
            }
        }
    }

    public void onPause() {
        if (rtspServer != null) {
            rtspServer.stop();
            rtspServer = null;
        }
        super.onPause();
        activityPaused = false;
    }
}
