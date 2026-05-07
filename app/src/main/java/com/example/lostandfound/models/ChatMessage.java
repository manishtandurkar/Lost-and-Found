package com.example.lostandfound.models;

import com.google.firebase.database.Exclude;
import com.google.firebase.database.IgnoreExtraProperties;

@IgnoreExtraProperties
public class ChatMessage {

    private String messageId;
    private String senderId;
    private String senderName;
    private String text;
    private long timestamp;

    // Default constructor required by Firebase
    public ChatMessage() {}

    public ChatMessage(String senderId, String senderName, String text) {
        this.senderId = senderId;
        this.senderName = senderName;
        this.text = text;
        this.timestamp = System.currentTimeMillis();
    }

    @Exclude
    public String getMessageId() { return messageId; }

    @Exclude
    public void setMessageId(String messageId) { this.messageId = messageId; }

    public String getSenderId() { return senderId; }
    public void setSenderId(String senderId) { this.senderId = senderId; }

    public String getSenderName() { return senderName; }
    public void setSenderName(String senderName) { this.senderName = senderName; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}
