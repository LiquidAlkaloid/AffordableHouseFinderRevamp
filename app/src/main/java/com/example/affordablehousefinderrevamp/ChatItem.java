package com.example.affordablehousefinderrevamp;

public class ChatItem {
    private String senderName;
    private String lastMessage;
    private String timestamp;
    private int unreadCount;       // 0 if no unread messages
    private boolean isRead;        // true if last message was read
    private String profileImageUrl; // URL (e.g., Firestore field) or local uri string

    // No-arg constructor required for Firestore deserialization
    public ChatItem() {}

    // Full constructor
    public ChatItem(String senderName,
                    String lastMessage,
                    String timestamp,
                    int unreadCount,
                    boolean isRead,
                    String profileImageUrl) {
        this.senderName      = senderName;
        this.lastMessage     = lastMessage;
        this.timestamp       = timestamp;
        this.unreadCount     = unreadCount;
        this.isRead          = isRead;
        this.profileImageUrl = profileImageUrl;
    }

    // Getters
    public String getSenderName()    { return senderName; }
    public String getLastMessage()   { return lastMessage; }
    public String getTimestamp()     { return timestamp; }
    public int    getUnreadCount()   { return unreadCount; }
    public boolean isRead()          { return isRead; }
    public String getProfileImageUrl() { return profileImageUrl; }

    // Setters (optional)
    public void setSenderName(String senderName)        { this.senderName = senderName; }
    public void setLastMessage(String lastMessage)      { this.lastMessage = lastMessage; }
    public void setTimestamp(String timestamp)          { this.timestamp = timestamp; }
    public void setUnreadCount(int unreadCount)         { this.unreadCount = unreadCount; }
    public void setRead(boolean read)                   { isRead = read; }
    public void setProfileImageUrl(String profileImageUrl) { this.profileImageUrl = profileImageUrl; }
}