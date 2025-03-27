package com.ruhanazevedo.workoutgenerator.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.ruhanazevedo.workoutgenerator.data.db.entity.UserPreferencesEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserPreferencesDao {
    @Query("SELECT * FROM user_preferences WHERE id = 1")
    fun get(): Flow<UserPreferencesEntity?>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertDefaults(prefs: UserPreferencesEntity)

    @Update
    suspend fun update(prefs: UserPreferencesEntity)
}
