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
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.core.view.GravityCompat;

import com.google.android.material.navigation.NavigationView;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import android.content.Intent;
import android.view.Gravity; // only if you still use Gravity.LEFT somewhere

public class PerformanceActivity extends AppCompatActivity {

    private DatabaseReference databaseRef;
    private LinearLayout container;

    private String currentFilter = "All";
    private String searchQuery = "";
    private String currentSort = "WORST";

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


        // 🔹 Drawer
        DrawerLayout drawerLayout = findViewById(R.id.drawer_layout_stats);
        NavigationView navView = findViewById(R.id.nav_view_stats);
        Button menuBtn = findViewById(R.id.menuBtnStats);

// ✅ Correct selected item
        navView.setCheckedItem(R.id.nav_performance);

// Open drawer
        menuBtn.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));

// Drawer navigation
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
                return true; // already here
            }

            drawerLayout.closeDrawers();
            return true;
        });

        navView.setCheckedItem(R.id.nav_performance);
        menuBtn.setOnClickListener(v -> drawerLayout.openDrawer(Gravity.LEFT));

        BottomNavigationView bottomNav = findViewById(R.id.bottomNavigation);

        bottomNav.setSelectedItemId(R.id.nav_performance);

        bottomNav.setOnItemSelectedListener(item -> {

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
                return true;
            }

            return true;
        });



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

        // 🔽 SORT
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

                if (selected.equals("A-Z")) currentSort = "ALPHA";
                else if (selected.equals("Recent")) currentSort = "RECENT";
                else currentSort = "WORST";

                renderUI();
            }
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // FILTERS
        findViewById(R.id.filterAll).setOnClickListener(v -> { currentFilter = "All"; renderUI(); });
        findViewById(R.id.filterImproved).setOnClickListener(v -> { currentFilter = "Improved"; renderUI(); });
        findViewById(R.id.filterSame).setOnClickListener(v -> { currentFilter = "Same"; renderUI(); });
        findViewById(R.id.filterDegraded).setOnClickListener(v -> { currentFilter = "Degraded"; renderUI(); });
        findViewById(R.id.filterNew).setOnClickListener(v -> {
            currentFilter = "New Data";
            renderUI();
        });

        findViewById(R.id.filterNoRecent).setOnClickListener(v -> {
            currentFilter = "No Recent Data";
            renderUI();
        });



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

                // 🔥 2-DAY WINDOW FIX
                long currentStart = now - (2 * oneDay);
                long previousStart = now - (4 * oneDay);

                for (DataSnapshot data : snapshot.getChildren()) {

                    Double mag = data.child("mag").getValue(Double.class);
                    String road = data.child("road").getValue(String.class);
                    Long time = data.child("time").getValue(Long.class);

                    if (mag == null || road == null || time == null) continue;

                    if (time >= currentStart) {
                        todayData.putIfAbsent(road, new ArrayList<>());
                        todayData.get(road).add(mag);
                    }
                    else if (time >= previousStart) {
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

        // SORTING
        if (currentSort.equals("ALPHA")) {
            roads.sort(String::compareToIgnoreCase);
        } else if (currentSort.equals("RECENT")) {
            roads.sort((a, b) ->
                    Double.compare(getAverage(todayData.get(b)), getAverage(todayData.get(a)))
            );
        } else {
            roads.sort((a, b) ->
                    Double.compare(
                            getAverage(todayData.get(b)) - getAverage(yesterdayData.get(b)),
                            getAverage(todayData.get(a)) - getAverage(yesterdayData.get(a))
                    )
            );
        }

        for (String road : roads) {

            if (!road.toLowerCase().contains(searchQuery)) continue;

            boolean hasCurrent = todayData.containsKey(road);
            boolean hasPrevious = yesterdayData.containsKey(road);

            double todayAvg = getAverage(todayData.get(road));
            double yesterdayAvg = getAverage(yesterdayData.get(road));

            String status;
            int color;
            double threshold = 0.5;

            // 🔥 FIXED LOGIC
            if (hasCurrent && hasPrevious) {

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

            } else if (hasPrevious && !hasCurrent) {

                status = "No Recent Data";
                color = 0xFF9CA3AF;

            } else if (hasCurrent && !hasPrevious) {

                status = "New Data";
                color = 0xFF60A5FA;

            } else {

                status = "No Data";
                color = 0xFF4B5563;
            }

            if (!currentFilter.equals("All") && !status.equals(currentFilter)) continue;

            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.HORIZONTAL);
            card.setPadding(20, 20, 20, 20);
            card.setBackgroundResource(R.drawable.card_bg);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
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

            if (hasCurrent && hasPrevious) {
                values.setText("Prev: " + String.format("%.2f", yesterdayAvg)
                        + " → Now: " + String.format("%.2f", todayAvg));
            } else if (hasPrevious) {
                values.setText("Prev: " + String.format("%.2f", yesterdayAvg) + " → No recent data");
            } else if (hasCurrent) {
                values.setText("New readings: " + String.format("%.2f", todayAvg));
            } else {
                values.setText("No data available");
            }

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
            card.setOnClickListener(v -> {
                Intent intent = new Intent(this, RoadDetailActivity.class);
                intent.putExtra("roadName", road);
                startActivity(intent);
            });
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