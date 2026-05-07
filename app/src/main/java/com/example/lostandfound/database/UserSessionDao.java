package com.example.lostandfound.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

@Dao
public interface UserSessionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(UserSessionEntity session);

    @Update
    void update(UserSessionEntity session);

    @Query("SELECT * FROM user_session LIMIT 1")
    UserSessionEntity getSession();

    @Query("DELETE FROM user_session")
    void clearSession();

    @Query("UPDATE user_session SET fcm_token = :token WHERE user_id = :userId")
    void updateFcmToken(String userId, String token);
}
