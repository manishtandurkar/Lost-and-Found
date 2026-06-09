package com.example.lostandfound.workers;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.lostandfound.activities.ItemDetailActivity;
import com.example.lostandfound.utils.Constants;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import androidx.core.app.NotificationCompat;

import com.example.lostandfound.R;

public class NewItemWorker extends Worker {

    private static final String PREFS_NAME = "NewItemWorkerPrefs";
    private static final String KEY_LAST_CHECK = "last_check_ts";
    private static final long DEFAULT_LOOKBACK_MS = 15 * 60 * 1000L; // 15 min

    public NewItemWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        SharedPreferences prefs = getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        long lastCheck = prefs.getLong(KEY_LAST_CHECK,
                System.currentTimeMillis() - DEFAULT_LOOKBACK_MS);
        long now = System.currentTimeMillis();

        AtomicInteger notifId = new AtomicInteger((int) (now % Integer.MAX_VALUE));
        CountDownLatch latch = new CountDownLatch(2);

        FirebaseDatabase db = FirebaseDatabase.getInstance(
                "https://lost-and-found-d65bc-default-rtdb.firebaseio.com");

        checkNode(db, Constants.DB_LOST_ITEMS, Constants.TYPE_LOST, lastCheck, notifId, latch);
        checkNode(db, Constants.DB_FOUND_ITEMS, Constants.TYPE_FOUND, lastCheck, notifId, latch);

        try {
            latch.await(20, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        prefs.edit().putLong(KEY_LAST_CHECK, now).apply();
        return Result.success();
    }

    private void checkNode(FirebaseDatabase db, String node, String type,
                           long since, AtomicInteger notifId, CountDownLatch latch) {
        Query query = db.getReference(node)
                .orderByChild("timestamp")
                .startAt(since + 1);

        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot child : snapshot.getChildren()) {
                    String title = child.child("title").getValue(String.class);
                    String category = child.child("category").getValue(String.class);
                    String locationName = child.child("locationName").getValue(String.class);
                    String itemId = child.getKey();
                    if (title == null) continue;

                    String notifTitle = (Constants.TYPE_LOST.equals(type) ? "Lost: " : "Found: ") + title;
                    String notifBody = (category != null ? category : "") +
                            (locationName != null ? " · " + locationName : "");

                    postNotification(notifTitle, notifBody, itemId, type, notifId.incrementAndGet());
                }
                latch.countDown();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                latch.countDown();
            }
        });
    }

    private void postNotification(String title, String body, String itemId,
                                  String itemType, int id) {
        Context ctx = getApplicationContext();
        NotificationManager manager =
                (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null) return;

        NotificationChannel channel = new NotificationChannel(
                Constants.CHANNEL_ID, Constants.CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT);
        manager.createNotificationChannel(channel);

        Intent intent = new Intent(ctx, ItemDetailActivity.class);
        intent.putExtra(Constants.EXTRA_ITEM_ID, itemId);
        intent.putExtra(Constants.EXTRA_ITEM_TYPE, itemType);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        PendingIntent pending = PendingIntent.getActivity(ctx, id, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(ctx, Constants.CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(body)
                .setAutoCancel(true)
                .setContentIntent(pending);

        manager.notify(id, builder.build());
    }
}
