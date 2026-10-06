package com.sensorlab.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "session_sensor",
    primaryKeys = ["session_id", "sensor_key"],
    foreignKeys = [
        ForeignKey(
            entity = SessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["session_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("session_id")]
)
data class SessionSensorEntity(
    @ColumnInfo(name = "session_id") val sessionId: String,
    @ColumnInfo(name = "sensor_key") val sensorKey: String,
    @ColumnInfo(name = "sensor_type") val sensorType: Int,
    val name: String,
    @ColumnInfo(name = "requested_hz") val requestedHz: Float,
    @ColumnInfo(name = "effective_hz") val effectiveHz: Float,
    @ColumnInfo(name = "sample_count") val sampleCount: Long,
    @ColumnInfo(name = "dropped_count") val droppedCount: Long,
    @ColumnInfo(name = "min_dt_ns") val minDtNs: Long,
    @ColumnInfo(name = "max_dt_ns") val maxDtNs: Long,
    @ColumnInfo(name = "median_dt_ns") val medianDtNs: Long
)

