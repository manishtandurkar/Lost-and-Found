package com.example.lostandfound.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.lostandfound.R;
import com.example.lostandfound.adapters.ChatAdapter;
import com.example.lostandfound.utils.Constants;
import com.example.lostandfound.utils.SessionManager;
import com.example.lostandfound.viewmodels.ChatViewModel;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;

public class ChatActivity extends AppCompatActivity {

    private ChatViewModel viewModel;
    private ChatAdapter adapter;
    private SessionManager sessionManager;
    private RecyclerView recyclerView;
    private EditText etMessage;
    private ProgressBar progressBar;
    private String chatId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        setTitle("Chat");

        sessionManager = new SessionManager(this);
        viewModel = new ViewModelProvider(this).get(ChatViewModel.class);

        String itemId = getIntent().getStringExtra(Constants.EXTRA_ITEM_ID);
        String otherUserId = getIntent().getStringExtra(Constants.EXTRA_OTHER_USER_ID);
        String myId = sessionManager.getUserId();

        chatId = viewModel.getOrCreateChatId(myId, otherUserId, itemId);
        viewModel.initChat(chatId, myId, otherUserId, itemId);

        recyclerView = findViewById(R.id.rvChat);
        etMessage = findViewById(R.id.etMessage);
        progressBar = findViewById(R.id.progressBarChat);
        MaterialButton btnSend = findViewById(R.id.btnSend);

        adapter = new ChatAdapter(myId);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        btnSend.setOnClickListener(v -> sendMessage(myId));

        viewModel.messages.observe(this, messages -> {
            if (messages != null) {
                adapter.setMessages(messages);
                if (!messages.isEmpty()) {
                    recyclerView.smoothScrollToPosition(messages.size() - 1);
                }
            }
        });

        viewModel.sendStatus.observe(this, status -> {
            if (status != null && status.startsWith("error:")) {
                Snackbar.make(recyclerView, "Failed to send message", Snackbar.LENGTH_SHORT).show();
            }
        });

        viewModel.listenForMessages(chatId);
    }

    private void sendMessage(String myId) {
        String text = etMessage.getText().toString().trim();
        if (text.isEmpty()) return;
        etMessage.setText("");
        viewModel.sendMessage(chatId, myId, text);
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
