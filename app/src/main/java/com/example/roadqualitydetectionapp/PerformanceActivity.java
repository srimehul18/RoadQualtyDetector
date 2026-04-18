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
import android.view.View;
import android.content.Intent;

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
        navView.setCheckedItem(R.id.nav_performance);
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

        menuBtn.setOnClickListener(v -> drawerLayout.openDrawer(Gravity.LEFT));

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

        // ✅ STEP 1: CREATE ROAD LIST
        ArrayList<String> roads = new ArrayList<>(currentData.keySet());

        // ✅ STEP 2: SORT (WORST FIRST)
        roads.sort((a, b) -> {

            double currentA = getAverage(currentData.get(a));
            double prevA = getAverage(previousData.getOrDefault(a, new ArrayList<>()));

            double currentB = getAverage(currentData.get(b));
            double prevB = getAverage(previousData.getOrDefault(b, new ArrayList<>()));

            double diffA = currentA - prevA;
            double diffB = currentB - prevB;

            return Double.compare(diffB, diffA); // descending
        });

        // ✅ STEP 3: LOOP THROUGH SORTED LIST
        for (String road : roads) {

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

            // ✅ STEP 4: FILTER LOGIC
            if (!currentFilter.equals("All") && !status.equals(currentFilter)) {
                continue;
            }

            // 🔥 CARD CONTAINER
            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.HORIZONTAL);
            card.setPadding(20, 20, 20, 20);
            card.setBackgroundResource(R.drawable.card_bg);
            card.setElevation(8f); // 🔥 shadow

            LinearLayout.LayoutParams cardParams =
                    new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                    );
            cardParams.setMargins(0, 0, 0, 24);
            card.setLayoutParams(cardParams);

            // 🔴 LEFT BAR
            View statusBar = new View(this);
            LinearLayout.LayoutParams barParams =
                    new LinearLayout.LayoutParams(12,
                            LinearLayout.LayoutParams.MATCH_PARENT);
            barParams.setMargins(0, 0, 20, 0);
            statusBar.setLayoutParams(barParams);
            statusBar.setBackgroundColor(color);

            // 📄 CONTENT
            LinearLayout content = new LinearLayout(this);
            content.setOrientation(LinearLayout.VERTICAL);

            // 🛣 ROAD NAME
            TextView roadName = new TextView(this);
            roadName.setText(road);
            roadName.setTextColor(0xFFFFFFFF);
            roadName.setTextSize(16f);
            roadName.setTypeface(null, android.graphics.Typeface.BOLD);

            // 📊 VALUES
            TextView details = new TextView(this);
            details.setText(
                    "Prev: " + String.format("%.2f", prevAvg) +
                            " → Now: " + String.format("%.2f", currentAvg)
            );
            details.setTextColor(0xFF9CA3AF);
            details.setTextSize(13f);

            // 📍 STATUS
            TextView statusText = new TextView(this);
            statusText.setText("Status: " + status);
            statusText.setTextColor(color);
            statusText.setTextSize(14f);

            // ADD CONTENT
            content.addView(roadName);
            content.addView(details);
            content.addView(statusText);

            // FINAL COMBINE
            card.addView(statusBar);
            card.addView(content);

            container.addView(card);
        }
    }
}