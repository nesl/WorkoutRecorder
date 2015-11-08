package edu.ucla.nesl.android.workoutrecorder;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;
import android.os.Vibrator;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class BGDataCollectionService extends Service implements SensorEventListener {
    private static final String TAG = "BGService";
    private final IBinder mBinder = new MyBinder();

    // Make everything static so that each bind will get
    // the same SensorManager, PrintWriter, and Wakelock
    private static SensorManager mSensorManager;
    private static ArrayList<Sensor> sensors = null;
    private static Map<Integer, BufferedWriter> sensorType2Logger = null;

    private static BufferedWriter loggerAcc;
    private static BufferedWriter loggerGyro;
    private static BufferedWriter loggerMag;
    private static BufferedWriter loggerGrav;

    private static long c[] = new long[20];

    private static Vibrator v;
    private static BGDataCollectionService mContext;

    private static PowerManager.WakeLock wakeLock;

    public BGDataCollectionService() {

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "Received start id " + startId + ": " + intent);
        mContext = this;
        return START_STICKY;
    }


    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        // for recording the time offset
        int sensorType = sensorEvent.sensor.getType();
        c[sensorType]++;
        if (c[sensorType] >= 1000) {
            Log.i(TAG, "type " + sensorType + " reached 1000 values.");
            c[sensorType] = 0;
        }
        long now = System.currentTimeMillis();

        String line = now + "," + sensorEvent.timestamp;
        for (float v : sensorEvent.values)
            line += "," + v;
        line += "\n";  // for JSON stringify

        try {
            sensorType2Logger.get(sensorType).write(line);
            sensorType2Logger.get(sensorType).flush();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    public class MyBinder extends Binder {
        public BGDataCollectionService getService() {
            return BGDataCollectionService.this;
        }
    }

    public static void startRecording(String timestring) {
        Log.i(TAG, "start recording");

        PowerManager pm = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
        wakeLock = pm.newWakeLock( PowerManager.PARTIAL_WAKE_LOCK, "MyWakelook");
        wakeLock.acquire();

        sensors = new ArrayList<>();
        sensorType2Logger = new HashMap<>();

        mSensorManager = ((SensorManager) mContext.getSystemService(SENSOR_SERVICE));
        sensors.add(mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER));
        sensors.add(mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE));
        sensors.add(mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD));
        sensors.add(mSensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY));

        v = (Vibrator) mContext.getSystemService(Context.VIBRATOR_SERVICE);

        String folder_prefix = "/sdcard/wear_" + timestring + "_";

        try {
            loggerAcc = new BufferedWriter(new FileWriter(folder_prefix + "acc.csv"));
            loggerGyro = new BufferedWriter(new FileWriter(folder_prefix + "gyro.csv"));
            loggerMag = new BufferedWriter(new FileWriter(folder_prefix + "mag.csv"));
            loggerGrav = new BufferedWriter(new FileWriter(folder_prefix + "grav.csv"));
        }
        catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, e.toString());
        }

        sensorType2Logger.put(Sensor.TYPE_ACCELEROMETER, loggerAcc);
        sensorType2Logger.put(Sensor.TYPE_GYROSCOPE,      loggerGyro);
        sensorType2Logger.put(Sensor.TYPE_MAGNETIC_FIELD, loggerMag);
        sensorType2Logger.put(Sensor.TYPE_GRAVITY, loggerGrav);

        // register sensors
        registerAllSensors();
        v.vibrate(500);
    }

    public static void stopRecording() {
        Log.i(TAG, "stop recording");

        unregisterAllSensors();
        mSensorManager = null;

        for (int sensor_id : sensorType2Logger.keySet()) {
            try {
                sensorType2Logger.get(sensor_id).flush();
                sensorType2Logger.get(sensor_id).close();
            }
            catch (Exception e) {
                e.printStackTrace();
                Log.e(TAG, e.toString());
            }
        }

        sensors.clear();
        sensors = null;
        sensorType2Logger.clear();
        sensorType2Logger = null;

        if (wakeLock != null) {
            wakeLock.release();
        }

        v.vibrate(1000);
    }

    private static void registerAllSensors() {
        for (Sensor sensor: sensors) {
            mSensorManager.registerListener(mContext, sensor, SensorManager.SENSOR_DELAY_FASTEST);
        }
    }

    private static void unregisterAllSensors() {
        for (Sensor sensor: sensors) {
            mSensorManager.unregisterListener(mContext, sensor);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.i(TAG, "onBind");
        return mBinder;
    }

    @Override
    public void onDestroy () {
        super.onDestroy();
        if (mSensorManager != null) {
            stopRecording();
        }
    }
}
