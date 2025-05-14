package com.example.affordablehousefinderrevamp.Model;

import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;

public class Message {
    private String senderId;
    private String receiverId;
    private String text;
    private @ServerTimestamp Date timestamp; // Firestore will populate this if null, or use client's Date
    private boolean read; // To track if the message has been read by the receiver

    // Required empty public constructor for Firestore deserialization
    public Message() {
    }

    // Constructor used in Chat_Seller.java and Chat_Buyer.java
    public Message(String senderId, String receiverId, String text, Date timestamp) {
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.text = text;
        this.timestamp = timestamp; // Client provides the timestamp
        this.read = false; // Default to unread
    }

    // Getters
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
        return read;
    }

    // Setters (useful for Firestore and potentially client-side updates)
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
        this.read = read;
    }
}
