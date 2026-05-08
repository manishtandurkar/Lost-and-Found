package com.example.lostandfound.viewmodels;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.lostandfound.models.Message;
import com.example.lostandfound.repository.ChatRepository;

import java.util.List;

public class ChatViewModel extends ViewModel {

    private final ChatRepository repository = new ChatRepository();
    public final MutableLiveData<List<Message>> messages = new MutableLiveData<>();
    public final MutableLiveData<String> sendStatus = new MutableLiveData<>();

    public String getOrCreateChatId(String userId1, String userId2, String itemId) {
        return repository.getOrCreateChatId(userId1, userId2, itemId);
    }

    public void initChat(String chatId, String myId, String otherId, String itemId) {
        repository.initChat(chatId, myId, otherId, itemId);
    }

    public void listenForMessages(String chatId) {
        repository.listenForMessages(chatId, messages);
    }

    public void sendMessage(String chatId, String senderId, String text) {
        Message message = new Message(null, senderId, text, System.currentTimeMillis());
        repository.sendMessage(chatId, message, new ChatRepository.Callback() {
            @Override
            public void onSuccess() {
                sendStatus.postValue("sent");
            }

            @Override
            public void onError(String error) {
                sendStatus.postValue("error:" + error);
            }
        });
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        repository.removeListener();
    }
}
