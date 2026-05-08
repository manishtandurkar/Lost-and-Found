package com.example.lostandfound.repository;

import android.content.Context;
import android.net.Uri;

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
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class ItemRepository {

    private final DatabaseReference dbRef;
    private final StorageReference storageRef;
    private final ItemDao itemDao;
    private final Executor executor;

    public ItemRepository(Context context) {
        dbRef = FirebaseDatabase.getInstance().getReference();
        storageRef = FirebaseStorage.getInstance().getReference("item_photos");
        itemDao = AppDatabase.getInstance(context).itemDao();
        executor = Executors.newSingleThreadExecutor();
    }

    public LiveData<List<ItemEntity>> getCachedItems() {
        return itemDao.getAllItems();
    }

    public LiveData<List<ItemEntity>> getCachedItemsByType(String type) {
        return itemDao.getItemsByType(type);
    }

    public LiveData<List<ItemEntity>> getCachedItemsByUser(String userId) {
        return itemDao.getItemsByUser(userId);
    }

    public void syncFromFirebase(SyncCallback callback) {
        List<Item> allItems = new ArrayList<>();
        final int[] pendingRequests = {2};

        ValueEventListener lostListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                for (DataSnapshot child : snapshot.getChildren()) {
                    Item item = child.getValue(Item.class);
                    if (item != null) {
                        item.setId(child.getKey());
                        item.setType(Constants.TYPE_LOST);
                        allItems.add(item);
                    }
                }
                pendingRequests[0]--;
                if (pendingRequests[0] == 0) persistAndNotify(allItems, callback);
            }

            @Override
            public void onCancelled(DatabaseError error) {
                if (callback != null) callback.onError(error.getMessage());
            }
        };

        ValueEventListener foundListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                for (DataSnapshot child : snapshot.getChildren()) {
                    Item item = child.getValue(Item.class);
                    if (item != null) {
                        item.setId(child.getKey());
                        item.setType(Constants.TYPE_FOUND);
                        allItems.add(item);
                    }
                }
                pendingRequests[0]--;
                if (pendingRequests[0] == 0) persistAndNotify(allItems, callback);
            }

            @Override
            public void onCancelled(DatabaseError error) {
                if (callback != null) callback.onError(error.getMessage());
            }
        };

        dbRef.child(Constants.DB_LOST_ITEMS).addListenerForSingleValueEvent(lostListener);
        dbRef.child(Constants.DB_FOUND_ITEMS).addListenerForSingleValueEvent(foundListener);
    }

    private void persistAndNotify(List<Item> items, SyncCallback callback) {
        executor.execute(() -> {
            List<ItemEntity> entities = new ArrayList<>();
            for (Item item : items) {
                entities.add(toEntity(item));
            }
            itemDao.deleteAll();
            itemDao.insertAll(entities);
            if (callback != null) callback.onSuccess();
        });
    }

    public void postItem(Item item, Uri photoUri, PostCallback callback) {
        String node = item.getType().equals(Constants.TYPE_LOST)
                ? Constants.DB_LOST_ITEMS : Constants.DB_FOUND_ITEMS;
        DatabaseReference itemRef = dbRef.child(node).push();
        item.setId(itemRef.getKey());

        if (photoUri != null) {
            StorageReference photoRef = storageRef.child(item.getId() + ".jpg");
            photoRef.putFile(photoUri)
                    .addOnSuccessListener(taskSnapshot ->
                            photoRef.getDownloadUrl().addOnSuccessListener(uri -> {
                                item.setPhotoUrl(uri.toString());
                                writeItemToFirebase(itemRef, item, callback);
                            }))
                    .addOnFailureListener(e -> writeItemToFirebase(itemRef, item, callback));
        } else {
            writeItemToFirebase(itemRef, item, callback);
        }
    }

    private void writeItemToFirebase(DatabaseReference ref, Item item, PostCallback callback) {
        ref.setValue(item)
                .addOnSuccessListener(unused -> {
                    executor.execute(() -> itemDao.insertItem(toEntity(item)));
                    if (callback != null) callback.onSuccess(item);
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onError(e.getMessage());
                });
    }

    public void markResolved(String itemId, String type) {
        String node = type.equals(Constants.TYPE_LOST)
                ? Constants.DB_LOST_ITEMS : Constants.DB_FOUND_ITEMS;
        dbRef.child(node).child(itemId).child("status").setValue(Constants.STATUS_RESOLVED);
        executor.execute(() -> itemDao.updateStatus(itemId, Constants.STATUS_RESOLVED));
    }

    public void deleteItem(String itemId, String type, DeleteCallback callback) {
        String node = type.equals(Constants.TYPE_LOST)
                ? Constants.DB_LOST_ITEMS : Constants.DB_FOUND_ITEMS;
        dbRef.child(node).child(itemId).removeValue()
                .addOnSuccessListener(unused -> {
                    executor.execute(() -> itemDao.deleteItem(itemId));
                    if (callback != null) callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onError(e.getMessage());
                });
    }

    public void getItemFromFirebase(String itemId, String type, ItemFetchCallback callback) {
        String node = type.equals(Constants.TYPE_LOST)
                ? Constants.DB_LOST_ITEMS : Constants.DB_FOUND_ITEMS;
        dbRef.child(node).child(itemId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                Item item = snapshot.getValue(Item.class);
                if (item != null) {
                    item.setId(snapshot.getKey());
                    item.setType(type);
                }
                if (callback != null) callback.onFetched(item);
            }

            @Override
            public void onCancelled(DatabaseError error) {
                if (callback != null) callback.onError(error.getMessage());
            }
        });
    }

    private ItemEntity toEntity(Item item) {
        ItemEntity e = new ItemEntity();
        e.id = item.getId();
        e.type = item.getType();
        e.title = item.getTitle();
        e.category = item.getCategory();
        e.description = item.getDescription();
        e.locationName = item.getLocationName();
        e.latitude = item.getLatitude();
        e.longitude = item.getLongitude();
        e.photoUrl = item.getPhotoUrl();
        e.postedBy = item.getPostedBy();
        e.contactPreference = item.getContactPreference();
        e.status = item.getStatus();
        e.timestamp = item.getTimestamp();
        e.lastSyncedAt = System.currentTimeMillis();
        return e;
    }

    public interface SyncCallback {
        void onSuccess();
        void onError(String message);
    }

    public interface PostCallback {
        void onSuccess(Item item);
        void onError(String message);
    }

    public interface DeleteCallback {
        void onSuccess();
        void onError(String message);
    }

    public interface ItemFetchCallback {
        void onFetched(Item item);
        void onError(String message);
    }
}
