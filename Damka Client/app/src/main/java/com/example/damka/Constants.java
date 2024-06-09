package com.example.damka;

import java.util.HashMap;
import java.util.Map;

public class Constants {
    public static final String version = "v0.1.2";
    public static final String serverIP = "192.168.68.106";
    public static final int serverPort = 9999;

    public static final String currentMail = "current_mail";

    public static final int LOGIN_CODE = 2;
    public static final int SIGNUP_CODE = 3;
    public static final int LOGOUT_CODE = 4;

    public static final int ADD_FRIEND_CODE = 5;
    public static final int ACCEPT_FRIEND_CODE = 6;
    public static final int DENY_FRIEND_CODE = 7;
    public static final int REMOVE_FRIEND_CODE = 8;
    public static final int GET_FRIENDS_LIST_CODE = 9;
    public static final int GET_USER_PROFILE_CODE = 10;
    public static final int NOTIFICATION_CODE = 11;
    public static final int GET_NOTIFICATION_CODE = 12;
    public static final int GET_LEADERBOARD_CODE = 13;
    public static final int UPDATE_USER_DATA = 14;

    public static final int FIND_GAME_CODE = 15;
    public static final int INVITE_CODE = 16;
    public static final int WAIT_FOR_OPPONENT = 17;
    public static final int GAME_DETAILS_CODE = 18;
    public static final int READY_CODE = 19;
    public static final int START_GAME_CODE = 20;
    public static final int LEAVE_GAME_CODE = 21;

    public static final int GAME_END_CODE = 22;
    public static final int OFFER_DRAW_CODE = 23;
    public static final int MOVE_CODE = 24;
    public static final int UPDATE_GAME_STATE_CODE = 25;
    public static final int FAILED_INVITE_CODE = 26;


    public static final Map<Integer, String> NotificationTypes = new HashMap<Integer, String>() {{
        put(1, "Friend Request");
        put(2, "Friend Accept");
        put(3, "Delete friend");
        put(4, "Game invite");
        put(5, "Game notification");
    }};

    public static final int BOARD_LENGTH = 8;
    public static final char WHITE_SOLDIER = 'w';
    public static final char BLACK_SOLDIER = 'b';
    public static final char WHITE_QUEEN = 'q';
    public static final char BLACK_QUEEN = 'k';

    public static final String PREF_PLACE = "app_preferences";
    public static final String PREF_VOLUME = "volume";
    public static final String PREF_VIBRATION = "vibration";
}
