package com.example.lostandfound.models;

public class Item {
    private String id;
    private String title;
    private String category;
    private String description;
    private String locationName;
    private double latitude;
    private double longitude;
    private String photoUrl;
    private String postedBy;
    private String contactPreference;
    private String status;
    private long timestamp;
    private String type; // "lost" or "found"

    public Item() {}

    public Item(String id, String title, String category, String description,
                String locationName, double latitude, double longitude,
                String photoUrl, String postedBy, String contactPreference,
                String status, long timestamp, String type) {
        this.id = id;
        this.title = title;
        this.category = category;
        this.description = description;
        this.locationName = locationName;
        this.latitude = latitude;
        this.longitude = longitude;
        this.photoUrl = photoUrl;
        this.postedBy = postedBy;
        this.contactPreference = contactPreference;
        this.status = status;
        this.timestamp = timestamp;
        this.type = type;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

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
    public void setContactPreference(String contactPreference) { this.contactPreference = contactPreference; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
}
