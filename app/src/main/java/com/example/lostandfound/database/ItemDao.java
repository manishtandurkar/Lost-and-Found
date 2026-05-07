package com.example.lostandfound.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface ItemDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(ItemEntity item);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<ItemEntity> items);

    @Update
    void update(ItemEntity item);

    @Delete
    void delete(ItemEntity item);

    @Query("DELETE FROM items_cache WHERE id = :itemId")
    void deleteById(String itemId);

    @Query("DELETE FROM items_cache")
    void deleteAll();

    @Query("SELECT * FROM items_cache ORDER BY timestamp DESC")
    LiveData<List<ItemEntity>> getAll();

    @Query("SELECT * FROM items_cache ORDER BY timestamp DESC")
    List<ItemEntity> getAllSync();

    @Query("SELECT * FROM items_cache WHERE type = :type ORDER BY timestamp DESC")
    LiveData<List<ItemEntity>> getByType(String type);

    @Query("SELECT * FROM items_cache WHERE posted_by = :userId ORDER BY timestamp DESC")
    LiveData<List<ItemEntity>> getByUser(String userId);

    @Query("SELECT * FROM items_cache WHERE id = :itemId LIMIT 1")
    ItemEntity getById(String itemId);

    @Query("SELECT * FROM items_cache WHERE category = :category AND type = :type ORDER BY timestamp DESC")
    List<ItemEntity> getByCategoryAndType(String category, String type);

    @Query("UPDATE items_cache SET status = :status WHERE id = :itemId")
    void updateStatus(String itemId, String status);

    @Query("SELECT COUNT(*) FROM items_cache")
    int getCount();
}
