package com.example.lostandfound.notifications;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;

import androidx.core.app.NotificationCompat;

import com.example.lostandfound.R;
import com.example.lostandfound.activities.ItemDetailActivity;
import com.example.lostandfound.activities.NotificationActivity;
import com.example.lostandfound.models.NotificationItem;
import com.example.lostandfound.repository.UserRepository;
import com.example.lostandfound.utils.Constants;
import com.example.lostandfound.utils.SessionManager;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class FCMService extends FirebaseMessagingService {

    @Override
    public void onNewToken(String token) {
        super.onNewToken(token);
        SessionManager session = new SessionManager(this);
        String userId = session.getUserId();
        if (userId != null) {
            new UserRepository().updateFcmToken(userId, token);
        }
    }

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        String title = remoteMessage.getNotification() != null
                ? remoteMessage.getNotification().getTitle() : "Lost & Found";
        String body = remoteMessage.getNotification() != null
                ? remoteMessage.getNotification().getBody() : "";
        String itemId = remoteMessage.getData().get("itemId");
        String itemType = remoteMessage.getData().getOrDefault("itemType", "lost");

        // Save to notification history
        NotificationActivity.saveNotification(this,
                new NotificationItem(title, body, itemId, System.currentTimeMillis()));

        // Build deep-link intent
        Intent intent;
        if (itemId != null && !itemId.isEmpty()) {
            intent = new Intent(this, ItemDetailActivity.class);
            intent.putExtra(Constants.EXTRA_ITEM_ID, itemId);
            intent.putExtra(Constants.EXTRA_ITEM_TYPE, itemType);
        } else {
            intent = new Intent(this, NotificationActivity.class);
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent,
                PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE);

        createNotificationChannel();

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, Constants.CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(body)
                .setAutoCancel(true)
                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                .setContentIntent(pendingIntent);

        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify((int) System.currentTimeMillis(), builder.build());
        }
    }

    private void createNotificationChannel() {
        NotificationChannel channel = new NotificationChannel(
                Constants.CHANNEL_ID,
                Constants.CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH);
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) manager.createNotificationChannel(channel);
    }
}
