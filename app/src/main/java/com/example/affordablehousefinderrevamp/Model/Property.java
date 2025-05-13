package com.example.affordablehousefinderrevamp.Model;

import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date; // Import Date
import java.util.List;

public class Property {
    @DocumentId // Firestore will automatically populate this with the document ID
    private String propertyId;

    private String title;
    private String location;
    private String price;
    private String description;
    private String imageUrl; // Main image URL (first from the list)
    private List<String> imageUrls; // List of all image URI strings
    private String sellerId;
    private String propertyType;
    private int bedrooms;
    private int bathrooms;
    private String area;

    @ServerTimestamp // Firestore will automatically populate this on the server
    private Date timestamp;

    private boolean isBookmarked; // UI state, not typically stored directly in the main property document for all users

    // No-argument constructor required for Firestore
    public Property() {}

    // Constructor without propertyId and timestamp as they can be auto-generated or set separately
    public Property(String title, String location, String price, String description,
                    String imageUrl, List<String> imageUrls, String sellerId, String propertyType,
                    int bedrooms, int bathrooms, String area) {
        this.title = title;
        this.location = location;
        this.price = price;
        this.description = description;
        this.imageUrl = imageUrl;
        this.imageUrls = imageUrls;
        this.sellerId = sellerId;
        this.propertyType = propertyType;
        this.bedrooms = bedrooms;
        this.bathrooms = bathrooms;
        this.area = area;
        // Timestamp is set by @ServerTimestamp
    }


    // Getters
    public String getPropertyId() { return propertyId; }
    public String getTitle() { return title; }
    public String getLocation() { return location; }
    public String getPrice() { return price; }
    public String getDescription() { return description; }
    public String getImageUrl() { return imageUrl; }
    public List<String> getImageUrls() { return imageUrls; }
    public String getSellerId() { return sellerId; }
    public String getPropertyType() { return propertyType; }
    public int getBedrooms() { return bedrooms; }
    public int getBathrooms() { return bathrooms; }
    public String getArea() { return area; }
    public Date getTimestamp() { return timestamp; } // Return Date

    @Exclude // Exclude from Firestore serialization if it's purely a UI concern
    public boolean isBookmarked() { return isBookmarked; }

    // Setters
    public void setPropertyId(String propertyId) { this.propertyId = propertyId; }
    public void setTitle(String title) { this.title = title; }
    public void setLocation(String location) { this.location = location; }
    public void setPrice(String price) { this.price = price; }
    public void setDescription(String description) { this.description = description; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public void setImageUrls(List<String> imageUrls) { this.imageUrls = imageUrls; }
    public void setSellerId(String sellerId) { this.sellerId = sellerId; }
    public void setPropertyType(String propertyType) { this.propertyType = propertyType; }
    public void setBedrooms(int bedrooms) { this.bedrooms = bedrooms; }
    public void setBathrooms(int bathrooms) { this.bathrooms = bathrooms; }
    public void setArea(String area) { this.area = area; }
    public void setTimestamp(Date timestamp) { this.timestamp = timestamp; } // Accept Date
    public void setBookmarked(boolean bookmarked) { isBookmarked = bookmarked; }
}
