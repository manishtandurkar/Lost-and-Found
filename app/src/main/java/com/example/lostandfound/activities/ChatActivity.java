package com.example.lostandfound.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.lostandfound.R;
import com.example.lostandfound.adapters.ChatAdapter;
import com.example.lostandfound.models.ChatMessage;
import com.example.lostandfound.utils.Constants;
import com.example.lostandfound.utils.SessionManager;
import com.example.lostandfound.viewmodels.ChatViewModel;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class ChatActivity extends AppCompatActivity {

    private ChatViewModel chatViewModel;
    private SessionManager sessionManager;

    private RecyclerView recyclerChat;
    private ChatAdapter chatAdapter;
    private TextInputEditText etMessage;
    private FloatingActionButton fabSend;
    private LinearLayout layoutEmpty;

    private String chatId;
    private String itemId;
    private String otherUserId;
    private String otherUserName = "User";
    private String currentUserId;
    private String currentUserName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        sessionManager = new SessionManager(this);
        currentUserId = sessionManager.getUserId();
        currentUserName = sessionManager.getUserName();

        chatId = getIntent().getStringExtra(Constants.EXTRA_CHAT_ID);
        itemId = getIntent().getStringExtra(Constants.EXTRA_ITEM_ID);
        otherUserId = getIntent().getStringExtra(Constants.EXTRA_OTHER_USER_ID);

        chatViewModel = new ViewModelProvider(this).get(ChatViewModel.class);

        initViews();
        loadOtherUserName();
        initChat();
        observeMessages();
    }

    private void initViews() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> finish());

        recyclerChat = findViewById(R.id.recycler_chat);
        etMessage = findViewById(R.id.et_message);
        fabSend = findViewById(R.id.fab_send);
        layoutEmpty = findViewById(R.id.layout_empty);

        chatAdapter = new ChatAdapter(new ArrayList<>(), currentUserId);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        recyclerChat.setLayoutManager(layoutManager);
        recyclerChat.setAdapter(chatAdapter);

        fabSend.setOnClickListener(v -> sendMessage());
    }

    private void loadOtherUserName() {
        if (otherUserId == null) return;
        FirebaseDatabase.getInstance()
                .getReference(Constants.NODE_USERS)
                .child(otherUserId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        String name = snapshot.child("name").getValue(String.class);
                        if (name != null) {
                            otherUserName = name;
                            MaterialToolbar tb = findViewById(R.id.toolbar);
                            if (tb != null) tb.setTitle(name);
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {}
                });
    }

    private void initChat() {
        if (chatId == null || itemId == null || currentUserId == null || otherUserId == null) {
            Snackbar.make(recyclerChat, "Cannot open chat: missing info", Snackbar.LENGTH_LONG).show();
            finish();
            return;
        }
        chatViewModel.initChat(chatId, itemId, currentUserId, otherUserId);
    }

    private void observeMessages() {
        chatViewModel.getMessagesLiveData().observe(this, messages -> {
            if (messages == null || messages.isEmpty()) {
                layoutEmpty.setVisibility(View.VISIBLE);
                recyclerChat.setVisibility(View.GONE);
            } else {
                layoutEmpty.setVisibility(View.GONE);
                recyclerChat.setVisibility(View.VISIBLE);
                chatAdapter.setMessages(messages);
                recyclerChat.scrollToPosition(messages.size() - 1);
            }
        });

        chatViewModel.getErrorLiveData().observe(this, error -> {
            if (error != null) {
                Snackbar.make(recyclerChat, "Error: " + error, Snackbar.LENGTH_LONG).show();
            }
        });
    }

    private void sendMessage() {
        if (etMessage.getText() == null) return;
        String text = etMessage.getText().toString().trim();
        if (text.isEmpty()) return;

        chatViewModel.sendMessage(currentUserId,
                currentUserName != null ? currentUserName : "Unknown",
                text);
        etMessage.setText("");
    }
}
