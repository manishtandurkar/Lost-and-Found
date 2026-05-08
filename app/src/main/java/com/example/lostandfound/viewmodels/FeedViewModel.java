package com.example.lostandfound.viewmodels;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.lostandfound.database.ItemEntity;
import com.example.lostandfound.repository.ItemRepository;

import java.util.List;

public class FeedViewModel extends AndroidViewModel {

    private final ItemRepository repository;
    private final MutableLiveData<String> syncStatus = new MutableLiveData<>();

    public FeedViewModel(@NonNull Application application) {
        super(application);
        repository = new ItemRepository(application);
    }

    public LiveData<List<ItemEntity>> getAllCachedItems() {
        return repository.getCachedItems();
    }

    public LiveData<List<ItemEntity>> getItemsByType(String type) {
        return repository.getCachedItemsByType(type);
    }

    public LiveData<List<ItemEntity>> getMyItems(String userId) {
        return repository.getCachedItemsByUser(userId);
    }

    public LiveData<String> getSyncStatus() {
        return syncStatus;
    }

    public void syncFromFirebase() {
        syncStatus.setValue("syncing");
        repository.syncFromFirebase(new ItemRepository.SyncCallback() {
            @Override
            public void onSuccess() {
                syncStatus.postValue("success");
            }

            @Override
            public void onError(String message) {
                syncStatus.postValue("error:" + message);
            }
        });
    }
}
