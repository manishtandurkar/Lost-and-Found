package com.example.lostandfound.database;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.annotation.NonNull;

@Entity(tableName = "items_cache")
public class ItemEntity {

    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "id")
    public String id;

    @ColumnInfo(name = "type")
    public String type;

    @ColumnInfo(name = "title")
    public String title;

    @ColumnInfo(name = "category")
    public String category;

    @ColumnInfo(name = "description")
    public String description;

    @ColumnInfo(name = "location_name")
    public String locationName;

    @ColumnInfo(name = "latitude")
    public double latitude;

    @ColumnInfo(name = "longitude")
    public double longitude;

    @ColumnInfo(name = "photo_url")
    public String photoUrl;

    @ColumnInfo(name = "posted_by")
    public String postedBy;

    @ColumnInfo(name = "contact_preference")
    public String contactPreference;

    @ColumnInfo(name = "status")
    public String status;

    @ColumnInfo(name = "timestamp")
    public long timestamp;

    @ColumnInfo(name = "last_synced_at")
    public long lastSyncedAt;

    public ItemEntity() {}

    public ItemEntity(@NonNull String id, String type, String title, String category,
                      String description, String locationName, double latitude, double longitude,
                      String photoUrl, String postedBy, String contactPreference,
                      String status, long timestamp) {
        this.id = id;
        this.type = type;
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
        this.lastSyncedAt = System.currentTimeMillis();
    }
}
