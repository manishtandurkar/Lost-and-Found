package com.example.lostandfound.models;

import com.google.firebase.database.Exclude;
import com.google.firebase.database.IgnoreExtraProperties;

@IgnoreExtraProperties
public class Notification {

    // Notification types
    public static final String TYPE_MATCH = "match";
    public static final String TYPE_CLAIM = "claim";
    public static final String TYPE_BROADCAST = "broadcast";

    private String notificationId;
    private String type;
    private String title;
    private String body;
    private String itemId;
    private String relatedUserId;
    private long timestamp;
    private boolean isRead;

    // Default constructor required by Firebase
    public Notification() {}

    public Notification(String type, String title, String body, String itemId) {
        this.type = type;
        this.title = title;
        this.body = body;
        this.itemId = itemId;
        this.timestamp = System.currentTimeMillis();
        this.isRead = false;
    }

    @Exclude
    public String getNotificationId() { return notificationId; }

    @Exclude
    public void setNotificationId(String notificationId) {
        this.notificationId = notificationId;
    }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }

    public String getItemId() { return itemId; }
    public void setItemId(String itemId) { this.itemId = itemId; }

    public String getRelatedUserId() { return relatedUserId; }
    public void setRelatedUserId(String relatedUserId) {
        this.relatedUserId = relatedUserId;
    }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public boolean isRead() { return isRead; }
    public void setRead(boolean read) { isRead = read; }
}
