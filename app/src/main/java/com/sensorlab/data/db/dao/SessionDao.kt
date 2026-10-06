package com.sensorlab.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.sensorlab.data.db.entity.SessionEntity
import com.sensorlab.data.db.entity.SessionSensorEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {
    @Query("SELECT * FROM session ORDER BY start_wall_ms DESC")
    fun getAllSessions(): Flow<List<SessionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: SessionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSessionSensors(sensors: List<SessionSensorEntity>)

    @Query("SELECT * FROM session WHERE id = :id")
    suspend fun getSessionById(id: String): SessionEntity?

    @Query("SELECT * FROM session_sensor WHERE session_id = :sessionId")
    suspend fun getSensorsForSession(sessionId: String): List<SessionSensorEntity>

    @Query("DELETE FROM session WHERE id = :id")
    suspend fun deleteSession(id: String)
}

