package com.example.lostandfound.viewmodels;

import android.app.Application;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import com.example.lostandfound.models.Item;
import com.example.lostandfound.repository.ItemRepository;
import com.example.lostandfound.utils.Constants;
import com.example.lostandfound.utils.SessionManager;

public class ReportViewModel extends AndroidViewModel {

    private final ItemRepository repository;
    private final SessionManager sessionManager;

    public final MutableLiveData<String> postStatus = new MutableLiveData<>();
    public final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);

    public ReportViewModel(@NonNull Application application) {
        super(application);
        repository = new ItemRepository(application);
        sessionManager = new SessionManager(application);
    }

    public void submitItem(String title, String category, String description,
                           String locationName, double latitude, double longitude,
                           Uri photoUri, String contactPreference, String type) {
        isLoading.setValue(true);

        Item item = new Item();
        item.setTitle(title);
        item.setCategory(category);
        item.setDescription(description);
        item.setLocationName(locationName);
        item.setLatitude(latitude);
        item.setLongitude(longitude);
        item.setPostedBy(sessionManager.getUserId());
        item.setContactPreference(contactPreference);
        item.setStatus(Constants.STATUS_ACTIVE);
        item.setTimestamp(System.currentTimeMillis());
        item.setType(type);

        repository.postItem(item, photoUri, new ItemRepository.PostCallback() {
            @Override
            public void onSuccess(Item postedItem) {
                isLoading.postValue(false);
                postStatus.postValue("success:" + postedItem.getId());
            }

            @Override
            public void onError(String message) {
                isLoading.postValue(false);
                postStatus.postValue("error:" + message);
            }
        });
    }
}
