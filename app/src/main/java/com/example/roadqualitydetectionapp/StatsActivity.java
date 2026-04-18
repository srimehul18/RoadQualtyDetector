package com.example.roadqualitydetectionapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;
import com.google.firebase.database.*;

import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import android.content.Intent;

public class StatsActivity extends AppCompatActivity {

    private TextView totalText, dangerText, moderateText, roadsText;
    private DatabaseReference databaseRef;
    private PieChart pieChart;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stats);

        totalText = findViewById(R.id.totalText);
        dangerText = findViewById(R.id.dangerText);
        moderateText = findViewById(R.id.moderateText);
        roadsText = findViewById(R.id.roadsText);
        pieChart = findViewById(R.id.pieChart);

        DrawerLayout drawerLayout = findViewById(R.id.drawer_layout_stats);
        NavigationView navView = findViewById(R.id.nav_view_stats);
        navView.setCheckedItem(R.id.nav_stats);
        Button menuBtn = findViewById(R.id.menuBtnStats);

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

        databaseRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {

                int total = 0;
                int dangerous = 0;
                int moderate = 0;

                // 🔥 NEW: store magnitudes per road
                HashMap<String, ArrayList<Double>> roadMagnitudes = new HashMap<>();

                for (DataSnapshot data : snapshot.getChildren()) {

                    Double mag = data.child("mag").getValue(Double.class);
                    String road = data.child("road").getValue(String.class);

                    if (mag == null || road == null) continue;

                    total++;

                    if (mag >= 16) dangerous++;
                    else if (mag >= 13.3) moderate++;

                    // store magnitude
                    roadMagnitudes.putIfAbsent(road, new ArrayList<>());
                    roadMagnitudes.get(road).add(mag);
                }

                // 📊 PIE CHART
                ArrayList<PieEntry> entries = new ArrayList<>();

                if (dangerous > 0)
                    entries.add(new PieEntry(dangerous, "Dangerous"));

                if (moderate > 0)
                    entries.add(new PieEntry(moderate, "Moderate"));

                PieDataSet dataSet = new PieDataSet(entries, "");

                dataSet.setColors(
                        0xFFE53935,
                        0xFFFFC107
                );

                dataSet.setValueTextColor(0xFFFFFFFF);
                dataSet.setValueTextSize(14f);

                PieData pieData = new PieData(dataSet);

                pieChart.setData(pieData);
                pieChart.setUsePercentValues(false);
                pieChart.setDrawHoleEnabled(false);
                pieChart.setEntryLabelColor(0xFFFFFFFF);
                pieChart.setEntryLabelTextSize(12f);
                pieChart.getDescription().setEnabled(false);
                pieChart.getLegend().setTextColor(0xFFFFFFFF);
                pieChart.invalidate();

                // 📊 COUNTERS
                totalText.setText(String.valueOf(total));
                dangerText.setText(String.valueOf(dangerous));
                moderateText.setText(String.valueOf(moderate));

                // 🔥 CALCULATE SCORE (ADVANCED)
                HashMap<String, Double> roadScore = new HashMap<>();

                for (String road : roadMagnitudes.keySet()) {

                    ArrayList<Double> mags = roadMagnitudes.get(road);

                    double sum = 0;
                    for (double m : mags) sum += m;

                    double avg = mags.size() == 0 ? 0 : sum / mags.size();

                    int count = mags.size();

                    // 🔥 ADVANCED SCORE
                    double score = avg * Math.log(count + 1);

                    roadScore.put(road, score);
                }


                ArrayList<Map.Entry<String, Double>> list =
                        new ArrayList<>(roadScore.entrySet());

                list.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));

                StringBuilder builder = new StringBuilder();

                builder.append("Based on average magnitude and no. of bumps detected)\n\n");

                int limit = Math.min(5, list.size());

                for (int i = 0; i < limit; i++) {

                    String road = list.get(i).getKey();
                    double score = list.get(i).getValue();

                    ArrayList<Double> mags = roadMagnitudes.get(road);

                    double sum = 0;
                    for (double m : mags) sum += m;

                    double avg = mags.size() == 0 ? 0 : sum / mags.size();

                    builder.append((i + 1)).append(". ")
                            .append(road)
                            .append("\n   Avg: ")
                            .append(String.format("%.2f", avg))
                            .append(" | Count: ")
                            .append(mags.size())
                            .append("\n   Score: ")
                            .append(String.format("%.2f", score))
                            .append("\n\n");
                }

                roadsText.setText(builder.toString());
            }

            @Override
            public void onCancelled(DatabaseError error) {}
        });
    }
}