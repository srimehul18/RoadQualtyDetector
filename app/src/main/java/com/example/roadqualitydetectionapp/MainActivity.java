package com.example.roadqualitydetectionapp;
import android.view.Gravity.*;
import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.android.material.navigation.NavigationView;

import java.util.HashMap;

import org.json.JSONObject;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;
import android.content.Intent;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private SensorManager sensorManager;
    private Sensor accelerometer;

    private TextView statusText, dataText;

    private FusedLocationProviderClient fusedLocationClient;
    private DatabaseReference databaseRef;

    private long lastUploadTime = 0;


    private String getRoadNameFromOSM(double lat, double lng) {
        try {
            String urlStr = "https://nominatim.openstreetmap.org/reverse?format=json&lat="
                    + lat + "&lon=" + lng;

            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", "RoadQualityApp");

            InputStream is = conn.getInputStream();
            Scanner scanner = new Scanner(is).useDelimiter("\\A");
            String response = scanner.hasNext() ? scanner.next() : "";

            JSONObject json = new JSONObject(response);
            JSONObject address = json.getJSONObject("address");

            if (address.has("road")) {
                return address.getString("road");
            } else if (address.has("suburb")) {
                return address.getString("suburb");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return "Unknown Road";
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        statusText = findViewById(R.id.status);
        dataText = findViewById(R.id.data);

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        databaseRef = FirebaseDatabase.getInstance().getReference("road_data_v2");

        DrawerLayout drawerLayout = findViewById(R.id.drawer_layout);
        NavigationView navView = findViewById(R.id.nav_view);
        navView.setCheckedItem(R.id.nav_dashboard);
        Button menuBtn = findViewById(R.id.menuBtn);

        navView.inflateMenu(R.menu.menu_drawer);

        menuBtn.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));

        navView.setNavigationItemSelectedListener(item -> {

            int id = item.getItemId();

            if (id == R.id.nav_dashboard) {
                startActivity(new Intent(this, MainActivity.class));
            }
            else if (id == R.id.nav_map) {
                startActivity(new Intent(this, MapActivity.class));
            }
            else if (id == R.id.nav_stats) {
                startActivity(new Intent(this, StatsActivity.class));
            }
            else if (id == R.id.nav_performance) {
                startActivity(new Intent(this, PerformanceActivity.class));
            }

            drawerLayout.closeDrawers();
            return true;
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {

        float x = event.values[0];
        float y = event.values[1];
        float z = event.values[2];

        double magnitude = Math.sqrt(x * x + y * y + z * z);

        // 🔄 ALWAYS update UI
        dataText.setText(
                "X: " + x +
                        "\nY: " + y +
                        "\nZ: " + z +
                        "\nMagnitude: " + magnitude
        );

        if (magnitude > 13.3 && System.currentTimeMillis() - lastUploadTime > 2000) {

            lastUploadTime = System.currentTimeMillis();

            statusText.setText("⚠️ Rough Road Detected!");
            statusText.setTextColor(0xFFEF4444);

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
                return;
            }

            fusedLocationClient.getCurrentLocation(
                    com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY,
                    null
            ).addOnSuccessListener(location -> {

                if (location != null) {

                    double lat = location.getLatitude();
                    double lng = location.getLongitude();

                    new Thread(() -> {

                        String roadName = getRoadNameFromOSM(lat, lng);
                        long timestamp = System.currentTimeMillis();

                        HashMap<String, Object> data = new HashMap<>();
                        data.put("lat", lat);
                        data.put("lng", lng);
                        data.put("mag", magnitude);
                        data.put("time", timestamp);
                        data.put("road", roadName);

                        databaseRef.push().setValue(data);

                    }).start();

                    dataText.setText(
                            "X: " + x +
                                    "\nY: " + y +
                                    "\nZ: " + z +
                                    "\nMagnitude: " + magnitude +
                                    "\nLat: " + lat +
                                    "\nLng: " + lng
                    );

                } else {
                    dataText.setText("Location not available...");
                }
            });

        } else {
            statusText.setText("Status: Normal");
            statusText.setTextColor(0xFF22C55E);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
}