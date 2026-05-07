package com.example.lostandfound.utils;

public final class Constants {

    private Constants() {}

    // Firebase database node names
    public static final String NODE_USERS = "users";
    public static final String NODE_LOST_ITEMS = "lost_items";
    public static final String NODE_FOUND_ITEMS = "found_items";
    public static final String NODE_CHATS = "chats";
    public static final String NODE_MESSAGES = "messages";
    public static final String NODE_NOTIFICATIONS = "notifications";
    public static final String NODE_PARTICIPANTS = "participants";

    // Firebase storage paths
    public static final String STORAGE_ITEM_PHOTOS = "item_photos/";

    // Firebase user fields
    public static final String FIELD_FCM_TOKEN = "fcmToken";
    public static final String FIELD_STATUS = "status";

    // Intent extra keys
    public static final String EXTRA_ITEM_ID = "extra_item_id";
    public static final String EXTRA_ITEM_TYPE = "extra_item_type";
    public static final String EXTRA_CHAT_ID = "extra_chat_id";
    public static final String EXTRA_OTHER_USER_ID = "extra_other_user_id";
    public static final String EXTRA_OTHER_USER_NAME = "extra_other_user_name";

    // College domain restriction
    public static final String COLLEGE_EMAIL_DOMAIN = "@stu.college.edu";

    // Item types
    public static final String ITEM_TYPE_LOST = "lost";
    public static final String ITEM_TYPE_FOUND = "found";

    // Item status
    public static final String STATUS_ACTIVE = "active";
    public static final String STATUS_RESOLVED = "resolved";

    // Contact preferences
    public static final String CONTACT_IN_APP_CHAT = "In-App Chat";
    public static final String CONTACT_PHONE = "Phone Number";

    // Item categories
    public static final String[] CATEGORIES = {
        "Electronics", "Accessories", "Books", "ID Card", "Clothing", "Bag", "Other"
    };

    // Notification channels
    public static final String NOTIFICATION_CHANNEL_ID = "lost_found_channel";
    public static final String NOTIFICATION_CHANNEL_NAME = "Lost & Found Alerts";

    // FCM notification types
    public static final String FCM_TYPE_MATCH = "match";
    public static final String FCM_TYPE_CLAIM = "claim";
    public static final String FCM_TYPE_BROADCAST = "broadcast";

    // Matching service intent keys
    public static final String KEY_NEW_ITEM_ID = "new_item_id";
    public static final String KEY_NEW_ITEM_TYPE = "new_item_type";
    public static final String KEY_NEW_ITEM_CATEGORY = "new_item_category";
    public static final String KEY_NEW_ITEM_TITLE = "new_item_title";
    public static final String KEY_NEW_ITEM_POSTED_BY = "new_item_posted_by";
}
