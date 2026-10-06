package com.sensorlab.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.sensorlab.data.db.entity.SessionSensorEntity

@Dao
interface SessionSensorDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSensor(sensor: SessionSensorEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSensors(sensors: List<SessionSensorEntity>)

    @Query("SELECT * FROM session_sensor WHERE session_id = :sessionId")
    suspend fun getSensorsForSession(sessionId: String): List<SessionSensorEntity>

    @Query("DELETE FROM session_sensor WHERE session_id = :sessionId AND sensor_key = :sensorKey")
    suspend fun deleteSensor(sessionId: String, sensorKey: String)
}
