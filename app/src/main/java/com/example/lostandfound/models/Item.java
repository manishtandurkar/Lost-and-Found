package com.example.lostandfound.models;

import com.google.firebase.database.Exclude;
import com.google.firebase.database.IgnoreExtraProperties;

@IgnoreExtraProperties
public class Item {

    // Item types
    public static final String TYPE_LOST = "lost";
    public static final String TYPE_FOUND = "found";

    // Status values
    public static final String STATUS_ACTIVE = "active";
    public static final String STATUS_RESOLVED = "resolved";

    private String id;
    private String type; // "lost" or "found"
    private String title;
    private String category;
    private String description;
    private String locationName;
    private double latitude;
    private double longitude;
    private String photoUrl;
    private String postedBy; // userId
    private String contactPreference; // "In-App Chat" or "Phone Number"
    private String status; // "active" or "resolved"
    private long timestamp;

    // Default constructor required by Firebase
    public Item() {}

    public Item(String title, String category, String description, String locationName,
                double latitude, double longitude, String photoUrl, String postedBy,
                String contactPreference, String type) {
        this.title = title;
        this.category = category;
        this.description = description;
        this.locationName = locationName;
        this.latitude = latitude;
        this.longitude = longitude;
        this.photoUrl = photoUrl;
        this.postedBy = postedBy;
        this.contactPreference = contactPreference;
        this.type = type;
        this.status = STATUS_ACTIVE;
        this.timestamp = System.currentTimeMillis();
    }

    @Exclude
    public String getId() { return id; }

    @Exclude
    public void setId(String id) { this.id = id; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getLocationName() { return locationName; }
    public void setLocationName(String locationName) { this.locationName = locationName; }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }

    public String getPhotoUrl() { return photoUrl; }
    public void setPhotoUrl(String photoUrl) { this.photoUrl = photoUrl; }

    public String getPostedBy() { return postedBy; }
    public void setPostedBy(String postedBy) { this.postedBy = postedBy; }

    public String getContactPreference() { return contactPreference; }
    public void setContactPreference(String contactPreference) {
        this.contactPreference = contactPreference;
    }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    @Exclude
    public boolean isLost() {
        return TYPE_LOST.equals(type);
    }

    @Exclude
    public boolean isResolved() {
        return STATUS_RESOLVED.equals(status);
    }
}
