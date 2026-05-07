package com.example.lostandfound.database;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.annotation.NonNull;

@Entity(tableName = "user_session")
public class UserSessionEntity {

    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "user_id")
    public String userId;

    @ColumnInfo(name = "name")
    public String name;

    @ColumnInfo(name = "email")
    public String email;

    @ColumnInfo(name = "profile_photo_url")
    public String profilePhotoUrl;

    @ColumnInfo(name = "fcm_token")
    public String fcmToken;

    public UserSessionEntity() {}

    public UserSessionEntity(@NonNull String userId, String name, String email,
                              String profilePhotoUrl) {
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.profilePhotoUrl = profilePhotoUrl;
    }
}
