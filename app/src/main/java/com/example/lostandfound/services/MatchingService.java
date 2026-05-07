package com.example.lostandfound.services;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

import com.example.lostandfound.database.AppDatabase;
import com.example.lostandfound.database.ItemDao;
import com.example.lostandfound.database.ItemEntity;
import com.example.lostandfound.models.Notification;
import com.example.lostandfound.utils.Constants;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Background service that runs the matching algorithm when a new item is posted.
 * Compares the new item's category against existing items of the opposite type.
 * If a match is found, writes a notification to Firebase for the matching item's owner.
 */
public class MatchingService extends Service {

    private static final String TAG = "MatchingService";

    private ExecutorService executor;
    private ItemDao itemDao;

    @Override
    public void onCreate() {
        super.onCreate();
        executor = Executors.newSingleThreadExecutor();
        itemDao = AppDatabase.getInstance(this).itemDao();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            stopSelf(startId);
            return START_NOT_STICKY;
        }

        String newItemId = intent.getStringExtra(Constants.KEY_NEW_ITEM_ID);
        String newItemType = intent.getStringExtra(Constants.KEY_NEW_ITEM_TYPE);
        String newItemCategory = intent.getStringExtra(Constants.KEY_NEW_ITEM_CATEGORY);
        String newItemTitle = intent.getStringExtra(Constants.KEY_NEW_ITEM_TITLE);
        String newItemPostedBy = intent.getStringExtra(Constants.KEY_NEW_ITEM_POSTED_BY);

        if (newItemId == null || newItemType == null || newItemCategory == null) {
            stopSelf(startId);
            return START_NOT_STICKY;
        }

        runMatching(newItemId, newItemType, newItemCategory, newItemTitle,
                newItemPostedBy, startId);

        return START_NOT_STICKY;
    }

    private void runMatching(String newItemId, String newItemType, String newItemCategory,
                              String newItemTitle, String newItemPostedBy, int startId) {

        // Determine the opposite type to search
        String oppositeType = Constants.ITEM_TYPE_FOUND.equals(newItemType)
                ? Constants.ITEM_TYPE_LOST : Constants.ITEM_TYPE_FOUND;

        String oppositeNode = Constants.ITEM_TYPE_LOST.equals(oppositeType)
                ? Constants.NODE_LOST_ITEMS : Constants.NODE_FOUND_ITEMS;

        // Query Firebase for items of opposite type in same category
        FirebaseDatabase.getInstance()
                .getReference(oppositeNode)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        for (DataSnapshot child : snapshot.getChildren()) {
                            String category = child.child("category").getValue(String.class);
                            String status = child.child("status").getValue(String.class);
                            String postedBy = child.child("postedBy").getValue(String.class);

                            // Skip resolved items or items by same user
                            if (Constants.STATUS_RESOLVED.equals(status)) continue;
                            if (newItemPostedBy != null && newItemPostedBy.equals(postedBy)) continue;

                            // Category match
                            if (newItemCategory.equalsIgnoreCase(category)) {
                                String matchedItemId = child.getKey();
                                String matchedItemTitle = child.child("title").getValue(String.class);

                                Log.d(TAG, "Match found: " + matchedItemId
                                        + " for new item: " + newItemId);

                                // Notify the owner of the matched item
                                if (postedBy != null) {
                                    sendMatchNotification(postedBy, newItemId, newItemTitle,
                                            matchedItemId, matchedItemTitle, newItemType);
                                }
                            }
                        }
                        stopSelf(startId);
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        Log.e(TAG, "Matching query cancelled", error.toException());
                        stopSelf(startId);
                    }
                });
    }

    /**
     * Write a notification record to Firebase for the matched item's owner.
     */
    private void sendMatchNotification(String targetUserId, String newItemId,
                                        String newItemTitle, String matchedItemId,
                                        String matchedItemTitle, String newItemType) {
        String typeLabel = Constants.ITEM_TYPE_FOUND.equals(newItemType) ? "found" : "lost";
        String notifTitle = "Potential Match Found!";
        String notifBody = "A " + typeLabel + " item \"" + (newItemTitle != null ? newItemTitle : "item")
                + "\" may match your post \"" + (matchedItemTitle != null ? matchedItemTitle : "your item") + "\"";

        Notification notification = new Notification(
                Constants.FCM_TYPE_MATCH, notifTitle, notifBody, newItemId);

        DatabaseReference notifRef = FirebaseDatabase.getInstance()
                .getReference(Constants.NODE_NOTIFICATIONS)
                .child(targetUserId)
                .push();

        notifRef.setValue(notification)
                .addOnSuccessListener(unused ->
                        Log.d(TAG, "Match notification sent to user: " + targetUserId))
                .addOnFailureListener(e ->
                        Log.e(TAG, "Failed to send notification", e));
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (executor != null) {
            executor.shutdown();
        }
    }
}
