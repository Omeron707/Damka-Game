package com.example.damka;

import org.json.JSONException;
import org.json.JSONObject;

public class Notification {
    private final int id;
    private final int type;
    private final String sourceID;
    private final String time;
    private final String content;

    public static Notification parseNotification(JSONObject message) throws JSONException {
        return new Notification(
                message.getInt("notification_id"),
                message.getInt("type"),
                message.getString("source"),
                message.getString("time"),
                message.getString("content")
        );
    }

    Notification(int id, int type, String source, String time, String content) {
        this.id = id;
        this.type = type;
        this.sourceID = source;
        this.time = time;
        this.content = content;
    }

    public int getId() {
        return id;
    }

    public int getType() {
        return type;
    }

    public String getSourceID() {
        return sourceID;
    }

    public String getTime() {
        return time;
    }

    public String getContent() {
        return content;
    }
}
