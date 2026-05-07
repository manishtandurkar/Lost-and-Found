package com.example.lostandfound.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.lostandfound.R;
import com.example.lostandfound.models.Notification;
import com.example.lostandfound.utils.Constants;
import com.example.lostandfound.utils.SessionManager;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class NotificationActivity extends AppCompatActivity {

    private RecyclerView recyclerNotifications;
    private View layoutEmpty;

    private final List<Notification> notificationList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> finish());

        recyclerNotifications = findViewById(R.id.recycler_notifications);
        layoutEmpty = findViewById(R.id.layout_empty);

        SessionManager sessionManager = new SessionManager(this);
        String userId = sessionManager.getUserId();
        if (userId == null) {
            finish();
            return;
        }

        // Simple list using a basic adapter — notifications stored in Firebase
        loadNotifications(userId);
    }

    private void loadNotifications(String userId) {
        FirebaseDatabase.getInstance()
                .getReference(Constants.NODE_NOTIFICATIONS)
                .child(userId)
                .orderByChild("timestamp")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        notificationList.clear();
                        for (DataSnapshot child : snapshot.getChildren()) {
                            Notification notif = child.getValue(Notification.class);
                            if (notif != null) {
                                notif.setNotificationId(child.getKey());
                                notificationList.add(0, notif); // newest first
                            }
                        }

                        if (notificationList.isEmpty()) {
                            layoutEmpty.setVisibility(View.VISIBLE);
                            recyclerNotifications.setVisibility(View.GONE);
                        } else {
                            layoutEmpty.setVisibility(View.GONE);
                            recyclerNotifications.setVisibility(View.VISIBLE);
                            setupRecycler();
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        layoutEmpty.setVisibility(View.VISIBLE);
                    }
                });
    }

    private void setupRecycler() {
        // Simple TextView-based adapter for notifications
        recyclerNotifications.setLayoutManager(new LinearLayoutManager(this));
        recyclerNotifications.setAdapter(new androidx.recyclerview.widget.RecyclerView.Adapter<NotifViewHolder>() {

            @Override
            public NotifViewHolder onCreateViewHolder(android.view.ViewGroup parent, int viewType) {
                android.widget.TextView tv = new android.widget.TextView(parent.getContext());
                tv.setPadding(32, 24, 32, 24);
                tv.setLayoutParams(new android.view.ViewGroup.LayoutParams(
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                        android.view.ViewGroup.LayoutParams.WRAP_CONTENT));
                return new NotifViewHolder(tv);
            }

            @Override
            public void onBindViewHolder(NotifViewHolder holder, int position) {
                Notification notif = notificationList.get(position);
                holder.textView.setText(notif.getTitle() + "\n" + notif.getBody());
                holder.textView.setTextSize(14);
                if (notif.getItemId() != null) {
                    holder.textView.setOnClickListener(v -> {
                        Intent intent = new Intent(NotificationActivity.this,
                                ItemDetailActivity.class);
                        intent.putExtra(Constants.EXTRA_ITEM_ID, notif.getItemId());
                        startActivity(intent);
                    });
                }
            }

            @Override
            public int getItemCount() { return notificationList.size(); }
        });
    }

    static class NotifViewHolder extends RecyclerView.ViewHolder {
        android.widget.TextView textView;
        NotifViewHolder(android.widget.TextView tv) {
            super(tv);
            this.textView = tv;
        }
    }
}
