package com.example.lostandfound.repository;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.lostandfound.database.AppDatabase;
import com.example.lostandfound.database.ItemDao;
import com.example.lostandfound.database.ItemEntity;
import com.example.lostandfound.models.Item;
import com.example.lostandfound.utils.Constants;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ItemRepository {

    private static final String TAG = "ItemRepository";

    private final DatabaseReference lostItemsRef;
    private final DatabaseReference foundItemsRef;
    private final StorageReference storageRef;
    private final ItemDao itemDao;
    private final ExecutorService executor;

    private final MutableLiveData<List<Item>> allItemsLiveData = new MutableLiveData<>();
    private final MutableLiveData<String> errorLiveData = new MutableLiveData<>();

    private final List<Item> lostItemsList = new ArrayList<>();
    private final List<Item> foundItemsList = new ArrayList<>();

    private ValueEventListener lostListener;
    private ValueEventListener foundListener;

    public ItemRepository(Context context) {
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        lostItemsRef = database.getReference(Constants.NODE_LOST_ITEMS);
        foundItemsRef = database.getReference(Constants.NODE_FOUND_ITEMS);
        storageRef = FirebaseStorage.getInstance().getReference();
        itemDao = AppDatabase.getInstance(context).itemDao();
        executor = Executors.newFixedThreadPool(3);
    }

    /**
     * Start real-time Firebase listeners that merge lost + found items and update LiveData.
     */
    public void startListening() {
        lostListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                lostItemsList.clear();
                for (DataSnapshot child : snapshot.getChildren()) {
                    Item item = child.getValue(Item.class);
                    if (item != null) {
                        item.setId(child.getKey());
                        item.setType(Constants.ITEM_TYPE_LOST);
                        lostItemsList.add(item);
                        cacheItem(item);
                    }
                }
                mergeLists();
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.e(TAG, "Lost items listener cancelled", error.toException());
                errorLiveData.postValue(error.getMessage());
            }
        };

        foundListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                foundItemsList.clear();
                for (DataSnapshot child : snapshot.getChildren()) {
                    Item item = child.getValue(Item.class);
                    if (item != null) {
                        item.setId(child.getKey());
                        item.setType(Constants.ITEM_TYPE_FOUND);
                        foundItemsList.add(item);
                        cacheItem(item);
                    }
                }
                mergeLists();
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.e(TAG, "Found items listener cancelled", error.toException());
                errorLiveData.postValue(error.getMessage());
            }
        };

        lostItemsRef.addValueEventListener(lostListener);
        foundItemsRef.addValueEventListener(foundListener);
    }

    /**
     * Stop Firebase listeners to avoid memory leaks.
     */
    public void stopListening() {
        if (lostListener != null) {
            lostItemsRef.removeEventListener(lostListener);
        }
        if (foundListener != null) {
            foundItemsRef.removeEventListener(foundListener);
        }
    }

    private void mergeLists() {
        List<Item> merged = new ArrayList<>();
        merged.addAll(lostItemsList);
        merged.addAll(foundItemsList);
        // Sort by timestamp descending
        merged.sort((a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()));
        allItemsLiveData.postValue(merged);
    }

    private void cacheItem(Item item) {
        executor.execute(() -> {
            ItemEntity entity = mapItemToEntity(item);
            itemDao.insert(entity);
        });
    }

    /**
     * Submit a new lost item to Firebase. Returns item ID or error via callbacks.
     */
    public void submitLostItem(Item item, OnItemSubmitCallback callback) {
        String itemId = lostItemsRef.push().getKey();
        if (itemId == null) {
            callback.onError("Failed to generate item ID");
            return;
        }
        item.setId(itemId);
        item.setType(Constants.ITEM_TYPE_LOST);

        lostItemsRef.child(itemId).setValue(item)
                .addOnSuccessListener(unused -> {
                    cacheItem(item);
                    callback.onSuccess(itemId);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to submit lost item", e);
                    callback.onError(e.getMessage());
                });
    }

    /**
     * Submit a new found item to Firebase with optional photo upload.
     */
    public void submitFoundItem(Item item, Uri photoUri, OnItemSubmitCallback callback) {
        String itemId = foundItemsRef.push().getKey();
        if (itemId == null) {
            callback.onError("Failed to generate item ID");
            return;
        }
        item.setId(itemId);
        item.setType(Constants.ITEM_TYPE_FOUND);

        if (photoUri != null) {
            uploadPhoto(itemId, photoUri, new OnPhotoUploadCallback() {
                @Override
                public void onSuccess(String downloadUrl) {
                    item.setPhotoUrl(downloadUrl);
                    saveFoundItem(item, itemId, callback);
                }

                @Override
                public void onFailure() {
                    // Submit without photo if upload fails
                    saveFoundItem(item, itemId, callback);
                    callback.onPhotoUploadFailed();
                }
            });
        } else {
            saveFoundItem(item, itemId, callback);
        }
    }

    private void saveFoundItem(Item item, String itemId, OnItemSubmitCallback callback) {
        foundItemsRef.child(itemId).setValue(item)
                .addOnSuccessListener(unused -> {
                    cacheItem(item);
                    callback.onSuccess(itemId);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to submit found item", e);
                    callback.onError(e.getMessage());
                });
    }

    /**
     * Upload photo to Firebase Storage and return download URL.
     */
    private void uploadPhoto(String itemId, Uri photoUri, OnPhotoUploadCallback callback) {
        StorageReference photoRef = storageRef
                .child(Constants.STORAGE_ITEM_PHOTOS + itemId + ".jpg");

        photoRef.putFile(photoUri)
                .addOnSuccessListener(taskSnapshot ->
                    photoRef.getDownloadUrl()
                            .addOnSuccessListener(uri -> callback.onSuccess(uri.toString()))
                            .addOnFailureListener(e -> callback.onFailure())
                )
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Photo upload failed", e);
                    callback.onFailure();
                });
    }

    /**
     * Mark an item as resolved in Firebase.
     */
    public void resolveItem(String itemId, String itemType, OnCompleteCallback callback) {
        DatabaseReference ref = Constants.ITEM_TYPE_LOST.equals(itemType)
                ? lostItemsRef : foundItemsRef;

        ref.child(itemId).child(Constants.FIELD_STATUS).setValue(Constants.STATUS_RESOLVED)
                .addOnSuccessListener(unused -> {
                    executor.execute(() -> itemDao.updateStatus(itemId, Constants.STATUS_RESOLVED));
                    callback.onComplete(true);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to resolve item", e);
                    callback.onComplete(false);
                });
    }

    /**
     * Load items from Room cache (for offline use).
     */
    public LiveData<List<ItemEntity>> getCachedItems() {
        return itemDao.getAll();
    }

    public LiveData<List<ItemEntity>> getCachedItemsByType(String type) {
        return itemDao.getByType(type);
    }

    public LiveData<List<ItemEntity>> getCachedItemsByUser(String userId) {
        return itemDao.getByUser(userId);
    }

    public LiveData<List<Item>> getAllItemsLiveData() {
        return allItemsLiveData;
    }

    public MutableLiveData<String> getErrorLiveData() {
        return errorLiveData;
    }

    /**
     * Fetch all cached items synchronously for matching service.
     */
    public List<ItemEntity> getCachedItemsByCategoryAndTypeSync(String category, String oppositeType) {
        return itemDao.getByCategoryAndType(category, oppositeType);
    }

    private ItemEntity mapItemToEntity(Item item) {
        return new ItemEntity(
                item.getId(),
                item.getType(),
                item.getTitle(),
                item.getCategory(),
                item.getDescription(),
                item.getLocationName(),
                item.getLatitude(),
                item.getLongitude(),
                item.getPhotoUrl(),
                item.getPostedBy(),
                item.getContactPreference(),
                item.getStatus(),
                item.getTimestamp()
        );
    }

    public interface OnItemSubmitCallback {
        void onSuccess(String itemId);
        void onError(String message);
        default void onPhotoUploadFailed() {}
    }

    public interface OnPhotoUploadCallback {
        void onSuccess(String downloadUrl);
        void onFailure();
    }

    public interface OnCompleteCallback {
        void onComplete(boolean success);
    }
}
