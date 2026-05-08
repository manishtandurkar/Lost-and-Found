package com.example.lostandfound.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {

    private final SharedPreferences prefs;
    private final SharedPreferences.Editor editor;

    public SessionManager(Context context) {
        prefs = context.getSharedPreferences(Constants.PREF_NAME, Context.MODE_PRIVATE);
        editor = prefs.edit();
    }

    public void saveSession(String userId, String name, String email, String photoUrl) {
        editor.putBoolean(Constants.PREF_IS_LOGGED_IN, true);
        editor.putString(Constants.PREF_USER_ID, userId);
        editor.putString(Constants.PREF_USER_NAME, name);
        editor.putString(Constants.PREF_USER_EMAIL, email);
        editor.putString(Constants.PREF_USER_PHOTO, photoUrl);
        editor.apply();
    }

    public void clearSession() {
        editor.clear();
        editor.apply();
    }

    public boolean isLoggedIn() {
        return prefs.getBoolean(Constants.PREF_IS_LOGGED_IN, false);
    }

    public String getUserId() {
        return prefs.getString(Constants.PREF_USER_ID, null);
    }

    public String getUserName() {
        return prefs.getString(Constants.PREF_USER_NAME, null);
    }

    public String getUserEmail() {
        return prefs.getString(Constants.PREF_USER_EMAIL, null);
    }

    public String getUserPhotoUrl() {
        return prefs.getString(Constants.PREF_USER_PHOTO, null);
    }
}
