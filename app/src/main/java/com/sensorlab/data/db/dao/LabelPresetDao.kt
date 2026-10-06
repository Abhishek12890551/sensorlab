package com.sensorlab.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.sensorlab.data.db.entity.LabelPresetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LabelPresetDao {
    @Query("SELECT * FROM label_preset ORDER BY sort_order ASC")
    fun getAllPresets(): Flow<List<LabelPresetEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPreset(preset: LabelPresetEntity)

    @Query("DELETE FROM label_preset WHERE id = :id")
    suspend fun deletePreset(id: Long)
}

