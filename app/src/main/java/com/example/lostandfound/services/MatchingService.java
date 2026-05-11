package com.example.lostandfound.services;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import androidx.annotation.Nullable;

import com.example.lostandfound.models.Item;
import com.example.lostandfound.utils.Constants;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.concurrent.Executors;

public class MatchingService extends Service {

    private DatabaseReference dbRef;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            stopSelf();
            return START_NOT_STICKY;
        }

        String newItemId = intent.getStringExtra(Constants.EXTRA_ITEM_ID);
        String newItemType = intent.getStringExtra(Constants.EXTRA_ITEM_TYPE);

        if (newItemId == null || newItemType == null) {
            stopSelf();
            return START_NOT_STICKY;
        }

        dbRef = FirebaseDatabase.getInstance("https://lost-and-found-d65bc-default-rtdb.firebaseio.com").getReference();

        String sourceNode = newItemType.equals(Constants.TYPE_FOUND)
                ? Constants.DB_FOUND_ITEMS : Constants.DB_LOST_ITEMS;
        String oppositeNode = newItemType.equals(Constants.TYPE_FOUND)
                ? Constants.DB_LOST_ITEMS : Constants.DB_FOUND_ITEMS;

        dbRef.child(sourceNode).child(newItemId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                Item newItem = snapshot.getValue(Item.class);
                if (newItem == null) { stopSelf(); return; }
                newItem.setId(newItemId);
                searchForMatches(newItem, oppositeNode);
            }

            @Override
            public void onCancelled(DatabaseError error) { stopSelf(); }
        });

        return START_NOT_STICKY;
    }

    private void searchForMatches(Item newItem, String oppositeNode) {
        dbRef.child(oppositeNode).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                for (DataSnapshot child : snapshot.getChildren()) {
                    Item candidate = child.getValue(Item.class);
                    if (candidate == null) continue;
                    candidate.setId(child.getKey());

                    if (isMatch(newItem, candidate)) {
                        sendMatchNotification(candidate);
                    }
                }
                stopSelf();
            }

            @Override
            public void onCancelled(DatabaseError error) { stopSelf(); }
        });
    }

    private boolean isMatch(Item newItem, Item candidate) {
        if (!safeEquals(newItem.getCategory(), candidate.getCategory())) return false;

        String newWords = ((newItem.getTitle() != null ? newItem.getTitle() : "") + " "
                + (newItem.getDescription() != null ? newItem.getDescription() : "")).toLowerCase();
        String candWords = ((candidate.getTitle() != null ? candidate.getTitle() : "") + " "
                + (candidate.getDescription() != null ? candidate.getDescription() : "")).toLowerCase();

        String[] tokens = newWords.split("\\s+");
        for (String token : tokens) {
            if (token.length() > 3 && candWords.contains(token)) return true;
        }
        return false;
    }

    private boolean safeEquals(String a, String b) {
        if (a == null || b == null) return false;
        return a.equalsIgnoreCase(b);
    }

    private void sendMatchNotification(Item matchedItem) {
        if (matchedItem.getPostedBy() == null) return;
        dbRef.child(Constants.DB_USERS).child(matchedItem.getPostedBy())
                .child("fcmToken").addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        String token = snapshot.getValue(String.class);
                        if (token != null && !token.isEmpty()) {
                            // FCM server-to-device messaging would be done via your backend/Cloud Functions.
                            // For now we log the intent. In production, use Firebase Cloud Functions.
                            android.util.Log.d("MatchingService",
                                    "Match found for item: " + matchedItem.getId()
                                            + ", notify token: " + token);
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {}
                });
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) { return null; }
}
