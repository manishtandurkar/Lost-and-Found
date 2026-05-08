package com.example.lostandfound.models;

public class NotificationItem {
    private String title;
    private String body;
    private String itemId;
    private long timestamp;

    public NotificationItem() {}

    public NotificationItem(String title, String body, String itemId, long timestamp) {
        this.title = title;
        this.body = body;
        this.itemId = itemId;
        this.timestamp = timestamp;
    }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }

    public String getItemId() { return itemId; }
    public void setItemId(String itemId) { this.itemId = itemId; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}
