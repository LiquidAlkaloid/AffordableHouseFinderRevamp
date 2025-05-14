package com.example.affordablehousefinderrevamp.Model;

import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;
import java.util.List;
import java.util.ArrayList; // Added for initializing imageUrls

public class Property {
    @DocumentId
    private String propertyId;

    private String title;
    private String location;
    private String price;
    private String description;
    private String imageUrl; // Can be the primary image URL or the first from imageUrls
    private List<String> imageUrls; // List of image URLs (e.g., direct HTTPS links or file:// URIs for local cache)
    private String sellerId;
    private String propertyType;
    private int bedrooms;
    private int bathrooms;
    private String area;
    private String status;

    @ServerTimestamp
    private Date timestamp; // Handles creation/update time by Firestore

    @Exclude
    private boolean isBookmarked;

    public Property() {
        // Firestore requires a no-argument constructor
        this.status = "Available"; // Default status
        this.imageUrls = new ArrayList<>(); // Initialize the list
    }

    // Constructor for creating a new property
    public Property(String title, String location, String price, String description,
                    String imageUrl, List<String> imageUrls, String sellerId, String propertyType,
                    int bedrooms, int bathrooms, String area, String status) {
        this.title = title;
        this.location = location;
        this.price = price;
        this.description = description;
        this.imageUrl = imageUrl; // This could be the first image from imageUrls or a separate primary image
        this.imageUrls = (imageUrls != null) ? imageUrls : new ArrayList<>(); // Ensure list is initialized
        this.sellerId = sellerId;
        this.propertyType = propertyType;
        this.bedrooms = bedrooms;
        this.bathrooms = bathrooms;
        this.area = area;
        this.status = (status != null && !status.isEmpty()) ? status : "Available";
        // Timestamp is set by @ServerTimestamp
    }


    // Getters
    public String getPropertyId() { return propertyId; }
    public String getTitle() { return title; }
    public String getLocation() { return location; }
    public String getPrice() { return price; }
    public String getDescription() { return description; }
    public String getImageUrl() { return imageUrl; } // Main/cover image
    public List<String> getImageUrls() { return imageUrls; } // Gallery images
    public String getSellerId() { return sellerId; }
    public String getPropertyType() { return propertyType; }
    public int getBedrooms() { return bedrooms; }
    public int getBathrooms() { return bathrooms; }
    public String getArea() { return area; }
    public String getStatus() { return status; }
    public Date getTimestamp() { return timestamp; }

    @Exclude
    public boolean isBookmarked() { return isBookmarked; }

    // Setters
    public void setPropertyId(String propertyId) { this.propertyId = propertyId; }
    public void setTitle(String title) { this.title = title; }
    public void setLocation(String location) { this.location = location; }
    public void setPrice(String price) { this.price = price; }
    public void setDescription(String description) { this.description = description; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public void setImageUrls(List<String> imageUrls) { this.imageUrls = (imageUrls != null) ? imageUrls : new ArrayList<>(); }
    public void setSellerId(String sellerId) { this.sellerId = sellerId; }
    public void setPropertyType(String propertyType) { this.propertyType = propertyType; }
    public void setBedrooms(int bedrooms) { this.bedrooms = bedrooms; }
    public void setBathrooms(int bathrooms) { this.bathrooms = bathrooms; }
    public void setArea(String area) { this.area = area; }
    public void setStatus(String status) { this.status = status; }
    public void setTimestamp(Date timestamp) { this.timestamp = timestamp; } // Allow setting for specific cases like preserving original on edit
    @Exclude
    public void setBookmarked(boolean bookmarked) { isBookmarked = bookmarked; }
}
