package net.majorkernelpanic.spydroid;

import java.io.IOException;

import net.majorkernelpanic.networking.RtspServer;
import net.majorkernelpanic.networking.Session;
import net.majorkernelpanic.streaming.video.VideoQuality;
import net.majorkernelpanic.streaming.video.VideoStream;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
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

public class SpydroidActivity extends Activity
{
    private static final String TAG = "SpydroidActivity";

    /** Default quality of video streams **/
    public static VideoQuality videoQuality = new VideoQuality(640, 480, 15, 500000);

    /** By default H.263 is the video encoder **/
    public static int videoEncoder = Session.VIDEO_H263;

    /** The HttpServer will use those variables to send reports about the state of the app to the http interface **/
    public static boolean activityPaused = true;
    public static Exception lastCaughtException;

    static private RtspServer rtspServer = null;

    private SurfaceHolder holder;
    private SurfaceView camera;
    private TextView line1, line2, signWifi, signStreaming;
    private ImageView buttonSettings, buttonAbout;
    private LinearLayout signInformation;
    private Context context;
    private Animation pulseAnimation;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.main);

        camera = (SurfaceView)findViewById(R.id.smallcameraview);
        context = this.getApplicationContext();
        line1 = (TextView)findViewById(R.id.line1);
        line2 = (TextView)findViewById(R.id.line2);
        buttonSettings = (ImageView)findViewById(R.id.button_settings);
        buttonAbout = (ImageView)findViewById(R.id.button_about);
        signWifi = (TextView)findViewById(R.id.advice);
        signStreaming = (TextView)findViewById(R.id.streaming);
        signInformation = (LinearLayout)findViewById(R.id.information);
        pulseAnimation = AnimationUtils.loadAnimation(this, R.anim.pulse);

        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);

        camera.getHolder().setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        holder = camera.getHolder();

        videoEncoder = Integer.parseInt(settings.getString("video_encoder", String.valueOf(videoEncoder)));

        // Read video quality settings from the preferences
        videoQuality = VideoQuality.merge(
                new VideoQuality(
                        settings.getInt("video_resX", 0),
                        settings.getInt("video_resY", 0),
                        Integer.parseInt(settings.getString("video_framerate", "0")),
                        Integer.parseInt(settings.getString("video_bitrate", "0"))*1000
                ),
                videoQuality);

        Session.setSurfaceHolder(holder);
        Session.setDefaultVideoEncoder(videoEncoder);
        Session.setDefaultVideoQuality(videoQuality);

        if (rtspServer == null) {
            rtspServer = new RtspServer(8086);
        } // if

        buttonSettings.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                // Starts QualityListActivity where user can change the quality of the stream
                Intent intent = new Intent(context,OptionsActivity.class);
                startActivityForResult(intent, 0);
            }
        });
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

    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }
}
