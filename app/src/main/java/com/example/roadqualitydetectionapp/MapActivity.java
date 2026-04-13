package com.example.roadqualitydetectionapp;

import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.core.content.ContextCompat;

import com.google.android.material.navigation.NavigationView;
import com.google.firebase.database.*;

import org.osmdroid.config.Configuration;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

import android.graphics.drawable.Drawable;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;

public class MapActivity extends AppCompatActivity {

    private MapView map;
    private DatabaseReference databaseRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Configuration.getInstance().setUserAgentValue(getPackageName());
        setContentView(R.layout.activity_map);

        // ✅ Map setup
        map = findViewById(R.id.map);
        map.setMultiTouchControls(true);
        map.setTileSource(org.osmdroid.tileprovider.tilesource.TileSourceFactory.MAPNIK);

        map.getController().setZoom(15.0);
        GeoPoint startPoint = new GeoPoint(28.6139, 77.2090);
        map.getController().setCenter(startPoint);

        // ✅ Drawer setup
        DrawerLayout drawerLayout = findViewById(R.id.drawer_layout_map);
        NavigationView navView = findViewById(R.id.nav_view_map);
        Button menuBtnMap = findViewById(R.id.menuBtnMap);

        navView.inflateMenu(R.menu.menu_drawer);

        menuBtnMap.setOnClickListener(v -> {
            drawerLayout.openDrawer(Gravity.LEFT); // ✅ FIXED
        });

        navView.setNavigationItemSelectedListener(item -> {

            if (item.getItemId() == R.id.nav_dashboard) {
                finish();
            }

            drawerLayout.closeDrawers();
            return true;
        });

        // 🔥 Firebase
        databaseRef = FirebaseDatabase.getInstance().getReference("road_data");

        databaseRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {

                map.getOverlays().clear();

                for (DataSnapshot data : snapshot.getChildren()) {

                    Double lat = data.child("lat").getValue(Double.class);
                    Double lng = data.child("lng").getValue(Double.class);
                    Double mag = data.child("mag").getValue(Double.class);

                    if (lat == null || lng == null || mag == null) continue;
                    if (mag < 13) continue; // ✅ threshold

                    GeoPoint point = new GeoPoint(lat, lng);

                    Marker marker = new Marker(map);
                    marker.setPosition(point);

                    Drawable icon;

                    // 🎯 COLOR LOGIC
                    if (mag >= 16) {
                        marker.setTitle("🔴 Dangerous pothole");
                        icon = ContextCompat.getDrawable(MapActivity.this, R.drawable.marker_red);
                    } else {
                        marker.setTitle("🟡 Minor bump");
                        icon = ContextCompat.getDrawable(MapActivity.this, R.drawable.marker_yellow);
                    }

                    // ✅ FIX: resize icon (VERY IMPORTANT)
                    marker.setIcon(resizeDrawable(icon, 80, 80));

                    map.getOverlays().add(marker);
                }

                map.invalidate();
            }

            @Override
            public void onCancelled(DatabaseError error) {}
        });
    }

    // ✅ FUNCTION TO RESIZE ICON (FIXES GIANT MARKER ISSUE)
    private Drawable resizeDrawable(Drawable image, int width, int height) {
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        image.setBounds(0, 0, width, height);
        image.draw(canvas);
        return new BitmapDrawable(getResources(), bitmap);
    }
}