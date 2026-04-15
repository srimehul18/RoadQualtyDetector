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

import java.util.HashMap;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.*;
import com.github.mikephil.charting.utils.ColorTemplate;
import java.util.ArrayList;

public class StatsActivity extends AppCompatActivity {

    private TextView totalText, dangerText, moderateText, roadsText;
    private DatabaseReference databaseRef;
    private PieChart pieChart;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stats);

        // ✅ UI
        totalText = findViewById(R.id.totalText);
        dangerText = findViewById(R.id.dangerText);
        moderateText = findViewById(R.id.moderateText);
        roadsText = findViewById(R.id.roadsText);
        pieChart = findViewById(R.id.pieChart);

        // ✅ Drawer
        DrawerLayout drawerLayout = findViewById(R.id.drawer_layout_stats);
        NavigationView navView = findViewById(R.id.nav_view_stats);
        Button menuBtn = findViewById(R.id.menuBtnStats);

        navView.inflateMenu(R.menu.menu_drawer);

        menuBtn.setOnClickListener(v -> {
            drawerLayout.openDrawer(Gravity.LEFT);
        });

        navView.setNavigationItemSelectedListener(item -> {

            int id = item.getItemId();

            if (id == R.id.nav_dashboard) {
                finish();
            }
            else if (id == R.id.nav_map) {
                startActivity(new Intent(StatsActivity.this, MapActivity.class));
            }
            else if (id == R.id.nav_stats) {
                // already here
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

                HashMap<String, Integer> roadCount = new HashMap<>();

                // ✅ FIRST: calculate values
                for (DataSnapshot data : snapshot.getChildren()) {

                    Double mag = data.child("mag").getValue(Double.class);
                    String road = data.child("road").getValue(String.class);

                    if (mag == null) continue;

                    total++;

                    if (mag >= 16) dangerous++;
                    else if (mag >= 13) moderate++;

                    if (road != null) {
                        int count = roadCount.containsKey(road) ? roadCount.get(road) : 0;
                        roadCount.put(road, count + 1);
                    }
                }

                // ✅ THEN: create chart using REAL values
                ArrayList<PieEntry> entries = new ArrayList<>();

                if (dangerous > 0)
                    entries.add(new PieEntry(dangerous, "Dangerous"));

                if (moderate > 0)
                    entries.add(new PieEntry(moderate, "Moderate"));

                PieDataSet dataSet = new PieDataSet(entries, "");

                dataSet.setColors(
                        0xFFE53935, // red
                        0xFFFFC107  // yellow
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

                pieChart.invalidate(); // 🔥 refresh

                // ✅ TEXT UI
                totalText.setText(String.valueOf(total));
                dangerText.setText(String.valueOf(dangerous));
                moderateText.setText(String.valueOf(moderate));

                ArrayList<HashMap.Entry<String, Integer>> list =
                        new ArrayList<>(roadCount.entrySet());

// 🔥 sort descending
                list.sort((a, b) -> b.getValue() - a.getValue());

// 🔥 build top 5
                StringBuilder builder = new StringBuilder();

                builder.append("Top 5 Worst Roads:\n\n");

                int limit = Math.min(5, list.size());

                for (int i = 0; i < limit; i++) {

                    String road = list.get(i).getKey();
                    int count = list.get(i).getValue();

                    builder.append((i + 1) + ". ")
                            .append(road)
                            .append(" → ")
                            .append(count)
                            .append("\n");
                }

                roadsText.setText(builder.toString());
            }

            @Override
            public void onCancelled(DatabaseError error) {}
        });
    }
}