package com.example.roadqualitydetectionapp;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.HashMap;
import org.json.JSONObject;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private SensorManager sensorManager;
    private Sensor accelerometer;

    private TextView statusText, dataText;

    private FusedLocationProviderClient fusedLocationClient;
    private DatabaseReference databaseRef;

    private long lastUploadTime = 0;

    // 🔥 SMART ROAD NAME FETCH (with fallback chain)
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

            if (json.has("address")) {
                JSONObject address = json.getJSONObject("address");

                if (address.has("road")) return address.getString("road");
                if (address.has("neighbourhood")) return address.getString("neighbourhood");
                if (address.has("suburb")) return address.getString("suburb");
                if (address.has("city")) return address.getString("city");
                if (address.has("town")) return address.getString("town");
                if (address.has("village")) return address.getString("village");
            }

        } catch (Exception e) {
            Log.e("OSM_ERROR", e.toString());
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

        if (accelerometer == null) {
            dataText.setText("No accelerometer found!");
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        databaseRef = FirebaseDatabase.getInstance().getReference("road_data_v2");

        // 🔹 Drawer
        DrawerLayout drawerLayout = findViewById(R.id.drawer_layout);
        NavigationView navView = findViewById(R.id.nav_view);
        Button menuBtn = findViewById(R.id.menuBtn);

        navView.setCheckedItem(R.id.nav_dashboard);

        menuBtn.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));

        navView.setNavigationItemSelectedListener(item -> {

            int id = item.getItemId();

            if (id == R.id.nav_dashboard) return true;
            else if (id == R.id.nav_map)
                startActivity(new Intent(this, MapActivity.class));
            else if (id == R.id.nav_stats)
                startActivity(new Intent(this, StatsActivity.class));
            else if (id == R.id.nav_performance)
                startActivity(new Intent(this, PerformanceActivity.class));

            drawerLayout.closeDrawers();
            return true;
        });

        // 🔹 Bottom Nav
        BottomNavigationView bottomNav = findViewById(R.id.bottomNavigation);
        bottomNav.setSelectedItemId(R.id.nav_dashboard);

        bottomNav.setOnItemSelectedListener(item -> {

            int id = item.getItemId();

            if (id == R.id.nav_dashboard) return true;
            else if (id == R.id.nav_map)
                startActivity(new Intent(this, MapActivity.class));
            else if (id == R.id.nav_stats)
                startActivity(new Intent(this, StatsActivity.class));
            else if (id == R.id.nav_performance)
                startActivity(new Intent(this, PerformanceActivity.class));

            return true;
        });

        // 🔥 Permission
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (accelerometer != null) {
            sensorManager.registerListener(
                    this,
                    accelerometer,
                    SensorManager.SENSOR_DELAY_UI
            );
        }
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

        dataText.setText(
                "X: " + String.format("%.2f", x) +
                        "\nY: " + String.format("%.2f", y) +
                        "\nZ: " + String.format("%.2f", z) +
                        "\nMagnitude: " + String.format("%.2f", magnitude)
        );

        if (magnitude > 13.3 && System.currentTimeMillis() - lastUploadTime > 2000) {

            lastUploadTime = System.currentTimeMillis();

            statusText.setText("Rough Road Detected");
            statusText.setTextColor(0xFFEF4444);

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) return;

            // 🔥 USE CURRENT LOCATION (FIXED)
            fusedLocationClient.getCurrentLocation(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    null
            ).addOnSuccessListener(location -> {

                if (location != null) {

                    double lat = location.getLatitude();
                    double lng = location.getLongitude();

                    Log.d("LOCATION", lat + ", " + lng);

                    new Thread(() -> {

                        String roadName = getRoadNameFromOSM(lat, lng);

                        Log.d("ROAD_NAME", roadName);

                        HashMap<String, Object> data = new HashMap<>();
                        data.put("mag", magnitude);
                        data.put("road", roadName);
                        data.put("lat", lat);
                        data.put("lng", lng);
                        data.put("time", System.currentTimeMillis());

                        databaseRef.push().setValue(data);

                    }).start();

                } else {
                    Log.e("LOCATION", "Location is NULL");
                }
            });

        } else {
            statusText.setText("Normal");
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