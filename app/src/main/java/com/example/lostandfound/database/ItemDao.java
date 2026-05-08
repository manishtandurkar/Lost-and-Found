package com.example.lostandfound.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface ItemDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertItem(ItemEntity item);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<ItemEntity> items);

    @Query("SELECT * FROM items_cache ORDER BY timestamp DESC")
    LiveData<List<ItemEntity>> getAllItems();

    @Query("SELECT * FROM items_cache WHERE type = :type ORDER BY timestamp DESC")
    LiveData<List<ItemEntity>> getItemsByType(String type);

    @Query("SELECT * FROM items_cache WHERE posted_by = :userId ORDER BY timestamp DESC")
    LiveData<List<ItemEntity>> getItemsByUser(String userId);

    @Query("SELECT * FROM items_cache WHERE id = :id LIMIT 1")
    ItemEntity getItemById(String id);

    @Query("DELETE FROM items_cache WHERE id = :id")
    void deleteItem(String id);

    @Query("DELETE FROM items_cache")
    void deleteAll();

    @Query("UPDATE items_cache SET status = :status WHERE id = :id")
    void updateStatus(String id, String status);
}
