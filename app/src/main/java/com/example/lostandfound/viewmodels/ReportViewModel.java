package com.example.lostandfound.viewmodels;

import android.app.Application;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import com.example.lostandfound.models.Item;
import com.example.lostandfound.repository.ItemRepository;

public class ReportViewModel extends AndroidViewModel {

    private final ItemRepository itemRepository;

    private final MutableLiveData<String> submittedItemId = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> photoUploadFailed = new MutableLiveData<>(false);

    // Form state persisted across rotation
    private Uri selectedPhotoUri = null;
    private double selectedLatitude = 0.0;
    private double selectedLongitude = 0.0;
    private String selectedLocationName = "";

    public ReportViewModel(@NonNull Application application) {
        super(application);
        itemRepository = new ItemRepository(application);
    }

    /**
     * Submit a lost item report.
     */
    public void submitLostItem(String title, String category, String description,
                                String locationName, double lat, double lng,
                                String contactPreference, String postedBy) {
        isLoading.setValue(true);

        Item item = new Item(title, category, description, locationName,
                lat, lng, null, postedBy, contactPreference, Item.TYPE_LOST);

        itemRepository.submitLostItem(item, new ItemRepository.OnItemSubmitCallback() {
            @Override
            public void onSuccess(String itemId) {
                isLoading.postValue(false);
                submittedItemId.postValue(itemId);
            }

            @Override
            public void onError(String message) {
                isLoading.postValue(false);
                errorMessage.postValue(message);
            }
        });
    }

    /**
     * Submit a found item report with optional photo.
     */
    public void submitFoundItem(String title, String category, String description,
                                 String locationName, double lat, double lng,
                                 String contactPreference, String postedBy, Uri photoUri) {
        isLoading.setValue(true);

        Item item = new Item(title, category, description, locationName,
                lat, lng, null, postedBy, contactPreference, Item.TYPE_FOUND);

        itemRepository.submitFoundItem(item, photoUri, new ItemRepository.OnItemSubmitCallback() {
            @Override
            public void onSuccess(String itemId) {
                isLoading.postValue(false);
                submittedItemId.postValue(itemId);
            }

            @Override
            public void onError(String message) {
                isLoading.postValue(false);
                errorMessage.postValue(message);
            }

            @Override
            public void onPhotoUploadFailed() {
                photoUploadFailed.postValue(true);
            }
        });
    }

    public void setSelectedPhotoUri(Uri uri) { this.selectedPhotoUri = uri; }
    public Uri getSelectedPhotoUri() { return selectedPhotoUri; }

    public void setSelectedLocation(double lat, double lng, String name) {
        this.selectedLatitude = lat;
        this.selectedLongitude = lng;
        this.selectedLocationName = name;
    }

    public double getSelectedLatitude() { return selectedLatitude; }
    public double getSelectedLongitude() { return selectedLongitude; }
    public String getSelectedLocationName() { return selectedLocationName; }
    public boolean hasLocation() { return selectedLatitude != 0.0 || selectedLongitude != 0.0; }

    public MutableLiveData<String> getSubmittedItemId() { return submittedItemId; }
    public MutableLiveData<String> getErrorMessage() { return errorMessage; }
    public MutableLiveData<Boolean> getIsLoading() { return isLoading; }
    public MutableLiveData<Boolean> getPhotoUploadFailed() { return photoUploadFailed; }
}
