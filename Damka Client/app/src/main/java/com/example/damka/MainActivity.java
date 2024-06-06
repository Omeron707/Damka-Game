package com.example.damka;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;


public class MainActivity extends AppCompatActivity implements WebSocketListener {

    private TextView screenText;
    private ProgressBar progressBar;
    private Button retryButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_loading_animation);

        screenText = findViewById(R.id.loading_text);
        progressBar = findViewById(R.id.circle_progress_bar);
        retryButton = findViewById(R.id.retry_button);

        Communicator.getInstance().setListener(this);

        retryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setLoading();
                Communicator.getInstance().connect();
            }
        });

        // Create an OnBackPressedCallback to handle the back button press
        OnBackPressedCallback backPressedCallback = new OnBackPressedCallback(true /* enabled by default */) {
            @Override
            public void handleOnBackPressed() {
                backPressed();
            }
        };
        getOnBackPressedDispatcher().addCallback(this, backPressedCallback);

        setLoading();
        Communicator.getInstance().connect();
    }

    /*
   set the UI elements to loading state
   progressbar - spinning circle
   text - Loading
   button - off
    */
    private void setLoading() {
        progressBar.setVisibility(View.VISIBLE);
        screenText.setText(R.string.loading);
        retryButton.setVisibility(View.GONE);
    }

    /*
   try to load user credentials
    */
    private boolean loadCredentials() {
        try {
            String mail = UserCredentialsSaver.getData(this, UserCredentialsSaver.USER_MAIL);
            String password = UserCredentialsSaver.getData(this, UserCredentialsSaver.USER_PASSWORD);
            if (mail != null && password != null) {
                Communicator.getInstance().sendLogin(mail, password);
                return true;
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return false;
    }

    /*
    set the UI elements to failed connection state
    progressbar - off
    text - failed to connect
    button - on (to enable retry)
     */
    private void setFailedToConnect() {
        progressBar.setVisibility(View.GONE);
        screenText.setText(R.string.failed_connection);
        retryButton.setVisibility(View.VISIBLE);
    }

    @Override
    public void onWebSocketConnected() {
        if (!loadCredentials()) {
            // WebSocket connected
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        }
    }

    @Override
    public void onMessageReceived(String message) {
        // Handle received message from the server
        try {
            JSONObject result = new JSONObject(message);
            int code = result.getInt("code");
            if (code == 0) {
                // Login successful
                String userID = result.getString("userID");
                String username = result.getString("username");
                double rating = result.getDouble("rating");
                boolean privacy = result.getBoolean("private");
                String currentMail = result.getString("mail");
                User loggedUser = new User(userID, username, rating, 1, privacy);

                // Start menu activity
                Intent intent = new Intent(MainActivity.this, MenuActivity.class);
                intent.putExtra("logged_user", loggedUser);
                intent.putExtra("current_mail", currentMail);
                startActivity(intent);
                finish();
            } else {
                Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                startActivity(intent);
                finish();
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onWebSocketClosed(String reason) {
        runOnUiThread(this::setFailedToConnect);
    }

    @Override
    public void onError(Exception ex) {
        runOnUiThread(this::setFailedToConnect);
    }

    /*
    override the normal phones back button behavior
    to show a dialog (want to exit? yes/ no)
    before closing the activity
    */
    public void backPressed() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Are you sure you want to exit?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    // Close the activity and disconnect the server
                    Communicator.getInstance().removeListener();
                    Communicator.getInstance().disconnect();
                    finish();
                })
                .setNegativeButton("No", (dialog, which) -> dialog.dismiss());
        AlertDialog dialog = builder.create();
        dialog.show();
    }
}