package com.example.roadqualitydetectionapp;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
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
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.HashMap;
import java.util.Scanner;

import org.json.JSONObject;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private SensorManager sensorManager;
    private Sensor accelerometer;

    private TextView statusText, dataText;

    private FusedLocationProviderClient fusedLocationClient;
    private DatabaseReference databaseRef;

    private long lastUploadTime = 0;

    // 🔥 Reverse geocoding
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

            if (address.has("road")) return address.getString("road");
            else if (address.has("suburb")) return address.getString("suburb");

        } catch (Exception e) {
            e.printStackTrace();
        }

        return "Unknown Road";
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 🔹 UI
        statusText = findViewById(R.id.status);
        dataText = findViewById(R.id.data);

        // 🔹 Sensors
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        if (accelerometer == null) {
            dataText.setText("No accelerometer found!");
        }

        // 🔹 Location + Firebase
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
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (accelerometer != null) {
            sensorManager.registerListener(
                    this,
                    accelerometer,
                    SensorManager.SENSOR_DELAY_UI   // 🔥 smoother updates
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

        // 🔥 ALWAYS UPDATE UI
        String sensorText =
                "X: " + String.format("%.2f", x) +
                        "\nY: " + String.format("%.2f", y) +
                        "\nZ: " + String.format("%.2f", z) +
                        "\nMagnitude: " + String.format("%.2f", magnitude);

        dataText.setText(sensorText);

        // 🔥 DETECTION
        if (magnitude > 13.3 && System.currentTimeMillis() - lastUploadTime > 2000) {

            lastUploadTime = System.currentTimeMillis();

            statusText.setText("Rough Road Detected");
            statusText.setTextColor(0xFFEF4444);

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