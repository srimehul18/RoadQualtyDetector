package com.example.roadqualitydetectionapp;

import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.widget.AdapterView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;
import com.google.firebase.database.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import android.view.View;
import android.content.Intent;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class PerformanceActivity extends AppCompatActivity {

    private DatabaseReference databaseRef;
    private LinearLayout container;

    private String currentFilter = "All";
    private String searchQuery = "";

    // 🔥 SORT MODE
    private String currentSort = "WORST"; // default same as before

    private HashMap<String, ArrayList<Double>> todayData = new HashMap<>();
    private HashMap<String, ArrayList<Double>> yesterdayData = new HashMap<>();

    private double getAverage(ArrayList<Double> list) {
        if (list == null || list.size() == 0) return 0;
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

        navView.setCheckedItem(R.id.nav_performance);
        menuBtn.setOnClickListener(v -> drawerLayout.openDrawer(Gravity.LEFT));

        // 🔍 SEARCH
        EditText searchBar = findViewById(R.id.searchBar);
        searchBar.addTextChangedListener(new android.text.TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void afterTextChanged(android.text.Editable s) {}

            public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchQuery = s.toString().toLowerCase();
                renderUI();
            }
        });

        // 🔽 SORT DROPDOWN (ONLY ADDITION)
        Spinner sortSpinner = findViewById(R.id.sortSpinner);

        String[] options = {"Worst", "Recent", "A-Z"};

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                R.layout.spinner_item,
                options
        );

        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        sortSpinner.setAdapter(adapter);

        sortSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

                String selected = parent.getItemAtPosition(position).toString();

                if (selected.equals("A-Z")) {
                    currentSort = "ALPHA";
                }
                else if (selected.equals("Recent")) {
                    currentSort = "RECENT";
                }
                else {
                    currentSort = "WORST";
                }

                renderUI();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // FILTER BUTTONS
        findViewById(R.id.filterAll).setOnClickListener(v -> { currentFilter = "All"; renderUI(); });
        findViewById(R.id.filterImproved).setOnClickListener(v -> { currentFilter = "Improved"; renderUI(); });
        findViewById(R.id.filterSame).setOnClickListener(v -> { currentFilter = "Same"; renderUI(); });
        findViewById(R.id.filterDegraded).setOnClickListener(v -> { currentFilter = "Degraded"; renderUI(); });

        // NAVIGATION
        navView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_dashboard)
                startActivity(new Intent(this, MainActivity.class));
            else if (id == R.id.nav_map)
                startActivity(new Intent(this, MapActivity.class));
            else if (id == R.id.nav_stats)
                startActivity(new Intent(this, StatsActivity.class));

            drawerLayout.closeDrawers();
            return true;
        });

        BottomNavigationView bottomNav = findViewById(R.id.bottomNavigation);
        bottomNav.setSelectedItemId(R.id.nav_performance);

        databaseRef = FirebaseDatabase.getInstance().getReference("road_data_v2");

        loadData();
    }

    private void loadData() {

        databaseRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {

                todayData.clear();
                yesterdayData.clear();

                long now = System.currentTimeMillis();

                long oneDay = 24L * 60 * 60 * 1000;

                long todayStart = now - oneDay;
                long yesterdayStart = now - (2 * oneDay);

                for (DataSnapshot data : snapshot.getChildren()) {

                    Double mag = data.child("mag").getValue(Double.class);
                    String road = data.child("road").getValue(String.class);
                    Long time = data.child("time").getValue(Long.class);

                    if (mag == null || road == null || time == null) continue;

                    if (time >= todayStart) {
                        todayData.putIfAbsent(road, new ArrayList<>());
                        todayData.get(road).add(mag);
                    }
                    else if (time >= yesterdayStart) {
                        yesterdayData.putIfAbsent(road, new ArrayList<>());
                        yesterdayData.get(road).add(mag);
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

        HashSet<String> allRoads = new HashSet<>();
        allRoads.addAll(todayData.keySet());
        allRoads.addAll(yesterdayData.keySet());

        ArrayList<String> roads = new ArrayList<>(allRoads);

        // 🔥 SORTING (ONLY CHANGE)
        if (currentSort.equals("ALPHA")) {

            roads.sort(String::compareToIgnoreCase);

        } else if (currentSort.equals("RECENT")) {

            roads.sort((a, b) -> {
                double aVal = getAverage(todayData.get(a));
                double bVal = getAverage(todayData.get(b));
                return Double.compare(bVal, aVal);
            });

        } else { // WORST (your original)

            roads.sort((a, b) -> {
                double diffA = getAverage(todayData.get(a)) - getAverage(yesterdayData.get(a));
                double diffB = getAverage(todayData.get(b)) - getAverage(yesterdayData.get(b));
                return Double.compare(diffB, diffA);
            });
        }

        for (String road : roads) {

            if (!road.toLowerCase().contains(searchQuery)) continue;

            double todayAvg = getAverage(todayData.get(road));
            double yesterdayAvg = getAverage(yesterdayData.get(road));

            double threshold = 0.5;
            String status;
            int color;

            if (todayAvg > yesterdayAvg + threshold) {
                status = "Degraded";
                color = 0xFFE53935;
            }
            else if (todayAvg < yesterdayAvg - threshold) {
                status = "Improved";
                color = 0xFF22C55E;
            }
            else {
                status = "Same";
                color = 0xFFFBBF24;
            }

            if (!currentFilter.equals("All") && !status.equals(currentFilter)) continue;

            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.HORIZONTAL);
            card.setPadding(20, 20, 20, 20);
            card.setBackgroundResource(R.drawable.card_bg);

            LinearLayout.LayoutParams params =
                    new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT);
            params.setMargins(0, 0, 0, 24);
            card.setLayoutParams(params);

            View bar = new View(this);
            bar.setLayoutParams(new LinearLayout.LayoutParams(12,
                    LinearLayout.LayoutParams.MATCH_PARENT));
            bar.setBackgroundColor(color);

            LinearLayout content = new LinearLayout(this);
            content.setOrientation(LinearLayout.VERTICAL);
            content.setPadding(20, 0, 0, 0);

            TextView name = new TextView(this);
            name.setText(road);
            name.setTextColor(0xFFFFFFFF);
            name.setTextSize(16f);

            TextView values = new TextView(this);
            values.setText("Yesterday: " + String.format("%.2f", yesterdayAvg)
                    + " → Today: " + String.format("%.2f", todayAvg));
            values.setTextColor(0xFF9CA3AF);

            TextView stat = new TextView(this);
            stat.setText("Status: " + status);
            stat.setTextColor(color);

            content.addView(name);
            content.addView(values);
            content.addView(stat);

            card.addView(bar);
            card.addView(content);

            container.addView(card);
        }

        if (container.getChildCount() == 0) {
            TextView empty = new TextView(this);
            empty.setText("No roads found");
            empty.setTextColor(0xFF9CA3AF);
            empty.setPadding(20,20,20,20);
            container.addView(empty);
        }
    }
}