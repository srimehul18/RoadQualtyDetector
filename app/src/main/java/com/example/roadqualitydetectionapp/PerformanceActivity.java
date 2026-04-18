package com.example.roadqualitydetectionapp;

import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;
import com.google.firebase.database.*;

import java.util.ArrayList;
import java.util.HashMap;

public class PerformanceActivity extends AppCompatActivity {

    private DatabaseReference databaseRef;
    private LinearLayout container;

    private String currentFilter = "All";

    private HashMap<String, ArrayList<Double>> currentData = new HashMap<>();
    private HashMap<String, ArrayList<Double>> previousData = new HashMap<>();

    private double getAverage(ArrayList<Double> list) {
        if (list.size() == 0) return 0;
        double sum = 0;
        for (double v : list) sum += v;
        return sum / list.size();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_performance);

        container = findViewById(R.id.performanceContainer);

        DrawerLayout drawerLayout = findViewById(R.id.drawer_layout_stats);
        NavigationView navView = findViewById(R.id.nav_view_stats);
        Button menuBtn = findViewById(R.id.menuBtnStats);

        // FILTER BUTTONS
        Button filterAll = findViewById(R.id.filterAll);
        Button filterImproved = findViewById(R.id.filterImproved);
        Button filterSame = findViewById(R.id.filterSame);
        Button filterDegraded = findViewById(R.id.filterDegraded);

        filterAll.setOnClickListener(v -> {
            currentFilter = "All";
            renderUI();
        });

        filterImproved.setOnClickListener(v -> {
            currentFilter = "Improved";
            renderUI();
        });

        filterSame.setOnClickListener(v -> {
            currentFilter = "Same";
            renderUI();
        });

        filterDegraded.setOnClickListener(v -> {
            currentFilter = "Degraded";
            renderUI();
        });

        navView.inflateMenu(R.menu.menu_drawer);

        menuBtn.setOnClickListener(v -> drawerLayout.openDrawer(Gravity.START));

        navView.setNavigationItemSelectedListener(item -> {

            int id = item.getItemId();

            if (id == R.id.nav_dashboard) finish();
            else if (id == R.id.nav_map)
                startActivity(new android.content.Intent(this, MapActivity.class));
            else if (id == R.id.nav_stats)
                startActivity(new android.content.Intent(this, StatsActivity.class));

            drawerLayout.closeDrawers();
            return true;
        });

        // 🔥 Firebase
        databaseRef = FirebaseDatabase.getInstance().getReference("road_data_v2");

        loadData();
    }

    private void loadData() {

        databaseRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {

                currentData.clear();
                previousData.clear();

                long now = System.currentTimeMillis();
                long threeDays = 3L * 24 * 60 * 60 * 1000;

                long currentStart = now - threeDays;
                long previousStart = now - (2 * threeDays);

                for (DataSnapshot data : snapshot.getChildren()) {

                    Double mag = data.child("mag").getValue(Double.class);
                    String road = data.child("road").getValue(String.class);
                    Long time = data.child("time").getValue(Long.class);

                    if (mag == null || road == null || time == null) continue;

                    if (time >= currentStart) {
                        currentData.putIfAbsent(road, new ArrayList<>());
                        currentData.get(road).add(mag);
                    }
                    else if (time >= previousStart) {
                        previousData.putIfAbsent(road, new ArrayList<>());
                        previousData.get(road).add(mag);
                    }
                }

                renderUI();
            }

            @Override
            public void onCancelled(DatabaseError error) {}
        });
    }

    private void renderUI() {

        container.removeAllViews();

        for (String road : currentData.keySet()) {

            double currentAvg = getAverage(currentData.get(road));
            double prevAvg = getAverage(
                    previousData.getOrDefault(road, new ArrayList<>())
            );

            double threshold = 0.5;
            String status;
            int color;

            if (currentAvg > prevAvg + threshold) {
                status = "Degraded";
                color = 0xFFE53935;
            }
            else if (currentAvg < prevAvg - threshold) {
                status = "Improved";
                color = 0xFF22C55E;
            }
            else {
                status = "Same";
                color = 0xFFFBBF24;
            }

            // 🔥 FILTER LOGIC
            if (!currentFilter.equals("All") && !status.equals(currentFilter)) {
                continue;
            }

            TextView card = new TextView(this);
            card.setText(
                    road +
                            "\nPrev: " + String.format("%.2f", prevAvg) +
                            " → Now: " + String.format("%.2f", currentAvg) +
                            "\nStatus: " + status
            );

            card.setTextColor(0xFFFFFFFF);
            card.setPadding(30, 30, 30, 30);
            card.setBackgroundColor(0xFF111827);

            LinearLayout.LayoutParams params =
                    new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                    );

            params.setMargins(0, 0, 0, 20);
            card.setLayoutParams(params);

            container.addView(card);
        }
    }
}