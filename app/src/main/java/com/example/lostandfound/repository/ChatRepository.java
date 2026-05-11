package com.example.lostandfound.repository;

import androidx.lifecycle.MutableLiveData;

import com.example.lostandfound.models.Message;
import com.example.lostandfound.utils.Constants;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.List;

public class ChatRepository {

    private final DatabaseReference chatsRef;
    private ChildEventListener activeListener;
    private String activeChatId;

    public ChatRepository() {
        chatsRef = FirebaseDatabase.getInstance("https://lost-and-found-d65bc-default-rtdb.firebaseio.com").getReference(Constants.DB_CHATS);
    }

    public String getOrCreateChatId(String userId1, String userId2, String itemId) {
        String[] ids = {userId1, userId2};
        java.util.Arrays.sort(ids);
        return ids[0] + "_" + ids[1] + "_" + itemId;
    }

    public void initChat(String chatId, String userId1, String userId2, String itemId) {
        DatabaseReference chatRef = chatsRef.child(chatId);
        chatRef.child("itemId").setValue(itemId);
        List<String> participants = new ArrayList<>();
        participants.add(userId1);
        participants.add(userId2);
        chatRef.child("participants").setValue(participants);
    }

    public void sendMessage(String chatId, Message message, Callback callback) {
        DatabaseReference msgRef = chatsRef.child(chatId).child("messages").push();
        message.setMessageId(msgRef.getKey());
        msgRef.setValue(message)
                .addOnSuccessListener(unused -> {
                    if (callback != null) callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onError(e.getMessage());
                });
    }

    public void listenForMessages(String chatId, MutableLiveData<List<Message>> liveData) {
        if (activeChatId != null && activeListener != null) {
            chatsRef.child(activeChatId).child("messages").removeEventListener(activeListener);
        }
        activeChatId = chatId;
        List<Message> messages = new ArrayList<>();

        activeListener = new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot snapshot, String previousChildName) {
                Message msg = snapshot.getValue(Message.class);
                if (msg != null) {
                    messages.add(msg);
                    liveData.postValue(new ArrayList<>(messages));
                }
            }

            @Override
            public void onChildChanged(DataSnapshot snapshot, String previousChildName) {}

            @Override
            public void onChildRemoved(DataSnapshot snapshot) {}

            @Override
            public void onChildMoved(DataSnapshot snapshot, String previousChildName) {}

            @Override
            public void onCancelled(DatabaseError error) {}
        };

        chatsRef.child(chatId).child("messages").addChildEventListener(activeListener);
    }

    public void removeListener() {
        if (activeChatId != null && activeListener != null) {
            chatsRef.child(activeChatId).child("messages").removeEventListener(activeListener);
        }
    }

    public interface Callback {
        void onSuccess();
        void onError(String message);
    }
}
