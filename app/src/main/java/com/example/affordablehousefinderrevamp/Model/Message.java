package com.example.affordablehousefinderrevamp.Model;

import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;

public class Message {
    private String messageId;
    private String senderId;
    private String receiverId; // ID of the recipient
    private String text;
    @ServerTimestamp
    private Date timestamp;
    private boolean isRead; // Optional: for read receipts

    // Required empty public constructor for Firestore
    public Message() {}

    public Message(String senderId, String receiverId, String text) {
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.text = text;
        // timestamp is set by @ServerTimestamp
        this.isRead = false; // Default to unread
    }

    // Getters
    public String getMessageId() {
        return messageId;
    }

    public String getSenderId() {
        return senderId;
    }

    public String getReceiverId() {
        return receiverId;
    }

    public String getText() {
        return text;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public boolean isRead() {
        return isRead;
    }

    // Setters
    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public void setReceiverId(String receiverId) {
        this.receiverId = receiverId;
    }

    public void setText(String text) {
        this.text = text;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public void setRead(boolean read) {
        isRead = read;
    }
}
