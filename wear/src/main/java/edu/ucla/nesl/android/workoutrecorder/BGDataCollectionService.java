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

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class BGDataCollectionService extends Service implements SensorEventListener {
    private static final String TAG = "BGService";
    private final IBinder mBinder = new MyBinder();

    // Make everything static so that each bind will get
    // the same SensorManager, PrintWriter, and Wakelock
    private static SensorManager mSensorManager;
    private static ArrayList<Sensor> sensors = new ArrayList<>();
    private static Map<Integer, PrintWriter> sensorType2Logger = new HashMap<>();

    private static PrintWriter loggerAcc;
    private static PrintWriter loggerGyro;
    private static PrintWriter loggerMag;
    private static PrintWriter loggerGrav;

    private static PowerManager.WakeLock wakeLock;

    public BGDataCollectionService() {
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        // for recording the time offset
        int sensorType = sensorEvent.sensor.getType();
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

    public void startRecording(String timestring) {
        Log.i(TAG, "start recording");

        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wakeLock = pm.newWakeLock( PowerManager.PARTIAL_WAKE_LOCK, "MyWakelook");
        wakeLock.acquire();

        mSensorManager = ((SensorManager) getSystemService(SENSOR_SERVICE));
        sensors.add(mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER));
        sensors.add(mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE));
        sensors.add(mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD));
        sensors.add(mSensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY));

        String folder_prefix = "/sdcard/wear_"
                + timestring + "_";

        try {
            loggerAcc = new PrintWriter(folder_prefix + "acc.csv");
            loggerGyro = new PrintWriter(folder_prefix + "gyro.csv");
            loggerMag = new PrintWriter(folder_prefix + "mag.csv");
            loggerGrav = new PrintWriter(folder_prefix + "grav.csv");
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        sensorType2Logger.put(Sensor.TYPE_ACCELEROMETER,  loggerAcc);
        sensorType2Logger.put(Sensor.TYPE_GYROSCOPE,      loggerGyro);
        sensorType2Logger.put(Sensor.TYPE_MAGNETIC_FIELD, loggerMag);
        sensorType2Logger.put(Sensor.TYPE_GRAVITY, loggerGrav);

        // register sensors
        registerAllSensors();
    }

    public void stopRecording() {
        Log.i(TAG, "stop recording");

        unregisterAllSensors();
        for (int sensor_id : sensorType2Logger.keySet()) {
            try {
                sensorType2Logger.get(sensor_id).flush();
                sensorType2Logger.get(sensor_id).close();
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }

        sensors.clear();
        sensorType2Logger.clear();

        if (wakeLock != null) {
            wakeLock.release();
        }
    }

    private void registerAllSensors() {
        for (Sensor sensor: sensors) {
            mSensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_FASTEST);
        }
    }

    private void unregisterAllSensors() {
        for (Sensor sensor: sensors) {
            mSensorManager.unregisterListener(this, sensor);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.i(TAG, "onBind");
        return mBinder;
    }
}
