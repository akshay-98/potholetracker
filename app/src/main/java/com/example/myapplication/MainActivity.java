package com.example.myapplication;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.hardware.SensorManager;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity implements SensorEventListener {
    private SensorManager senSensorManager;
    private Sensor senAccelerometer;
    private LocationManager locationManager;
    public Location loc;

    private long lastUpdate = 0;
    private float last_x, last_y, last_z;
    private static final int SHAKE_THRESHOLD = 600;
    private Toast toast;

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.i("info:","app started");

        senSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        senAccelerometer = senSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        senSensorManager.registerListener(this, senAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {

        Sensor mySensor = sensorEvent.sensor;

        if (mySensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            float x = sensorEvent.values[0];
            float y = sensorEvent.values[1];
            float z = sensorEvent.values[2];

            long curTime = System.currentTimeMillis();

            if ((curTime - lastUpdate) > 100) {
                long diffTime = (curTime - lastUpdate);
                lastUpdate = curTime;

                float speed = Math.abs(x + y + z - last_x - last_y - last_z) / diffTime * 10000;

                if (speed > SHAKE_THRESHOLD) {
                    if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        // TODO: Consider calling
                        //    Activity#requestPermissions
                        // here to request the missing permissions, and then overriding
                        //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                        //                                          int[] grantResults)
                        // to handle the case where the user grants the permission. See the documentation
                        // for Activity#requestPermissions for more details.
                        return;
                    }
                    loc = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                    Log.i("Info:","Device Shaken:"+String.valueOf(speed));
                    Log.i("Info","coordinates:"+loc.getLatitude()+","+loc.getLongitude());
                    Context context = getBaseContext();
                    CharSequence text = "Device Shaken";
                    int duration = Toast.LENGTH_SHORT;

                    if(toast==null) {
                        toast = Toast.makeText(context, text, duration);
                        toast.show();
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                toast=null;

                            }
                        }, 2000);
                    }
                    else{
                        toast.cancel();
                        toast=null;
                    }



                }

                last_x = x;
                last_y = y;
                last_z = z;
            }

        }

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    protected void onPause() {
        super.onPause();
        senSensorManager.unregisterListener(this);
    }

    protected void onResume() {
        super.onResume();
        senSensorManager.registerListener(this, senAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
    }
}
