package com.example.roadqualitydetectionapp;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.*;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class RoadDetailActivity extends AppCompatActivity {

    private DatabaseReference databaseRef;
    private LinearLayout container;
    private String roadName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_road_detail);

        container = findViewById(R.id.detailContainer);

        roadName = getIntent().getStringExtra("roadName");

        databaseRef = FirebaseDatabase.getInstance().getReference("road_data_v2");

        loadData();
    }

    private void loadData() {

        databaseRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {

                container.removeAllViews();

                for (DataSnapshot data : snapshot.getChildren()) {

                    String road = data.child("road").getValue(String.class);
                    Double mag = data.child("mag").getValue(Double.class);
                    Long time = data.child("time").getValue(Long.class);

                    if (road == null || mag == null || time == null) continue;
                    if (!road.equals(roadName)) continue;

                    // 🔥 FORMAT TIME
                    String date = new SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault())
                            .format(new Date(time));

                    // 🔥 MAIN ROW (BAR + CARD)
                    LinearLayout row = new LinearLayout(RoadDetailActivity.this);
                    row.setOrientation(LinearLayout.HORIZONTAL);

                    LinearLayout.LayoutParams rowParams =
                            new LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.MATCH_PARENT,
                                    LinearLayout.LayoutParams.WRAP_CONTENT);
                    rowParams.setMargins(0, 0, 0, 16);
                    row.setLayoutParams(rowParams);

                    // 🔴🟡🟢 COLOR BAR
                    View bar = new View(RoadDetailActivity.this);

                    LinearLayout.LayoutParams barParams =
                            new LinearLayout.LayoutParams(12,
                                    LinearLayout.LayoutParams.MATCH_PARENT);

                    bar.setLayoutParams(barParams);

                    int color;
                    if (mag >= 16) {
                        color = 0xFFE53935; // 🔴 Dangerous
                    } else if (mag >= 13.3) {
                        color = 0xFFFBBF24; // 🟡 Moderate
                    } else {
                        color = 0xFF22C55E; // 🟢 Smooth
                    }

                    bar.setBackgroundColor(color);

                    // CARD CONTENT
                    LinearLayout item = new LinearLayout(RoadDetailActivity.this);
                    item.setOrientation(LinearLayout.VERTICAL);
                    item.setPadding(24, 20, 24, 20);
                    item.setBackgroundResource(R.drawable.card_bg);

                    LinearLayout.LayoutParams itemParams =
                            new LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.MATCH_PARENT,
                                    LinearLayout.LayoutParams.WRAP_CONTENT);
                    itemParams.setMargins(16, 0, 0, 0);
                    item.setLayoutParams(itemParams);

                    // 🔹 MAG TEXT
                    TextView magText = new TextView(RoadDetailActivity.this);
                    magText.setText("Magnitude: " + String.format(Locale.getDefault(), "%.2f", mag));
                    magText.setTextColor(0xFFFFFFFF);
                    magText.setTextSize(15f);

                    // 🔹 TIME TEXT
                    TextView timeText = new TextView(RoadDetailActivity.this);
                    timeText.setText(date);
                    timeText.setTextColor(0xFF9CA3AF);
                    timeText.setTextSize(13f);

                    // ADD TEXT
                    item.addView(magText);
                    item.addView(timeText);

                    // ADD BAR + CARD
                    row.addView(bar);
                    row.addView(item);

                    // ADD TO SCREEN
                    container.addView(row);
                }

                // 🟡 EMPTY STATE
                if (container.getChildCount() == 0) {
                    TextView empty = new TextView(RoadDetailActivity.this);
                    empty.setText("No data available");
                    empty.setTextColor(0xFF9CA3AF);
                    empty.setPadding(24, 24, 24, 24);
                    container.addView(empty);
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {}
        });
    }
}