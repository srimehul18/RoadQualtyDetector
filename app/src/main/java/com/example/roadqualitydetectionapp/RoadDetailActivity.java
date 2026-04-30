package com.example.roadqualitydetectionapp;

import android.os.Bundle;
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

                double sum = 0;
                double max = 0;
                int count = 0;
                long lastTime = 0;

                String lastDate = "";

                for (DataSnapshot data : snapshot.getChildren()) {

                    String road = data.child("road").getValue(String.class);
                    Double mag = data.child("mag").getValue(Double.class);
                    Long time = data.child("time").getValue(Long.class);

                    if (road == null || mag == null || time == null) continue;
                    if (!road.equals(roadName)) continue;

                    // 📊 stats
                    sum += mag;
                    count++;
                    if (mag > max) max = mag;
                    if (time > lastTime) lastTime = time;

                    // 📅 date grouping
                    String dateOnly = new SimpleDateFormat("dd MMM", Locale.getDefault())
                            .format(new Date(time));

                    if (!dateOnly.equals(lastDate)) {
                        lastDate = dateOnly;

                        TextView dateHeader = new TextView(RoadDetailActivity.this);
                        dateHeader.setText("📅 " + dateOnly);
                        dateHeader.setTextColor(0xFFFFFFFF);
                        dateHeader.setTextSize(16f);
                        dateHeader.setPadding(20, 30, 20, 10);
                        container.addView(dateHeader);
                    }

                    // 🎨 color coding
                    int color;
                    if (mag > 15) color = 0xFFE53935;      // red
                    else if (mag > 13) color = 0xFFFBBF24; // yellow
                    else color = 0xFF22C55E;               // green

                    // 🧾 item card
                    LinearLayout item = new LinearLayout(RoadDetailActivity.this);
                    item.setOrientation(LinearLayout.VERTICAL);
                    item.setPadding(24, 20, 24, 20);
                    item.setBackgroundResource(R.drawable.card_bg);

                    LinearLayout.LayoutParams params =
                            new LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.MATCH_PARENT,
                                    LinearLayout.LayoutParams.WRAP_CONTENT);
                    params.setMargins(0, 0, 0, 16);
                    item.setLayoutParams(params);

                    String fullTime = new SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault())
                            .format(new Date(time));

                    TextView magText = new TextView(RoadDetailActivity.this);
                    magText.setText("Magnitude: " + String.format("%.2f", mag));
                    magText.setTextColor(color);
                    magText.setTextSize(15f);

                    TextView timeText = new TextView(RoadDetailActivity.this);
                    timeText.setText(fullTime);
                    timeText.setTextColor(0xFF9CA3AF);
                    timeText.setTextSize(13f);

                    item.addView(magText);
                    item.addView(timeText);

                    container.addView(item);
                }

                // 🧾 SUMMARY + INSIGHT
                if (count > 0) {

                    double avg = sum / count;

                    String lastSeen = new SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault())
                            .format(new Date(lastTime));

                    // Summary card
                    LinearLayout summary = new LinearLayout(RoadDetailActivity.this);
                    summary.setOrientation(LinearLayout.VERTICAL);
                    summary.setPadding(30, 30, 30, 30);
                    summary.setBackgroundResource(R.drawable.card_bg);

                    LinearLayout.LayoutParams params =
                            new LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.MATCH_PARENT,
                                    LinearLayout.LayoutParams.WRAP_CONTENT);
                    params.setMargins(0, 0, 0, 24);
                    summary.setLayoutParams(params);

                    TextView stats = new TextView(RoadDetailActivity.this);
                    stats.setText("Avg: " + String.format("%.2f", avg)
                            + "   |   Max: " + String.format("%.2f", max)
                            + "\nReadings: " + count
                            + "   |   Last: " + lastSeen);
                    stats.setTextColor(0xFFFFFFFF);
                    stats.setTextSize(15f);

                    // 🧠 Insight
                    String insight;
                    if (avg > 15)
                        insight = "⚠️ Road condition is poor (high vibrations)";
                    else if (avg > 13)
                        insight = "⚠️ Moderate road quality";
                    else
                        insight = "✅ Road is smooth and stable";

                    TextView insightText = new TextView(RoadDetailActivity.this);
                    insightText.setText(insight);
                    insightText.setTextColor(0xFF60A5FA);
                    insightText.setTextSize(14f);
                    insightText.setPadding(0, 10, 0, 0);

                    summary.addView(stats);
                    summary.addView(insightText);

                    // 🔥 add summary at TOP
                    container.addView(summary, 0);
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {}
        });
    }
}