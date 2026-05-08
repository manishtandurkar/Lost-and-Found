package com.example.lostandfound.models;

public class User {
    private String userId;
    private String name;
    private String email;
    private String profilePhotoUrl;
    private String college;
    private String fcmToken;

    public User() {}

    public User(String userId, String name, String email, String profilePhotoUrl,
                String college, String fcmToken) {
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.profilePhotoUrl = profilePhotoUrl;
        this.college = college;
        this.fcmToken = fcmToken;
    }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getProfilePhotoUrl() { return profilePhotoUrl; }
    public void setProfilePhotoUrl(String profilePhotoUrl) { this.profilePhotoUrl = profilePhotoUrl; }

    public String getCollege() { return college; }
    public void setCollege(String college) { this.college = college; }

    public String getFcmToken() { return fcmToken; }
    public void setFcmToken(String fcmToken) { this.fcmToken = fcmToken; }
}
