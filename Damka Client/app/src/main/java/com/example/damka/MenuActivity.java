package com.example.damka;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.Base64;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.snackbar.Snackbar;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class MenuActivity extends AppCompatActivity implements WebSocketListener {

    private MediaPlayer mediaPlayer;
    private User loggedUser;
    private String loggedUserMail;
    private FriendsFragment friendsPage;
    private HomeFragment homePage;
    private LeaderBoardFragment leaderBoardPage;
    private boolean isReturnFromGame = false;

    private ActivityResultLauncher<Intent> gameActivityLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);
        // hide phones navigation bar
        this.getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        View rootLayout = getWindow().getDecorView().getRootView();
        rootLayout.requestLayout();

        SharedPreferences sharedPreferences = getSharedPreferences(Constants.PREF_PLACE, Context.MODE_PRIVATE);
        int savedVolume = sharedPreferences.getInt(Constants.PREF_VOLUME, 50);

        this.mediaPlayer = MediaPlayer.create(this, R.raw.aylex_spring);
        mediaPlayer.setLooping(true);
        mediaPlayer.setVolume(savedVolume, savedVolume);
        mediaPlayer.start();

        List<User> friendsList = new ArrayList<>();
        List<User> friendsRequestList = new ArrayList<>();
        List<User> leaderboardList = new ArrayList<>();

        Intent intent = getIntent();
        loggedUser = intent.getParcelableExtra("logged_user");
        loggedUserMail = intent.getStringExtra("current_mail");

        Communicator.getInstance().setListener(this);

        ViewPager2 viewPager = findViewById(R.id.viewPager);
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);

        friendsPage = FriendsFragment.newInstance(friendsList, friendsRequestList);
        homePage = HomeFragment.newInstance(loggedUser, loggedUserMail);
        leaderBoardPage = LeaderBoardFragment.newInstance(leaderboardList);

        // Set up ViewPager adapter
        FragmentStateAdapter adapter = new FragmentStateAdapter(getSupportFragmentManager(), getLifecycle()) {
            @NonNull
            @Override
            public Fragment createFragment(int position) {
                switch (position) {
                    case 0:
                        return friendsPage;
                    case 1:
                        return homePage;
                    case 2:
                        return leaderBoardPage;
                }
                // default if something go wrong
                return homePage;
            }

            @Override
            public int getItemCount() {
                return 3;
            }

        };
        viewPager.setAdapter(adapter);
        viewPager.setCurrentItem(1, false);
        viewPager.setOffscreenPageLimit(2);
        bottomNavigationView.setSelectedItemId(getMenuItemId(1));

        // Set listener for ViewPager page changes
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                // Close the keyboard when user scrolls to a different page
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(viewPager.getWindowToken(), 0);

                // Update BottomNavigationView item when a page is selected in the ViewPager
                bottomNavigationView.setSelectedItemId(getMenuItemId(position));
            }

            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                // Unused
            }

            @Override
            public void onPageScrollStateChanged(int state) {
                // Unused
            }
        });

        bottomNavigationView.setOnItemSelectedListener(new BottomNavigationView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int itemID = item.getItemId();
                if (itemID == R.id.friends) {
                    viewPager.setCurrentItem(0);
                } else if (itemID == R.id.home) {
                    viewPager.setCurrentItem(1);
                } else if (itemID == R.id.leaderboard) {
                    viewPager.setCurrentItem(2);
                } else {
                    return false;
                }
                return true;
            }
        });

        // set up game activity launcher
        gameActivityLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                Communicator.getInstance().setListener(this);
                if (result.getResultCode() == Activity.RESULT_OK) {
                    Intent data = result.getData();
                    if (data != null) {
                        String res = data.getStringExtra("result");
                        Constants.GameStage stage = (Constants.GameStage)data.getSerializableExtra("stage");
                        // update friends list if played against a friend
                        if (Objects.equals(res, "end_friend"))
                        {
                            Communicator.getInstance().sendCode(Constants.GET_FRIENDS_LIST_CODE);
                        }
                        // update user profile unless game didn't start
                        if (stage != Constants.GameStage.LOOKING_FOR_GAME) {
                            this.isReturnFromGame = true;
                            Communicator.getInstance().sendGetProfile(this.loggedUser.getUserID());
                        }
                    }
                } else {
                    Communicator.getInstance().sendCode(Constants.GET_FRIENDS_LIST_CODE);
                }
            }
        );

        // Create an OnBackPressedCallback to handle the back button press
        OnBackPressedCallback backPressedCallback = new OnBackPressedCallback(true /* enabled by default */) {
            @Override
            public void handleOnBackPressed() {
                backPressed();
            }
        };
        getOnBackPressedDispatcher().addCallback(this, backPressedCallback);

        Communicator.getInstance().sendCode(Constants.GET_NOTIFICATION_CODE);
        Communicator.getInstance().sendCode(Constants.GET_FRIENDS_LIST_CODE);
        Communicator.getInstance().sendCode(Constants.GET_LEADERBOARD_CODE);
    }

    private int getMenuItemId(int position) {
        switch (position) {
            case 0:
                return R.id.friends;
            case 1:
                return R.id.home;
            case 2:
                return R.id.leaderboard;
        }
        return R.id.home; // Default to home
    }

    public void FriendNotificationClicked(String userID) {
        ViewPager2 viewPager = findViewById(R.id.viewPager);
        viewPager.setCurrentItem(0);
        friendsPage.highlightFriend(userID);
    }

    public void FriendRequestNotificationClicked(String userID) {
        ViewPager2 viewPager = findViewById(R.id.viewPager);
        viewPager.setCurrentItem(0);
        friendsPage.highlightFriendRequest(userID);
    }

    public void startGameActivity() {
        Intent intent = new Intent(MenuActivity.this, GameActivity.class);
        gameActivityLauncher.launch(intent);
    }

    public void startGameActivityFriendly(User opponent, int gameID) {
        Intent intent = new Intent(MenuActivity.this, GameActivity.class);
        intent.putExtra("opponent", opponent);
        intent.putExtra("gameID", gameID);
        gameActivityLauncher.launch(intent);
    }

    @Override
    public void onWebSocketConnected() {
    }

    @Override
    public void onMessageReceived(String message) {
        runOnUiThread(() -> {
            try {
                JSONObject result = new JSONObject(message);
                int code = result.getInt("code");
                switch (code) {
                    case Constants.LOGOUT_CODE:
                        logout(result);
                        break;
                    case Constants.ADD_FRIEND_CODE:
                        responseAddFriend(result);
                        break;
                    case Constants.ACCEPT_FRIEND_CODE:
                        responseAcceptFriend(result);
                        break;
                    case Constants.DENY_FRIEND_CODE:
                        responseDenyFriend(result);
                        break;
                    case Constants.REMOVE_FRIEND_CODE:
                        responseRemoveFriend(result);
                        break;
                    case Constants.GET_FRIENDS_LIST_CODE:
                        updateFriends(result);
                        break;
                    case Constants.GET_USER_PROFILE_CODE:
                        showProfile(result);
                        break;
                    case Constants.NOTIFICATION_CODE:
                        getNotification(result);
                        break;
                    case Constants.GET_NOTIFICATION_CODE:
                        responseGetNotificationsList(result);
                        break;
                    case Constants.GET_LEADERBOARD_CODE:
                            responseGetLeaderboard(result);
                        break;
                    case Constants.UPDATE_USER_DATA:
                        responseUpdateData(result);
                        break;
                }
            } catch (JSONException e) {
                Toast.makeText(MenuActivity.this, "Json Error", Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            }
        });
    }

    @Override
    public void onWebSocketClosed(String reason) {
        // WebSocket connection closed
        runOnUiThread(() -> {
            Toast.makeText(MenuActivity.this, "Connection disconnected", Toast.LENGTH_SHORT).show();
            if (!Objects.equals(loggedUser.getUsername(), "Guest")) {
                Intent intent = new Intent(MenuActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
            }
        });
    }

    @Override
    public void onError(Exception ex) {
        runOnUiThread(() -> {
            String msg = "Error: " + ex.toString();
            Toast.makeText(MenuActivity.this, msg, Toast.LENGTH_SHORT).show();
        });
    }

    private void logout(JSONObject message) throws JSONException {
        if (message.getInt("success") == 0) {
            UserCredentialsSaver.clearAllData(this);
            Intent intent = new Intent(MenuActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        } else {
            Toast.makeText(this, "Failed to logout,", Toast.LENGTH_SHORT).show();
        }
    }

    private void responseAddFriend(JSONObject message) throws JSONException {
        String msg;
        if (message.getInt("success") == 0) {
            msg = "Send successfully";
        } else {
            msg = message.getString("error");
        }
        Toast.makeText(MenuActivity.this, msg, Toast.LENGTH_SHORT).show();
    }

    private void responseAcceptFriend(JSONObject message) throws JSONException {
        String msg;
        if (message.getInt("success") == 0) {
            msg = "Send successfully";
            Communicator.getInstance().sendCode(Constants.GET_FRIENDS_LIST_CODE);
        } else {
            msg = message.getString("error");
        }
        Toast.makeText(MenuActivity.this, msg, Toast.LENGTH_SHORT).show();
    }

    private void responseDenyFriend(JSONObject message) throws JSONException {
        String msg;
        if (message.getInt("success") == 0) {
            msg = "Denied successfully";
            Communicator.getInstance().sendCode(Constants.GET_FRIENDS_LIST_CODE);
        } else {
            msg = message.getString("error");
        }
        Toast.makeText(MenuActivity.this, msg, Toast.LENGTH_SHORT).show();
    }

    private void responseRemoveFriend(JSONObject message) throws JSONException {
        String msg;
        if (message.getInt("success") == 0) {
            msg = "Removed successfully";
            Communicator.getInstance().sendCode(Constants.GET_FRIENDS_LIST_CODE);
        } else {
            msg = message.getString("error");
        }
        Toast.makeText(MenuActivity.this, msg, Toast.LENGTH_SHORT).show();
    }

    private void updateFriends(JSONObject message ) throws JSONException {
        // extract friends
        List<User> friendsList = new ArrayList<>();
        List<User> requestsList = new ArrayList<>();
        JSONArray friendsJsonArray = message.getJSONArray("friends_list");

        // Iterate over the array and parse each user object
        for (int i = 0; i < friendsJsonArray.length(); i++) {
            JSONObject userObject = friendsJsonArray.getJSONObject(i);
            String username = userObject.getString("username");
            String userId = userObject.getString("userID");
            double rating = userObject.getDouble("rating");
            int online = userObject.getInt("online");
            boolean privacy = userObject.getBoolean("private");

            // Create a new User object and add it to the list
            User user = new User(userId, username, rating, online, privacy);
            friendsList.add(user);
        }
        //extract friend requests
        JSONArray requestsJsonArray = message.getJSONArray("request_friends_list");

        // Iterate over the array and parse each user object
        for (int i = 0; i < requestsJsonArray.length(); i++) {
            JSONObject userObject = requestsJsonArray.getJSONObject(i);
            String username = userObject.getString("username");
            String userId = userObject.getString("userID");
            double rating = userObject.getDouble("rating");
            int online = userObject.getInt("online");
            boolean privacy = userObject.getBoolean("private");

            // Create a new User object and add it to the list
            User user = new User(userId, username, rating, online, privacy);
            requestsList.add(user);
        }

        friendsPage.setFriendsList(friendsList, requestsList);
    }

    private void showProfile(JSONObject message) throws JSONException {
        JSONObject profile = message.getJSONObject("profile");
        User user;
        if (profile.getBoolean("private")) {
            user = new User(
                    profile.getString("userID"),
                    profile.getString("username"),
                    profile.getDouble("rating"),
                    profile.getInt("online"),
                    profile.getBoolean("private")
            );
        } else {
            user = User.parsUser(profile);
        }
        if (Objects.equals(user.getUserID(), this.loggedUser.getUserID())) {
            this.loggedUser.setRating(user.getRating());
            homePage.refresh();
            if (this.isReturnFromGame) {
                this.isReturnFromGame = false;
                return;
            }
        }
        Intent intent = new Intent(MenuActivity.this, ProfileActivity.class);
        intent.putExtra("user_profile", user);
        startActivity(intent);
    }

    private void getNotification(JSONObject message) throws JSONException {
        Notification notification = Notification.parseNotification(message);
        if (notification.getType() == 1 || notification.getType() == 2 || notification.getType() == 3) {
           Communicator.getInstance().sendCode(Constants.GET_FRIENDS_LIST_CODE);
        }
        if ((notification.getType() == 1 || notification.getType() == 2) && !Objects.equals(notification.getContent(), "")) {
            this.homePage.addInboxMessages(notification);
        }
        if (notification.getType() == 4) {
            gameInvite(notification);
        }
    }


    private void gameInvite(Notification notification) {
        String userID = notification.getSourceID();
        User opponent = this.friendsPage.getFriend(userID);
        if (opponent == null) {
            return;
        }

        int gameID = Integer.parseInt(notification.getContent());
        String inviteMessage = "Do you want to play against " + opponent.getUsername() + "?";

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(inviteMessage)
                .setPositiveButton("Yes", (dialog, which) -> this.startGameActivityFriendly(opponent, gameID))
                .setNegativeButton("No", (dialog, which) -> dialog.dismiss());
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void responseGetNotificationsList(JSONObject message) throws JSONException {
        List<Notification> notifications = new ArrayList<>();

        JSONArray notificationsJsonArray = message.getJSONArray("notifications");
        for (int i = 0; i < notificationsJsonArray.length(); i++) {
            JSONObject notificationsObject = notificationsJsonArray.getJSONObject(i);
            notifications.add(Notification.parseNotification(notificationsObject));
        }
        homePage.setInboxMessages(notifications);
    }

    private void responseGetLeaderboard(JSONObject message) throws JSONException {
        List<User> leaderboardUsers = new ArrayList<>();

        JSONArray leaderboardJsonArray = message.getJSONArray("leaderboard");
        for (int i = 0; i < leaderboardJsonArray.length(); i++) {
            JSONObject userObject = leaderboardJsonArray.getJSONObject(i);
            String username = userObject.getString("username");
            String userId = userObject.getString("userID");
            double rating = userObject.getDouble("rating");
            boolean privacy = userObject.getBoolean("private");

            // Create a new User object and add it to the list
            User user = new User(userId, username, rating, 0, privacy);
            leaderboardUsers.add(user);
        }
        leaderBoardPage.setLeaderboardList(leaderboardUsers);
    }

    private void responseUpdateData(JSONObject message) throws JSONException {
        if (message.getInt("success") == 0) {
            String username = message.getString("username");
            String mail = message.getString("mail");
            int privacy = message.getInt("private");
            if (!mail.equals("")) {
               loggedUserMail = mail;
            }
            if (!username.equals("")) {
                loggedUser.setUsername(username);
                homePage.refresh();
            }
            if (privacy != -1)
            {
                loggedUser.setPrivacy(privacy == 1);
            }
        } else {
            homePage.showSettingError(message.getString("error_field"), message.getString("error"));
        }
    }

    public void setMediaVolume(int progress) {
        float volume = progress / 100f;
        this.mediaPlayer.setVolume(volume, volume);
        SharedPreferences sharedPreferences = this.getSharedPreferences(Constants.PREF_PLACE, Context.MODE_PRIVATE);
        sharedPreferences.edit().putInt(Constants.PREF_VOLUME, progress).apply();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mediaPlayer != null && !mediaPlayer.isPlaying()) {
            mediaPlayer.start();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
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
                    // Close the activity
                    Communicator.getInstance().removeListener();
                    Communicator.getInstance().disconnect();
                    finish();
                })
                .setNegativeButton("No", (dialog, which) -> {
                    dialog.dismiss();
                });
        AlertDialog dialog = builder.create();
        dialog.show();
    }
}