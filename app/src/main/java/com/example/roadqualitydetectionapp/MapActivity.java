package com.example.roadqualitydetectionapp;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.database.*;

import org.osmdroid.config.Configuration;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

public class MapActivity extends AppCompatActivity {

    private MapView map;
    private DatabaseReference databaseRef;
    private FusedLocationProviderClient fusedLocationClient;

    private Marker userMarker; // 🔥 keep reference

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Configuration.getInstance().setUserAgentValue(getPackageName());
        setContentView(R.layout.activity_map);

        // 🔹 Map setup
        map = findViewById(R.id.map);
        map.setMultiTouchControls(true);
        map.setTileSource(org.osmdroid.tileprovider.tilesource.TileSourceFactory.MAPNIK);
        map.getController().setZoom(15.0);

        GeoPoint startPoint = new GeoPoint(28.6139, 77.2090);
        map.getController().setCenter(startPoint);

        // 🔹 Location
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);

        } else {
            fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {

                if (location != null) {

                    GeoPoint userPoint = new GeoPoint(
                            location.getLatitude(),
                            location.getLongitude()
                    );

                    map.getController().setZoom(17.0);
                    map.getController().setCenter(userPoint);

                    userMarker = new Marker(map);
                    userMarker.setPosition(userPoint);
                    userMarker.setTitle("📍 You are here");

                    map.getOverlays().add(userMarker);
                }
            });
        }

        // 🔹 Drawer
        DrawerLayout drawerLayout = findViewById(R.id.drawer_layout_map);
        NavigationView navView = findViewById(R.id.nav_view_map);
        Button menuBtnMap = findViewById(R.id.menuBtnMap);

        navView.setCheckedItem(R.id.nav_map);

        menuBtnMap.setOnClickListener(v ->
                drawerLayout.openDrawer(GravityCompat.START)
        );

        navView.setNavigationItemSelectedListener(item -> {

            int id = item.getItemId();

            if (id == R.id.nav_dashboard)
                startActivity(new Intent(this, MainActivity.class));

            else if (id == R.id.nav_stats)
                startActivity(new Intent(this, StatsActivity.class));

            else if (id == R.id.nav_performance)
                startActivity(new Intent(this, PerformanceActivity.class));

            drawerLayout.closeDrawers();
            return true;
        });

        // 🔥 Bottom Navigation
        BottomNavigationView bottomNav = findViewById(R.id.bottomNavigation);

        bottomNav.setSelectedItemId(R.id.nav_map); // ✅ highlight current

        bottomNav.setOnItemSelectedListener(item -> {

            int id = item.getItemId();

            if (id == R.id.nav_map) return true;

            else if (id == R.id.nav_dashboard)
                startActivity(new Intent(this, MainActivity.class));

            else if (id == R.id.nav_stats)
                startActivity(new Intent(this, StatsActivity.class));

            else if (id == R.id.nav_performance)
                startActivity(new Intent(this, PerformanceActivity.class));

            return true;
        });

        // 🔹 Firebase
        databaseRef = FirebaseDatabase.getInstance().getReference("road_data_v2");

        databaseRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {

                map.getOverlays().clear();

                // 🔥 re-add user marker
                if (userMarker != null) {
                    map.getOverlays().add(userMarker);
                }

                for (DataSnapshot data : snapshot.getChildren()) {

                    Double lat = data.child("lat").getValue(Double.class);
                    Double lng = data.child("lng").getValue(Double.class);
                    Double mag = data.child("mag").getValue(Double.class);

                    if (lat == null || lng == null || mag == null) continue;
                    if (mag < 13.3) continue;

                    GeoPoint point = new GeoPoint(lat, lng);

                    Marker marker = new Marker(map);
                    marker.setPosition(point);

                    Drawable icon;

                    if (mag >= 16) {
                        marker.setTitle("🔴 Dangerous pothole");
                        icon = ContextCompat.getDrawable(MapActivity.this, R.drawable.marker_red);
                    } else {
                        marker.setTitle("🟡 Moderate bump");
                        icon = ContextCompat.getDrawable(MapActivity.this, R.drawable.marker_yellow);
                    }

                    marker.setIcon(resizeDrawable(icon, 80, 80));
                    map.getOverlays().add(marker);
                }

                map.invalidate();
            }

            @Override
            public void onCancelled(DatabaseError error) {}
        });
    }

    private Drawable resizeDrawable(Drawable image, int width, int height) {
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        image.setBounds(0, 0, width, height);
        image.draw(canvas);
        return new BitmapDrawable(getResources(), bitmap);
    }
}