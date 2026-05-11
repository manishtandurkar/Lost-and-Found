package com.example.lostandfound.repository;

import com.example.lostandfound.models.User;
import com.example.lostandfound.utils.Constants;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class UserRepository {

    private final DatabaseReference usersRef;

    public UserRepository() {
        usersRef = FirebaseDatabase.getInstance("https://lost-and-found-d65bc-default-rtdb.firebaseio.com").getReference(Constants.DB_USERS);
    }

    public void createOrUpdateUser(User user, Callback callback) {
        android.util.Log.d("UserRepository", "Writing user to DB: " + user.getUserId());
        usersRef.child(user.getUserId()).setValue(user)
                .addOnSuccessListener(unused -> {
                    android.util.Log.d("UserRepository", "DB write success");
                    if (callback != null) callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("UserRepository", "DB write failed: " + e.getMessage());
                    if (callback != null) callback.onError(e.getMessage());
                });
    }

    public void getUser(String userId, UserFetchCallback callback) {
        usersRef.child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                User user = snapshot.getValue(User.class);
                if (callback != null) callback.onFetched(user);
            }

            @Override
            public void onCancelled(DatabaseError error) {
                if (callback != null) callback.onError(error.getMessage());
            }
        });
    }

    public void updateFcmToken(String userId, String token) {
        usersRef.child(userId).child("fcmToken").setValue(token);
    }

    public interface Callback {
        void onSuccess();
        void onError(String message);
    }

    public interface UserFetchCallback {
        void onFetched(User user);
        void onError(String message);
    }
}
