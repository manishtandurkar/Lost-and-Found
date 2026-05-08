package com.example.lostandfound.utils;

public class Constants {
    public static final String PREF_NAME = "LostAndFoundSession";
    public static final String PREF_USER_ID = "userId";
    public static final String PREF_USER_NAME = "userName";
    public static final String PREF_USER_EMAIL = "userEmail";
    public static final String PREF_USER_PHOTO = "userPhoto";
    public static final String PREF_IS_LOGGED_IN = "isLoggedIn";

    public static final String DB_USERS = "users";
    public static final String DB_LOST_ITEMS = "lost_items";
    public static final String DB_FOUND_ITEMS = "found_items";
    public static final String DB_CHATS = "chats";

    public static final String STATUS_ACTIVE = "active";
    public static final String STATUS_RESOLVED = "resolved";

    public static final String TYPE_LOST = "lost";
    public static final String TYPE_FOUND = "found";

    public static final String CONTACT_CHAT = "In-App Chat";
    public static final String CONTACT_PHONE = "Phone Number";

    public static final String[] CATEGORIES = {
            "Electronics", "Accessories", "Books", "ID Card", "Clothing", "Bag", "Other"
    };

    public static final String COLLEGE_DOMAIN = "college.edu";

    public static final String EXTRA_ITEM_ID = "item_id";
    public static final String EXTRA_ITEM_TYPE = "item_type";
    public static final String EXTRA_CHAT_ID = "chat_id";
    public static final String EXTRA_OTHER_USER_ID = "other_user_id";
    public static final String EXTRA_FILTER = "filter";

    public static final String FILTER_ALL = "All";
    public static final String FILTER_LOST = "Lost";
    public static final String FILTER_FOUND = "Found";
    public static final String FILTER_MY_POSTS = "My Posts";

    public static final String CHANNEL_ID = "lost_found_channel";
    public static final String CHANNEL_NAME = "Lost & Found Notifications";

    public static final int RC_SIGN_IN = 100;
    public static final int RC_LOCATION_PERMISSION = 101;
    public static final int RC_CAMERA_PERMISSION = 102;
    public static final int RC_STORAGE_PERMISSION = 103;
    public static final int RC_IMAGE_PICK = 200;
    public static final int RC_IMAGE_CAPTURE = 201;
    public static final int RC_PLACE_PICKER = 202;
}
