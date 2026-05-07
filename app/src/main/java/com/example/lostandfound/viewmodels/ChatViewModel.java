package com.example.lostandfound.viewmodels;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import com.example.lostandfound.models.ChatMessage;
import com.example.lostandfound.utils.Constants;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.List;

public class ChatViewModel extends AndroidViewModel {

    private static final String TAG = "ChatViewModel";

    private DatabaseReference messagesRef;
    private ChildEventListener messageListener;

    private final MutableLiveData<List<ChatMessage>> messagesLiveData =
            new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<String> errorLiveData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> messageSentLiveData = new MutableLiveData<>();

    private String currentChatId;

    public ChatViewModel(@NonNull Application application) {
        super(application);
    }

    /**
     * Initialize chat for a specific chatId. Creates the chat node if it doesn't exist.
     */
    public void initChat(String chatId, String itemId, String userId, String otherUserId) {
        this.currentChatId = chatId;

        DatabaseReference chatRef = FirebaseDatabase.getInstance()
                .getReference(Constants.NODE_CHATS)
                .child(chatId);

        // Set participants and item reference
        chatRef.child("itemId").setValue(itemId);
        chatRef.child("participants").child(userId).setValue(true);
        chatRef.child("participants").child(otherUserId).setValue(true);

        messagesRef = chatRef.child(Constants.NODE_MESSAGES);
        startListeningForMessages();
    }

    private void startListeningForMessages() {
        messageListener = new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot snapshot, String previousChildName) {
                ChatMessage message = snapshot.getValue(ChatMessage.class);
                if (message != null) {
                    message.setMessageId(snapshot.getKey());
                    List<ChatMessage> currentList = new ArrayList<>();
                    if (messagesLiveData.getValue() != null) {
                        currentList.addAll(messagesLiveData.getValue());
                    }
                    currentList.add(message);
                    messagesLiveData.postValue(currentList);
                }
            }

            @Override
            public void onChildChanged(DataSnapshot snapshot, String previousChildName) {}

            @Override
            public void onChildRemoved(DataSnapshot snapshot) {}

            @Override
            public void onChildMoved(DataSnapshot snapshot, String previousChildName) {}

            @Override
            public void onCancelled(DatabaseError error) {
                Log.e(TAG, "Message listener cancelled", error.toException());
                errorLiveData.postValue(error.getMessage());
            }
        };

        messagesRef.addChildEventListener(messageListener);
    }

    /**
     * Send a chat message.
     */
    public void sendMessage(String senderId, String senderName, String text) {
        if (messagesRef == null || text == null || text.trim().isEmpty()) return;

        ChatMessage message = new ChatMessage(senderId, senderName, text.trim());
        String messageId = messagesRef.push().getKey();
        if (messageId == null) {
            errorLiveData.postValue("Failed to generate message ID");
            return;
        }

        messagesRef.child(messageId).setValue(message)
                .addOnSuccessListener(unused -> messageSentLiveData.postValue(true))
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to send message", e);
                    errorLiveData.postValue("Failed to send message: " + e.getMessage());
                });
    }

    /**
     * Generate a deterministic chat ID from two user IDs and item ID.
     */
    public static String generateChatId(String userId1, String userId2, String itemId) {
        // Sort user IDs to ensure same chat ID regardless of who initiates
        String combined = userId1.compareTo(userId2) < 0
                ? userId1 + "_" + userId2
                : userId2 + "_" + userId1;
        return combined + "_" + itemId;
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (messagesRef != null && messageListener != null) {
            messagesRef.removeEventListener(messageListener);
        }
    }

    public MutableLiveData<List<ChatMessage>> getMessagesLiveData() { return messagesLiveData; }
    public MutableLiveData<String> getErrorLiveData() { return errorLiveData; }
    public MutableLiveData<Boolean> getMessageSentLiveData() { return messageSentLiveData; }
}
