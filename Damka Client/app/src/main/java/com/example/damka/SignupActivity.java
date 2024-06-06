package com.example.damka;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.app.DatePickerDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Calendar;
import java.util.Locale;

public class SignupActivity extends AppCompatActivity implements WebSocketListener{
    private EditText usernameText;
    private EditText mailText;
    private EditText passwordText;
    private EditText confirmPasswordText;
    private EditText birthdateText;
    private Button signupButton;
    private TextView loginLink;
    private ToggleButton togglePasswordButton;
    private TextView errorText;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        this.usernameText = findViewById(R.id.username_input);
        this.mailText = findViewById(R.id.mail_input);
        this.passwordText = findViewById(R.id.password_input);
        this.confirmPasswordText = findViewById(R.id.confirm_password_input);
        this.birthdateText = findViewById(R.id.birthdate_input);

        this.signupButton = findViewById(R.id.signup_button);
        this.loginLink = findViewById(R.id.login_link);
        this.togglePasswordButton = findViewById(R.id.toggle_password_visibility);
        this.errorText = findViewById(R.id.error_text);
        Communicator.getInstance().setListener(this);

        togglePasswordButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                togglePasswordVisibility(isChecked);
            }
        });

        birthdateText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDatePickerDialog();
            }
        });

        signupButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                trySignup();
            }
        });

        loginLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                moveToLogin();
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

    private void showDatePickerDialog() {
        final Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                        // Format selected date as dd/mm/yyyy
                        String selectedDate = String.format(Locale.getDefault(), "%02d/%02d/%04d", dayOfMonth, month + 1, year);
                        birthdateText.setText(selectedDate);
                    }
                }, year, month, dayOfMonth);

        datePickerDialog.show();
    }

    // change password visibility
    private void togglePasswordVisibility(boolean isChecked) {
        if (isChecked) {
            // Show Password
            passwordText.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            confirmPasswordText.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
        } else {
            // Hide Password
            passwordText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            confirmPasswordText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        }
        // Move cursor to the end of the input field
        passwordText.setSelection(passwordText.getText().length());
        confirmPasswordText.setSelection(confirmPasswordText.getText().length());
    }

    /*
    collect the data from the input fields
    and try to sign as new user to the server
    */
    private void trySignup() {
        String username = this.usernameText.getText().toString();
        String mail = this.mailText.getText().toString();
        String password = this.passwordText.getText().toString();
        String confirmPassword = this.confirmPasswordText.getText().toString();
        String birthdate = this.birthdateText.getText().toString();

        errorText.setBackground(null); //clean errors field
        errorText.setText("\n");
        disableInteraction();

        // check both fields have values
        if (username.isEmpty() || mail.isEmpty() || password.isEmpty() || confirmPassword.isEmpty() || birthdate.isEmpty())
        {
            String msg = "Must enter all fields";
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
            enableInteraction();
            return;
        }

        if (!password.equals(confirmPassword))
        {
            String msg = "Passwords don't match";
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
            enableInteraction();
            return;
        }

        JSONObject signupParams = new JSONObject();
        try {
            signupParams.put("code", Constants.SIGNUP_CODE);

            signupParams.put("username",username);
            signupParams.put("mail", mail);
            signupParams.put("password", password);
            signupParams.put("confirm_password", confirmPassword);
            signupParams.put("birthdate", birthdate);

            UserCredentialsSaver.saveData(this, Constants.currentMail, mail);
            Communicator.getInstance().sendMessage(signupParams.toString());
        } catch (JSONException e) {
            enableInteraction();
        }
    }

    /*
    move to login activity
    if the user already have an account
    */
    private void moveToLogin() {
        Intent intent = new Intent(SignupActivity.this, LoginActivity.class);
        startActivity(intent);
        finish();
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

                // Start menu activity
                Intent intent = new Intent(SignupActivity.this, MenuActivity.class);
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
            Toast.makeText(SignupActivity.this, "Connection disconnected", Toast.LENGTH_SHORT).show();

            // Change activities
            Intent intent = new Intent(SignupActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        });
    }

    @Override
    public void onError(Exception ex) {
        // Handle WebSocket error
    }

    // Disable all UI elements
    private void disableInteraction() {
        usernameText.setEnabled(false);
        mailText.setEnabled(false);
        passwordText.setEnabled(false);
        confirmPasswordText.setEnabled(false);
        birthdateText.setEnabled(false);
        signupButton.setEnabled(false);
        togglePasswordButton.setEnabled(false);
        loginLink.setEnabled(false);
    }

    // Enable all UI elements
    private void enableInteraction() {
        usernameText.setEnabled(true);
        mailText.setEnabled(true);
        passwordText.setEnabled(true);
        confirmPasswordText.setEnabled(true);
        birthdateText.setEnabled(true);
        signupButton.setEnabled(true);
        togglePasswordButton.setEnabled(true);
        loginLink.setEnabled(true);
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