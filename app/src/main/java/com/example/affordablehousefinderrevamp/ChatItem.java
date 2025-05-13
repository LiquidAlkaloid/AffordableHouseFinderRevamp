package com.example.affordablehousefinderrevamp; // Or your designated model package e.g., com.example.affordablehousefinderrevamp.Model

import com.example.affordablehousefinderrevamp.R; // Required if you use R.drawable constants as defaults

public class ChatItem {
    private String chatId; // Unique ID for the chat conversation/session
    private String otherUserId; // UID of the other user in this chat session
    private String senderName;  // Display name of the other user (the "sender" from the perspective of the chat list item)
    private String lastMessage;
    private String timestamp;   // Display timestamp (e.g., "10:30 AM", "Yesterday")
    private int unreadCount;    // Number of unread messages for the current user in this chat
    private boolean isRead;     // For the read tick: typically, if the current user sent the last message, this indicates if the other user has read it.
    private String profileImageUrl; // URL for the other user's profile image (for Glide/Picasso)
    private int profileImageResource; // Placeholder drawable resource if URL is not available or fails to load

    // Default constructor is required for Firebase Realtime Database object mapping
    public ChatItem() {}

    /**
     * Constructor based on the sample data instantiation in Chat_Buyer.java.
     * ChatItem("chat1_placeholder", "seller123", "John Seller (Sample)", "Okay, see you then!", "10:30 AM", 0, true, "user_buyer_id_sample", R.drawable.ic_person_placeholder)
     * The 8th argument (String currentUserId_unused) from the sample is ignored here as it's not a direct property of the ChatItem itself,
     * but rather contextual information for the list.
     *
     * @param chatId                Unique ID for this chat session.
     * @param otherUserId           User ID of the other participant in the chat.
     * @param senderName            Display name of the other participant.
     * @param lastMessage           The last message exchanged.
     * @param timestamp             Timestamp of the last message (display string).
     * @param unreadCount           Number of unread messages for the current user.
     * @param isRead                Read status of the last message (context-dependent meaning).
     * @param profileImageResource  Placeholder drawable for the profile image.
     */
    public ChatItem(String chatId, String otherUserId, String senderName, String lastMessage, String timestamp,
                    int unreadCount, boolean isRead, String ignoredCurrentUserField, int profileImageResource) {
        this.chatId = chatId;
        this.otherUserId = otherUserId;
        this.senderName = senderName;
        this.lastMessage = lastMessage;
        this.timestamp = timestamp;
        this.unreadCount = unreadCount;
        this.isRead = isRead;
        this.profileImageResource = profileImageResource;
        this.profileImageUrl = null; // Initialize image URL as null, can be set later
    }

    // Getters
    public String getChatId() { return chatId; }
    public String getOtherUserId() { return otherUserId; }
    public String getSenderName() { return senderName; }
    public String getLastMessage() { return lastMessage; }
    public String getTimestamp() { return timestamp; }
    public int getUnreadCount() { return unreadCount; }
    public boolean isRead() { return isRead; }
    public String getProfileImageUrl() { return profileImageUrl; }
    public int getProfileImageResource() { return profileImageResource; }

    // Setters
    public void setChatId(String chatId) { this.chatId = chatId; }
    public void setOtherUserId(String otherUserId) { this.otherUserId = otherUserId; }
    public void setSenderName(String senderName) { this.senderName = senderName; }
    public void setLastMessage(String lastMessage) { this.lastMessage = lastMessage; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
    public void setUnreadCount(int unreadCount) { this.unreadCount = unreadCount; }
    public void setRead(boolean read) { isRead = read; }
    public void setProfileImageUrl(String profileImageUrl) { this.profileImageUrl = profileImageUrl; }
    public void setProfileImageResource(int profileImageResource) { this.profileImageResource = profileImageResource; }

    /**
     * Alias for getOtherUserId() to match the usage in Chat_Buyer's item click listener
     * where 'receiverId' refers to the ID of the person the current user is chatting with.
     * @return The User ID of the other participant in the chat.
     */
    public String getReceiverId() {
        return this.otherUserId;
    }
}
