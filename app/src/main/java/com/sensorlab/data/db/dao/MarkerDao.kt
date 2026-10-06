package com.sensorlab.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.sensorlab.data.db.entity.MarkerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MarkerDao {
    @Query("SELECT * FROM marker WHERE session_id = :sessionId ORDER BY t_ns ASC")
    fun getMarkersForSession(sessionId: String): Flow<List<MarkerEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMarker(marker: MarkerEntity)

    @Query("DELETE FROM marker WHERE id = :id")
    suspend fun deleteMarker(id: Long)
}

