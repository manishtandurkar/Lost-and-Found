package com.example.lostandfound.notifications;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.example.lostandfound.R;
import com.example.lostandfound.activities.ItemDetailActivity;
import com.example.lostandfound.utils.Constants;
import com.example.lostandfound.utils.SessionManager;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;

/**
 * Firebase Cloud Messaging service.
 * Handles incoming FCM messages and token refresh.
 */
public class FCMService extends FirebaseMessagingService {

    private static final String TAG = "FCMService";
    private static final int NOTIFICATION_ID_BASE = 1000;

    /**
     * Called when a new FCM token is generated.
     * Updates the token in Firebase under users/{userId}/fcmToken.
     */
    @Override
    public void onNewToken(String token) {
        super.onNewToken(token);
        Log.d(TAG, "New FCM token: " + token);
        updateFcmTokenInFirebase(token);
    }

    private void updateFcmTokenInFirebase(String token) {
        SessionManager sessionManager = new SessionManager(this);
        String userId = sessionManager.getUserId();
        if (userId != null) {
            FirebaseDatabase.getInstance()
                    .getReference(Constants.NODE_USERS)
                    .child(userId)
                    .child(Constants.FIELD_FCM_TOKEN)
                    .setValue(token)
                    .addOnSuccessListener(unused ->
                            Log.d(TAG, "FCM token updated for user: " + userId))
                    .addOnFailureListener(e ->
                            Log.e(TAG, "Failed to update FCM token", e));
        }
    }

    /**
     * Called when an FCM message is received while the app is in foreground.
     */
    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        Log.d(TAG, "Message received from: " + remoteMessage.getFrom());

        String title = null;
        String body = null;
        String itemId = null;
        String notifType = null;

        // Extract notification payload
        if (remoteMessage.getNotification() != null) {
            title = remoteMessage.getNotification().getTitle();
            body = remoteMessage.getNotification().getBody();
        }

        // Extract data payload (overrides notification payload values if present)
        Map<String, String> data = remoteMessage.getData();
        if (!data.isEmpty()) {
            if (data.containsKey("title")) title = data.get("title");
            if (data.containsKey("body")) body = data.get("body");
            if (data.containsKey("itemId")) itemId = data.get("itemId");
            if (data.containsKey("type")) notifType = data.get("type");
        }

        if (title == null) title = getString(R.string.app_name);
        if (body == null) body = "You have a new notification";

        buildAndShowNotification(title, body, itemId, notifType);
    }

    private void buildAndShowNotification(String title, String body,
                                           String itemId, String type) {
        createNotificationChannel();

        // Build PendingIntent to open ItemDetailActivity if itemId is available
        Intent intent;
        if (itemId != null) {
            intent = new Intent(this, ItemDetailActivity.class);
            intent.putExtra(Constants.EXTRA_ITEM_ID, itemId);
        } else {
            intent = getPackageManager().getLaunchIntentForPackage(getPackageName());
            if (intent == null) return;
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        int requestCode = (int) System.currentTimeMillis();
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, requestCode, intent,
                PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(
                this, Constants.NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_placeholder_image)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                .setContentIntent(pendingIntent);

        NotificationManager manager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify(NOTIFICATION_ID_BASE + requestCode % 1000, builder.build());
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    Constants.NOTIFICATION_CHANNEL_ID,
                    Constants.NOTIFICATION_CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("Notifications for lost and found item matches and chats");
            channel.enableVibration(true);

            NotificationManager manager =
                    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }
}
