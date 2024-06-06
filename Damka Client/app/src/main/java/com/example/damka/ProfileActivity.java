package com.example.damka;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ProfileActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
        this.getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);

        TextView usernameValue = findViewById(R.id.username_value);
        TextView userIdValue = findViewById(R.id.userId_value);

        RecyclerView elementsRecyclerView = findViewById(R.id.profiles_elements_recycler_view);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        elementsRecyclerView.setLayoutManager(layoutManager);

        Intent intent = getIntent();
        User user = intent.getParcelableExtra("user_profile");

        Map<String, String> elemetnsMap = new LinkedHashMap<>();
        HashMap<String, Double> ratingMap = new HashMap<>();
        if (user != null) {
            usernameValue.setText(user.getUsername());
            userIdValue.setText(String.format("#%s", user.getUserID()));
            if (!user.getPrivacy()) {
                elemetnsMap.put("friends", String.valueOf(user.getFriendsCount()));
                elemetnsMap.put("rating", String.valueOf((int) user.getRating()));
                elemetnsMap.put("best rating", String.valueOf((int) user.getBestRating()));
                elemetnsMap.put("games played", String.valueOf(user.getGamesPlayed()));
                elemetnsMap.put("Wins", String.valueOf(user.getWins()));
                elemetnsMap.put("Losses", String.valueOf(user.getLosses()));
                elemetnsMap.put("Draws", String.valueOf(user.getDraws()));
                ratingMap = user.getEloRatingMap();
            } else {
                elemetnsMap.put("private", "true");
                elemetnsMap.put("rating", String.valueOf((int) user.getRating()));
            }
        }

        profileAdapter itemAdapter = new profileAdapter(elemetnsMap);
        elementsRecyclerView.setAdapter(itemAdapter);


        GraphView graph = findViewById(R.id.ratings_graph_view);
        HashMap<Float, Float> dataMap = convertAndSortHashMap(ratingMap);
        graph.setData(dataMap);

        userIdValue.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Copy text to clipboard
                copyToClipboard(userIdValue.getText().toString());
                // Show toast indicating text is copied
                Toast.makeText(ProfileActivity.this, "User ID copied", Toast.LENGTH_SHORT).show();
            }
        });

        Button close = findViewById(R.id.close_profile);
        close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    private void copyToClipboard(String text) {
        text = text.substring(1);
        // Get clipboard manager
        ClipboardManager clipboardManager = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        // Create clip data
        ClipData clipData = ClipData.newPlainText("userId", text);
        // Set clip data to clipboard
        clipboardManager.setPrimaryClip(clipData);
    }

    private static HashMap<Float, Float> convertAndSortHashMap(HashMap<String, Double> timeRatingMap) {
        // Create a list of map entries
        List<Map.Entry<String, Double>> entryList = new ArrayList<>(timeRatingMap.entrySet());

        // Define the date format
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());

        // Sort the entries by the timestamp
        entryList.sort(new Comparator<Map.Entry<String, Double>>() {
            @Override
            public int compare(Map.Entry<String, Double> e1, Map.Entry<String, Double> e2) {
                try {
                    Date date1 = sdf.parse(e1.getKey());
                    Date date2 = sdf.parse(e2.getKey());
                    assert date1 != null;
                    return date1.compareTo(date2);
                } catch (ParseException e) {
                    e.printStackTrace();
                    return 0;
                }
            }
        });

        // Create the new HashMap with sequential float keys
        HashMap<Float, Float> floatTimeRatingMap = new HashMap<>();
        float index = 0.0f;
        for (Map.Entry<String, Double> entry : entryList) {
            floatTimeRatingMap.put(index, entry.getValue().floatValue());
            index += 1.0f;
        }

        return floatTimeRatingMap;
    }
}