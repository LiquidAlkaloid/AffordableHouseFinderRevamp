package com.example.affordablehousefinderrevamp;

public class ChatItem {
    private String senderName;
    private String lastMessage;
    private String timestamp;
    private int unreadCount; // 0 if no unread messages or if read
    private boolean isRead; // True if the last message is read (shows checkmark)
    private int profileImageResId; // Placeholder for local drawable resource ID

    // Constructor
    public ChatItem(String senderName, String lastMessage, String timestamp, int unreadCount, boolean isRead, int profileImageResId) {
        this.senderName = senderName;
        this.lastMessage = lastMessage;
        this.timestamp = timestamp;
        this.unreadCount = unreadCount;
        this.isRead = isRead;
        this.profileImageResId = profileImageResId; // e.g., R.drawable.profile_placeholder
    }

    // Getters
    public String getSenderName() {
        return senderName;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public int getUnreadCount() {
        return unreadCount;
    }

    public boolean isRead() {
        return isRead;
    }

    public int getProfileImageResId() {
        return profileImageResId;
    }

    // Optional Setters if needed
    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    public void setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public void setUnreadCount(int unreadCount) {
        this.unreadCount = unreadCount;
    }

    public void setRead(boolean read) {
        isRead = read;
    }

    public void setProfileImageResId(int profileImageResId) {
        this.profileImageResId = profileImageResId;
    }
}
