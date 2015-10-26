package edu.ucla.nesl.android.workoutrecorder;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.support.wearable.view.WatchViewStub;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

public class MainActivity extends Activity {
    private TextView mTextView;
    // private BGDataCollectionService serviceInstance = null;
    private static final String TAG = "Activity";
    // Set format for date and time
    private static TimeString mTimestring = new TimeString();
    // Current state of the recording
    private boolean mTracking = false;
    private String mTime = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Keep screen on
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        final WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub);
        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
            @Override
            public void onLayoutInflated(WatchViewStub stub) {
                mTextView = (TextView) stub.findViewById(R.id.text);

                // Get saved recording state from shared preference
                SharedPreferences sharedPref = getSharedPreferences(
                        getString(R.string.preference_file_key), Context.MODE_PRIVATE);
                mTracking = sharedPref.getBoolean(getString(R.string.saved_rec_flag), false);
                if (mTracking) {
                    mTime = sharedPref.getString(getString(R.string.saved_rec_time), "");
                    mTextView.setText("Tracking started at " + mTime);
                }
                else {
                    mTextView.setText("Tracking stopped");
                }
            }
        });

        // Start the service
        Intent intent = new Intent(this, BGDataCollectionService.class);
        startService(intent);
    }

    public void onStartClicked(View view) {
        Log.i(TAG, "start clicked");
        mTracking = true;
        mTime = mTimestring.currentTimeForDisplay();
        BGDataCollectionService.startRecording(mTimestring.currentTimeForFile());
        mTextView.setText("Tracking started at " + mTime);
    }

    public void onStopClicked(View view) {
        Log.i(TAG, "stop clicked");
        BGDataCollectionService.stopRecording();
        mTextView.setText("Tracking stopped");
        mTracking = false;
        mTime = null;
    }

    protected void onPause() {
        super.onPause();
        // Unbind to service on pause
//        unbindService(mConnection);
//        serviceInstance = null;
        // Save current recording state to shared preference
        SharedPreferences sharedPref = getSharedPreferences(
                getString(R.string.preference_file_key), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putBoolean(getString(R.string.saved_rec_flag), mTracking);
        if (mTracking) {
            editor.putString(getString(R.string.saved_rec_time), mTime);
        }
        else {
            editor.putString(getString(R.string.saved_rec_time), null);
        }
        editor.commit();
    }

    protected void onResume() {
        super.onResume();
//        // Bind to service on resume
//        Intent intent= new Intent(this, BGDataCollectionService.class);
//        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
    }

//    private ServiceConnection mConnection = new ServiceConnection() {
//        public void onServiceConnected(ComponentName className, IBinder b) {
//            BGDataCollectionService.MyBinder binder = (BGDataCollectionService.MyBinder) b;
//            serviceInstance = binder.getService();
//            Log.i(TAG, "service connected");
//        }
//
//        public void onServiceDisconnected(ComponentName className) {
//            serviceInstance = null;
//            Log.i(TAG, "service DISconnected");
//        }
//    };
}
