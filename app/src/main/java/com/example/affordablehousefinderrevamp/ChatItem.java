package com.example.affordablehousefinderrevamp;

import com.google.firebase.firestore.PropertyName;
import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;

public class ChatItem {
    private String chatId;
    private String otherUserId;
    private String senderName;
    private String lastMessage;
    private int unreadCount;
    private Date timestampDate;
    private String propertyId;
    private String propertyName;
    private String offerStatus;
    private Boolean conversationClosed;

    // Default constructor for Firestore
    public ChatItem() {
        this.unreadCount = 0;
        this.conversationClosed = false;
        this.offerStatus = "pending";
    }

    // Getters and setters
    public String getChatId() { return chatId; }
    public void setChatId(String chatId) { this.chatId = chatId; }

    public String getOtherUserId() { return otherUserId; }
    public void setOtherUserId(String otherUserId) { this.otherUserId = otherUserId; }

    public String getSenderName() { return senderName; }
    public void setSenderName(String senderName) { this.senderName = senderName; }

    public String getLastMessage() { return lastMessage; }
    public void setLastMessage(String lastMessage) { this.lastMessage = lastMessage; }

    public int getUnreadCount() { return unreadCount; }
    public void setUnreadCount(int unreadCount) { this.unreadCount = unreadCount; }

    public Date getTimestampDate() { return timestampDate; }
    public void setTimestampDate(Date timestampDate) { this.timestampDate = timestampDate; }

    public String getPropertyId() { return propertyId; }
    public void setPropertyId(String propertyId) { this.propertyId = propertyId; }

    public String getPropertyName() { return propertyName; }
    public void setPropertyName(String propertyName) { this.propertyName = propertyName; }

    public String getOfferStatus() { return offerStatus; }
    public void setOfferStatus(String offerStatus) { this.offerStatus = offerStatus; }

    public Boolean getConversationClosed() { return conversationClosed; }
    public void setConversationClosed(Boolean conversationClosed) { this.conversationClosed = conversationClosed; }
}

