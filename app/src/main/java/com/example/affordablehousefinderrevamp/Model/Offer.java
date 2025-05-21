package com.example.affordablehousefinderrevamp.Model;

import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;

public class Offer {
    private String offerId; // Document ID
    private String propertyId;
    private String propertyTitle; // For easier display
    private String propertyImageUrl; // For easier display
    private String buyerId;
    private String buyerName; // For easier display
    private String sellerId;
    private String offerAmount;
    private String status; // "pending", "accepted", "declined"
    @ServerTimestamp
    private Date timestamp;

    public Offer() {
        // Firestore requires a no-argument constructor
    }

    public Offer(String propertyId, String propertyTitle, String propertyImageUrl, String buyerId, String buyerName, String sellerId, String offerAmount) {
        this.propertyId = propertyId;
        this.propertyTitle = propertyTitle;
        this.propertyImageUrl = propertyImageUrl;
        this.buyerId = buyerId;
        this.buyerName = buyerName;
        this.sellerId = sellerId;
        this.offerAmount = offerAmount;
        this.status = "pending"; // Default status
    }

    // Getters
    public String getOfferId() { return offerId; }
    public String getPropertyId() { return propertyId; }
    public String getPropertyTitle() { return propertyTitle; }
    public String getPropertyImageUrl() { return propertyImageUrl; }
    public String getBuyerId() { return buyerId; }
    public String getBuyerName() { return buyerName; }
    public String getSellerId() { return sellerId; }
    public String getOfferAmount() { return offerAmount; }
    public String getStatus() { return status; }
    public Date getTimestamp() { return timestamp; }

    // Setters
    public void setOfferId(String offerId) { this.offerId = offerId; }
    public void setPropertyId(String propertyId) { this.propertyId = propertyId; }
    public void setPropertyTitle(String propertyTitle) { this.propertyTitle = propertyTitle; }
    public void setPropertyImageUrl(String propertyImageUrl) { this.propertyImageUrl = propertyImageUrl; }
    public void setBuyerId(String buyerId) { this.buyerId = buyerId; }
    public void setBuyerName(String buyerName) { this.buyerName = buyerName; }
    public void setSellerId(String sellerId) { this.sellerId = sellerId; }
    public void setOfferAmount(String offerAmount) { this.offerAmount = offerAmount; }
    public void setStatus(String status) { this.status = status; }
    public void setTimestamp(Date timestamp) { this.timestamp = timestamp; }
}