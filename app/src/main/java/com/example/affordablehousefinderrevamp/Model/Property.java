package com.example.affordablehousefinderrevamp.Model;

import com.google.firebase.firestore.ServerTimestamp; // For @ServerTimestamp if you want Firestore to manage it
import java.util.Date; // Required for @ServerTimestamp
import java.util.List;
import java.util.Map;

public class Property {

    // Constants for property status
    public static final String STATUS_AVAILABLE = "Available";
    public static final String STATUS_RESERVED = "Reserved";
    public static final String STATUS_TAKEN = "Taken";

    private String name; // Used as title
    private String location;
    private String price;
    private String description;
    private String propertyType;
    private String numBedrooms; // Kept as String to match EditText inputs and existing usage
    private String numBathrooms; // Kept as String
    private String area;
    private List<String> imageUrls; // List of image URI strings
    private String sellerId;
    private String sellerName;
    private String sellerContact;
    private String propertyId; // Firestore document ID
    private String status;
    private Map<String, Boolean> amenities;
    private @ServerTimestamp Date timestamp; // Firestore server-side timestamp. Getter returns Date.
    private String videoUrl;

    // Required default constructor for Firebase/Firestore
    public Property() {
        this.status = STATUS_AVAILABLE; // Default status
        // Timestamp will be set by Firestore or in the setters/constructors
    }

    // Constructor used in UploadActivity
    public Property(String name, String location, String price, String description,
                    List<String> imageUrls, String sellerId, String propertyType,
                    String numBedrooms, String numBathrooms, String area, String status) {
        this.name = name;
        this.location = location;
        this.price = price;
        this.description = description;
        this.imageUrls = imageUrls;
        this.sellerId = sellerId;
        this.propertyType = propertyType;
        this.numBedrooms = numBedrooms;
        this.numBathrooms = numBathrooms;
        this.area = area;
        this.status = (status != null && !status.isEmpty()) ? status : STATUS_AVAILABLE;
        // this.timestamp = new Date(); // Or rely on @ServerTimestamp
    }


    // Getters and Setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getPrice() { return price; }
    public void setPrice(String price) { this.price = price; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getPropertyType() { return propertyType; }
    public void setPropertyType(String propertyType) { this.propertyType = propertyType; }

    public String getNumBedrooms() { return numBedrooms; }
    public void setNumBedrooms(String numBedrooms) { this.numBedrooms = numBedrooms; }

    public String getNumBathrooms() { return numBathrooms; }
    public void setNumBathrooms(String numBathrooms) { this.numBathrooms = numBathrooms; }

    public String getArea() { return area; }
    public void setArea(String area) { this.area = area; }

    public List<String> getImageUrls() { return imageUrls; }
    public void setImageUrls(List<String> imageUrls) { this.imageUrls = imageUrls; }

    // Convenience getter for a single image URL (e.g., the first one for display)
    public String getPrimaryImageUrl() {
        if (imageUrls != null && !imageUrls.isEmpty()) {
            return imageUrls.get(0);
        }
        return null; // Or a placeholder URL string
    }

    public String getSellerId() { return sellerId; }
    public void setSellerId(String sellerId) { this.sellerId = sellerId; }

    public String getSellerName() { return sellerName; }
    public void setSellerName(String sellerName) { this.sellerName = sellerName; }

    public String getSellerContact() { return sellerContact; }
    public void setSellerContact(String sellerContact) { this.sellerContact = sellerContact; }

    public String getPropertyId() { return propertyId; }
    public void setPropertyId(String propertyId) { this.propertyId = propertyId; }

    public String getStatus() {
        if (status == null || status.isEmpty()) {
            return STATUS_AVAILABLE; // Default to available if status is not set
        }
        return status;
    }
    public void setStatus(String status) { this.status = status; }

    public Map<String, Boolean> getAmenities() { return amenities; }
    public void setAmenities(Map<String, Boolean> amenities) { this.amenities = amenities; }

    public Date getTimestamp() { return timestamp; } // Getter for @ServerTimestamp
    public void setTimestamp(Date timestamp) { this.timestamp = timestamp; } // Setter for @ServerTimestamp

    public String getVideoUrl() { return videoUrl; }
    public void setVideoUrl(String videoUrl) { this.videoUrl = videoUrl; }

    public String getImageUrl() {
        return null;
    }
}
