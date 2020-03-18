package com.example.myapplication;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import androidx.camera.camera2.Camera2Config;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.core.impl.ImageCaptureConfig;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import androidx.camera.camera2.Camera2Config;
import androidx.camera.core.CameraX;
import androidx.camera.core.CameraXConfig;
import androidx.camera.lifecycle.ProcessCameraProvider;
import com.google.common.util.concurrent.ListenableFuture;

import com.loopj.android.http.*;//loopj library



import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.Image;
import android.os.Build;
import android.os.Bundle;
import android.hardware.SensorManager;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;

import cz.msebera.android.httpclient.Header;

import static androidx.camera.core.ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY;


public class MainActivity extends AppCompatActivity implements SensorEventListener, LocationListener, CameraXConfig.Provider {
    private SensorManager senSensorManager;
    private Sensor senAccelerometer;
    private LocationManager locationManager;
    public Location loc;
    public double latitude;
    public double longitude;
    public Preview mPreview;
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;


    public ImageCapture imageCapture;
    public Executor executor = new ThreadPerTaskExecutor();
    public CameraSelector cameraSelector;
    public Preview preview;


    private long lastUpdate = 0;
    private float last_x, last_y, last_z,speed;
    private static final int SHAKE_THRESHOLD = 600;

    FirebaseDatabase database = FirebaseDatabase.getInstance();

    class ThreadPerTaskExecutor implements Executor {
        public void execute(Runnable r) {
            new Thread(r).start();
        }
    }



    @Override
    public void onLocationChanged(Location location) {
        //Hey, a non null location! Sweet!
        Log.d("onLocationChanged", "invoked");
        //remove location callback:
        locationManager.removeUpdates(this);

        //open the map:
        latitude = location.getLatitude();
        longitude = location.getLongitude();
        Log.d("Long ", String.valueOf(longitude));
        Log.d("Lat",String.valueOf(latitude));

        doLocationStuff(location);


    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {

    }

    @Override
    public void onProviderEnabled(String s) {

    }

    @Override
    public void onProviderDisabled(String s) {

    }

    @NonNull
    @Override
    public CameraXConfig getCameraXConfig() {
        return Camera2Config.defaultConfig();

    }


    public static class User {

        public String accvalue;
        public String Latitude;
        public String Longitude;

        public User(String accvalue,String Latitude,String Longitude) {
            this.accvalue=accvalue;
            this.Latitude=Latitude;
            this.Longitude=Longitude;

        }


    }
    void bindPreview(@NonNull ProcessCameraProvider cameraProvider) {
        PreviewView previewView=(PreviewView)findViewById(R.id.view_finder);
        preview = new Preview.Builder()
                .build();

        preview.setSurfaceProvider(previewView.getPreviewSurfaceProvider());

        cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build();
        cameraProvider.bindToLifecycle((LifecycleOwner)this, cameraSelector,imageCapture, preview);


    }
    public Bitmap toBitmap(Image image) {
        Image.Plane[] planes = image.getPlanes();
        ByteBuffer yBuffer = planes[0].getBuffer();
        ByteBuffer uBuffer = planes[1].getBuffer();
        ByteBuffer vBuffer = planes[2].getBuffer();

        int ySize = yBuffer.remaining();
        int uSize = uBuffer.remaining();
        int vSize = vBuffer.remaining();

        byte[] nv21 = new byte[ySize + uSize + vSize];
        //U and V are swapped
        yBuffer.get(nv21, 0, ySize);
        vBuffer.get(nv21, ySize, vSize);
        uBuffer.get(nv21, ySize + vSize, uSize);

        YuvImage yuvImage = new YuvImage(nv21, ImageFormat.NV21, image.getWidth(), image.getHeight(), null);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        yuvImage.compressToJpeg(new Rect(0, 0, yuvImage.getWidth(), yuvImage.getHeight()), 75, out);

        byte[] imageBytes = out.toByteArray();
        return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
    }
    public void takepic(String Latitude,String Longitude)
    {
        Log.i("called","takepic called");
        File storageDir = this.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        ImageCapture.OutputFileOptions outputFileOptions =
                new ImageCapture.OutputFileOptions.Builder(new File(storageDir+timeStamp+".jpg")).build();

        File file=new File(storageDir+timeStamp+".jpg");
        try {
            file.createNewFile();
            Log.i("info:","file sucessfully created at:"+storageDir);
        } catch (IOException e) {
            Log.d("error file creation",String.valueOf(e));
        }

        imageCapture.takePicture(outputFileOptions,executor, new ImageCapture.OnImageSavedCallback() {
            @Override
            public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                Log.i("SUCESS:", "Image saved scuessfully");
                Handler mainHandler = new Handler(Looper.getMainLooper());
                Runnable myRunnable = new Runnable() {
                    @Override
                    public void run() {
                        AsyncHttpClient client = new AsyncHttpClient();
                        RequestParams params = new RequestParams();
                        params.put("Latitude",Latitude);
                        params.put("Longitude",Longitude);
                        File pimage=new File(storageDir +timeStamp + ".jpg");
                        try {
                            params.put("file", new FileInputStream(pimage));
                            Log.i("sucess into:","file inserted to packet");
                        } catch (Exception e) {
                            Log.e("error", "unable to put into packet");
                        }


                        client.post("http://a680566a.ngrok.io/image", params, new AsyncHttpResponseHandler() {
                            @Override
                            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                                String str=null;
                                try{
                                str = new String(responseBody, "UTF-8");}
                                catch (Exception e)
                                {
                                    Log.e("errorconvertingresponse",String.valueOf(e));

                                }
                                Log.i("Sucess:", "Image Sucessfully sent to server:"+str);

                            }

                            @Override
                            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                                Log.e("error:", "unable to send image:" + String.valueOf(statusCode)+String.valueOf(headers));

                            }



                        }
                        );
                    }
                };
                mainHandler.post(myRunnable);

            }

            @Override
            public void onError(@NonNull ImageCaptureException exception) {
                Log.d("error:",String.valueOf(exception));

            }
        }
    );




    }



    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                bindPreview(cameraProvider);
            } catch (ExecutionException | InterruptedException e) {
                // No errors need to be handled for this Future.
                // This should never be reached.
            }
        }, ContextCompat.getMainExecutor(this));

        imageCapture =   new ImageCapture.Builder()
                .setTargetRotation(getWindowManager().getDefaultDisplay().getRotation()).build();




        Log.i("info:","app started");

        senSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        senAccelerometer = senSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        senSensorManager.registerListener(this, senAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.d("Info:","GPS Enabled");

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

                speed = Math.abs(x + y + z - last_x - last_y - last_z) / diffTime * 10000;

                if (speed > SHAKE_THRESHOLD) {
                    Log.i("Info:", "Device Shaken:" + String.valueOf(speed));
                    if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        // TODO: Consider calling
                        //    Activity#requestPermissions
                        // here to request the missing permissions, and then overriding
                        //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                        //                                          int[] grantResults)
                        // to handle the case where the user grants the permission. See the documentation
                        // for Activity#requestPermissions for more details.
                        Log.d("Permission", "No location prmission");
                        return;
                    }
                    loc = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                    if(loc != null) {
                        doLocationStuff(loc);
                    }
                    else{
                        //No last known location, request one
                        Criteria criteria = new Criteria();
                        String bestProvider = String.valueOf(locationManager.getBestProvider(criteria, true)).toString();

                        locationManager.requestLocationUpdates(bestProvider, 1000, 0, this);
                    }



                }

                last_x = x;
                last_y = y;
                last_z = z;
            }

        }

    }


    private void doLocationStuff(Location loc){
        Log.i("Info", "coordinates:" + loc.getLatitude() + "," + loc.getLongitude());

        takepic(String.valueOf(loc.getLatitude()),String.valueOf(loc.getLongitude()));



        DatabaseReference myRef = database.getReference();
        DatabaseReference newref = myRef.push();

        newref.setValue(new User(String.valueOf(speed), String.valueOf(loc.getLatitude()), String.valueOf(loc.getLongitude())));


        Toast.makeText(MainActivity.this, "Location Updated!", Toast.LENGTH_SHORT).show();
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
