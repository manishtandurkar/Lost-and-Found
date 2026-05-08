package com.example.lostandfound.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.lostandfound.R;
import com.example.lostandfound.adapters.NotificationAdapter;
import com.example.lostandfound.models.NotificationItem;
import com.example.lostandfound.utils.Constants;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class NotificationActivity extends AppCompatActivity {

    private static final String PREF_NOTIFS = "notif_history";
    private static final String KEY_NOTIFS = "notifs_json";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification);

        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        setTitle("Notifications");

        RecyclerView rv = findViewById(R.id.rvNotifications);
        TextView tvEmpty = findViewById(R.id.tvEmptyNotifications);

        NotificationAdapter adapter = new NotificationAdapter(item -> {
            if (item.getItemId() != null && !item.getItemId().isEmpty()) {
                Intent intent = new Intent(this, ItemDetailActivity.class);
                intent.putExtra(Constants.EXTRA_ITEM_ID, item.getItemId());
                intent.putExtra(Constants.EXTRA_ITEM_TYPE, "lost");
                startActivity(intent);
            }
        });
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(adapter);

        List<NotificationItem> items = loadNotifications();
        adapter.setItems(items);
        tvEmpty.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private List<NotificationItem> loadNotifications() {
        SharedPreferences prefs = getSharedPreferences(PREF_NOTIFS, MODE_PRIVATE);
        String json = prefs.getString(KEY_NOTIFS, "[]");
        List<NotificationItem> list = new ArrayList<>();
        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                list.add(new NotificationItem(
                        obj.optString("title"),
                        obj.optString("body"),
                        obj.optString("itemId"),
                        obj.optLong("timestamp")));
            }
        } catch (Exception ignored) {}
        return list;
    }

    public static void saveNotification(android.content.Context ctx, NotificationItem item) {
        SharedPreferences prefs = ctx.getSharedPreferences(PREF_NOTIFS, android.content.Context.MODE_PRIVATE);
        String json = prefs.getString(KEY_NOTIFS, "[]");
        try {
            JSONArray arr = new JSONArray(json);
            JSONObject obj = new JSONObject();
            obj.put("title", item.getTitle());
            obj.put("body", item.getBody());
            obj.put("itemId", item.getItemId());
            obj.put("timestamp", item.getTimestamp());
            arr.put(obj);
            prefs.edit().putString(KEY_NOTIFS, arr.toString()).apply();
        } catch (Exception ignored) {}
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
