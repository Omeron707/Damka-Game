package com.example.damka;

import android.os.Parcel;
import android.os.Parcelable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class User implements Parcelable {
    private final String userID;
    private String username;
    private double rating;
    private boolean privacy;
    private final int online;
    private final double bestRating;
    private final int friendsCount;
    private final int gamesPlayed;
    private final int wins;
    private final int losses;
    private final int draws;
    private final HashMap<String, Double> ratingsMap;

    private static HashMap<String, Double> parseRatingHistory(JSONArray ratingsArray) throws JSONException {
        HashMap<String, Double> ratingsMap = new HashMap<>();

        for (int i = 0; i < ratingsArray.length(); i++) {
            JSONObject ratingsObject = ratingsArray.getJSONObject(i);
            ratingsMap.put(ratingsObject.getString("timestamp"), ratingsObject.getDouble("rating"));
        }
        return ratingsMap;
    }

    public static User parsUser(JSONObject profile) throws JSONException {
        return new User(
                profile.getString("userID"),
                profile.getString("username"),
                profile.getDouble("rating"),
                profile.getInt("online"),
                profile.getBoolean("private"),
                profile.getDouble("best_rating"),
                profile.getInt("friends_amount"),
                profile.getInt("games_played"),
                profile.getInt("wins"),
                profile.getInt("losses"),
                profile.getInt("draws"),
                User.parseRatingHistory(profile.getJSONArray("ratings_history"))
        );
    }

    public User(String userID, String username, double rating, int online, boolean privacy) {
        this.userID = userID;
        this.username = username;
        this.rating = rating;
        this.online = online;
        this.privacy = privacy;
        this.bestRating = -1;
        this.friendsCount = -1;
        this.gamesPlayed = -1;
        this.wins = -1;
        this.losses = -1;
        this.draws = -1;
        this.ratingsMap = new HashMap<>();
    }

    public User(String userID, String username, double rating, int online, boolean privacy, double bestRating, int friendsCnt, int games, int win, int lose, int draw, HashMap<String, Double> map) {
        this.userID = userID;
        this.username = username;
        this.rating = rating;
        this.online = online;
        this.privacy = privacy;
        this.bestRating = bestRating;
        this.friendsCount = friendsCnt;
        this.gamesPlayed = games;
        this.wins = win;
        this.losses = lose;
        this.draws = draw;
        this.ratingsMap = map;
    }

    public String getUserID() {
        return userID;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public double getRating() {
        return rating;
    }

    public void setRating(double rating) {
        this.rating = rating;
    }

    public void setPrivacy(boolean privacy) {
        this.privacy = privacy;
    }

    public int getOnline() {
        return online;
    }

    public boolean getPrivacy() {
        return privacy;
    }

    public double getBestRating() {
        return bestRating;
    }

    public int getFriendsCount() {
        return friendsCount;
    }

    public int getGamesPlayed() {
        return gamesPlayed;
    }

    public int getWins() {
        return wins;
    }

    public int getLosses() {
        return losses;
    }

    public int getDraws() {
        return draws;
    }

    public HashMap<String, Double> getEloRatingMap() {
        return ratingsMap;
    }

//implementation of Parcelable

    protected User(Parcel in) {
        userID = in.readString();
        username = in.readString();
        rating = in.readDouble();
        online = in.readInt();
        int p = in.readInt();
        privacy = p == 1;
        bestRating = in.readDouble();
        friendsCount = in.readInt();
        gamesPlayed = in.readInt();
        wins = in.readInt();
        losses = in.readInt();
        draws = in.readInt();
        int size = in.readInt();
        ratingsMap = new HashMap<>(size);
        for (int i = 0; i < size; i++) {
            String key = in.readString();
            Double value = in.readDouble();
            ratingsMap.put(key, value);
        }
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(userID);
        dest.writeString(username);
        dest.writeDouble(rating);
        dest.writeInt(online);
        int p;
        if (privacy) {
            p = 1;
        } else {
            p = 0;
        }
        dest.writeInt(p);
        dest.writeDouble(bestRating);
        dest.writeInt(friendsCount);
        dest.writeInt(gamesPlayed);
        dest.writeInt(wins);
        dest.writeInt(losses);
        dest.writeInt(draws);
        dest.writeInt(ratingsMap.size());
        for (Map.Entry<String, Double> entry : ratingsMap.entrySet()) {
            dest.writeString(entry.getKey());
            dest.writeDouble(entry.getValue());
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<User> CREATOR = new Creator<User>() {
        @Override
        public User createFromParcel(Parcel in) {
            return new User(in);
        }

        @Override
        public User[] newArray(int size) {
            return new User[size];
        }
    };
}
