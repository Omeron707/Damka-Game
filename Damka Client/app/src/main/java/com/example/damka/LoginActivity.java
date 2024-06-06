package com.example.damka;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;
import org.json.JSONException;
import org.json.JSONObject;


public class LoginActivity extends AppCompatActivity implements WebSocketListener {

    private EditText mailText;
    private EditText passwordText;
    private Button loginButton;
    private TextView signupLink;
    private ToggleButton togglePasswordButton;
    private TextView errorText;
    private CheckBox rememberMeBox;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        this.mailText = findViewById(R.id.mail_input);
        this.passwordText = findViewById(R.id.password_input);
        this.loginButton = findViewById(R.id.login_button);
        this.signupLink = findViewById(R.id.signup_link);
        this.togglePasswordButton = findViewById(R.id.toggle_password_visibility);
        this.errorText = findViewById(R.id.error_text);
        this.rememberMeBox = findViewById(R.id.toggle_remember_me);
        Communicator.getInstance().setListener(this);

        togglePasswordButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                togglePasswordVisibility(isChecked);
            }
        });

        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                tryLogin();
            }
        });

        signupLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                moveToSignup();
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
    }

    // change password visibility
    private void togglePasswordVisibility(boolean isChecked) {
            if (isChecked) {
                // Show Password
                passwordText.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            } else {
                // Hide Password
                passwordText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            }
            // Move cursor to the end of the input field
            passwordText.setSelection(passwordText.getText().length());
    }

    /*
    save mail and password for automatic future logins
     */
    private void saveCredentials() {
        UserCredentialsSaver.saveData(this, UserCredentialsSaver.USER_MAIL, mailText.getText().toString());
        UserCredentialsSaver.saveData(this, UserCredentialsSaver.USER_PASSWORD, passwordText.getText().toString());
    }

    /*
    collect the data from the input fields
    and try to connect to the server
    */
    private void tryLogin() {
        String mail = this.mailText.getText().toString();
        String password = this.passwordText.getText().toString();

        errorText.setBackground(null); //clean errors field
        errorText.setText("\n");
        disableInteraction();

        // check both fields have values
        if (mail.isEmpty() || password.isEmpty())
        {
            String msg = "Must enter all fields";
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
            enableInteraction();
            return;
        }
        try {
            Communicator.getInstance().sendLogin(mail, password);
        }
        catch (JSONException e) {
            enableInteraction();
        }
    }

    /*
    move to sign up activity
    if the user want to create new account
    */
    private void moveToSignup() {
        this.mailText.setText("");
        this.passwordText.setText("");
        Intent intent = new Intent(LoginActivity.this, SignupActivity.class);
        startActivity(intent);
        finish();
    }

    // Disable all UI elements
    private void disableInteraction() {
        mailText.setEnabled(false);
        passwordText.setEnabled(false);
        loginButton.setEnabled(false);
        signupLink.setEnabled(false);
        togglePasswordButton.setEnabled(false);
    }

    // Enable all UI elements
    private void enableInteraction() {
        mailText.setEnabled(true);
        passwordText.setEnabled(true);
        loginButton.setEnabled(true);
        signupLink.setEnabled(true);
        togglePasswordButton.setEnabled(true);
    }

    @Override
    public void onWebSocketConnected() {
        // WebSocket connected
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

                runOnUiThread(() -> {
                    if (this.rememberMeBox.isChecked()) {
                        saveCredentials();
                    }
                });

                // Start menu activity
                Intent intent = new Intent(LoginActivity.this, MenuActivity.class);
                intent.putExtra("logged_user", loggedUser);
                intent.putExtra("current_mail", currentMail);
                startActivity(intent);
                finish(); // Close LoginActivity
            } else {
                // Login failed
                String errorField = result.getString("error_field");
                String errorContent = result.getString("error");
                String errorMsg = "Field: " + errorField + "\nError: " + errorContent;
                runOnUiThread(() -> {
                    errorText.setText(errorMsg);
                    errorText.setBackgroundResource(R.drawable.errors_background);
                    enableInteraction();
                });
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onWebSocketClosed(String reason) {
        // WebSocket connection closed
        runOnUiThread(() -> {
            // Perform any actions needed before changing activities, such as displaying a toast message
            Toast.makeText(LoginActivity.this, "Connection disconnected", Toast.LENGTH_SHORT).show();

            // Change activities
            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        });
    }

    @Override
    public void onError(Exception ex) {
        runOnUiThread(() -> {
            String msg = "Error: " + ex.toString();
            Toast.makeText(LoginActivity.this, msg, Toast.LENGTH_SHORT).show();
        });
    }

    /*
    override the normal phones back button behavior
    to show a dialog (want to exit? yes/ no)
    before closing the activity
    */
    public void backPressed() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Are you sure you want to exit?")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Close the activity
                        Communicator.getInstance().removeListener();
                        Communicator.getInstance().disconnect();
                        finish();
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Dismiss the dialog
                        dialog.dismiss();
                    }
                });
        AlertDialog dialog = builder.create();
        dialog.show();
    }
}