package com.example.lostandfound.viewmodels;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.example.lostandfound.database.ItemEntity;
import com.example.lostandfound.models.Item;
import com.example.lostandfound.repository.ItemRepository;
import com.example.lostandfound.utils.Constants;

import java.util.ArrayList;
import java.util.List;

public class FeedViewModel extends AndroidViewModel {

    public enum FilterType {
        ALL, LOST_ONLY, FOUND_ONLY, MY_POSTS
    }

    private final ItemRepository itemRepository;

    private final MutableLiveData<FilterType> filterType = new MutableLiveData<>(FilterType.ALL);
    private final MutableLiveData<String> currentUserId = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isOffline = new MutableLiveData<>(false);

    // Firebase live items merged
    private final LiveData<List<Item>> firebaseItems;

    // Cached items for offline
    private final LiveData<List<ItemEntity>> cachedItems;

    // Filtered items exposed to UI
    private final MediatorLiveData<List<Item>> filteredItems = new MediatorLiveData<>();

    public FeedViewModel(@NonNull Application application) {
        super(application);
        itemRepository = new ItemRepository(application);

        firebaseItems = itemRepository.getAllItemsLiveData();
        cachedItems = itemRepository.getCachedItems();

        // Observe Firebase items and apply filter
        filteredItems.addSource(firebaseItems, items -> {
            if (items != null) {
                applyFilter(items);
            }
        });

        // Observe filter changes and re-apply
        filteredItems.addSource(filterType, type -> {
            List<Item> items = firebaseItems.getValue();
            if (items != null) {
                applyFilter(items);
            }
        });

        // Observe userId changes and re-apply
        filteredItems.addSource(currentUserId, userId -> {
            List<Item> items = firebaseItems.getValue();
            if (items != null) {
                applyFilter(items);
            }
        });
    }

    private void applyFilter(List<Item> items) {
        FilterType type = filterType.getValue();
        String userId = currentUserId.getValue();
        List<Item> result = new ArrayList<>();

        for (Item item : items) {
            switch (type != null ? type : FilterType.ALL) {
                case LOST_ONLY:
                    if (Constants.ITEM_TYPE_LOST.equals(item.getType())) {
                        result.add(item);
                    }
                    break;
                case FOUND_ONLY:
                    if (Constants.ITEM_TYPE_FOUND.equals(item.getType())) {
                        result.add(item);
                    }
                    break;
                case MY_POSTS:
                    if (userId != null && userId.equals(item.getPostedBy())) {
                        result.add(item);
                    }
                    break;
                case ALL:
                default:
                    result.add(item);
                    break;
            }
        }
        filteredItems.setValue(result);
    }

    /**
     * Start real-time Firebase listeners.
     */
    public void startListening() {
        itemRepository.startListening();
    }

    /**
     * Stop listeners to avoid memory leaks.
     */
    public void stopListening() {
        itemRepository.stopListening();
    }

    /**
     * Load cached items for offline display.
     * Returns LiveData<List<Item>> converted from cached entities.
     */
    public LiveData<List<Item>> getOfflineItems() {
        return Transformations.map(cachedItems, entities -> {
            List<Item> items = new ArrayList<>();
            if (entities != null) {
                for (ItemEntity entity : entities) {
                    items.add(mapEntityToItem(entity));
                }
            }
            return items;
        });
    }

    private Item mapEntityToItem(ItemEntity entity) {
        Item item = new Item();
        item.setId(entity.id);
        item.setType(entity.type);
        item.setTitle(entity.title);
        item.setCategory(entity.category);
        item.setDescription(entity.description);
        item.setLocationName(entity.locationName);
        item.setLatitude(entity.latitude);
        item.setLongitude(entity.longitude);
        item.setPhotoUrl(entity.photoUrl);
        item.setPostedBy(entity.postedBy);
        item.setContactPreference(entity.contactPreference);
        item.setStatus(entity.status);
        item.setTimestamp(entity.timestamp);
        return item;
    }

    public void setFilter(FilterType filter) {
        filterType.setValue(filter);
    }

    public void setCurrentUserId(String userId) {
        currentUserId.setValue(userId);
    }

    public void setOffline(boolean offline) {
        isOffline.setValue(offline);
    }

    public LiveData<List<Item>> getFilteredItems() { return filteredItems; }
    public LiveData<FilterType> getFilterType() { return filterType; }
    public LiveData<Boolean> getIsOffline() { return isOffline; }
    public MutableLiveData<String> getErrorLiveData() { return itemRepository.getErrorLiveData(); }

    @Override
    protected void onCleared() {
        super.onCleared();
        stopListening();
    }
}
