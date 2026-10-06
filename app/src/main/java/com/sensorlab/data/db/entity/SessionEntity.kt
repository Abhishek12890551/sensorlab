package com.sensorlab.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "session")
data class SessionEntity(
    @PrimaryKey val id: String,
    val name: String,
    val label: String?,
    val notes: String?,
    @ColumnInfo(name = "phone_position") val phonePosition: String?,
    @ColumnInfo(name = "start_wall_ms") val startWallMs: Long,
    @ColumnInfo(name = "end_wall_ms") val endWallMs: Long?,
    @ColumnInfo(name = "boot_offset_ns") val bootOffsetNs: Long,
    val status: String,
    @ColumnInfo(name = "interruption_reason") val interruptionReason: String?,
    @ColumnInfo(name = "size_bytes") val sizeBytes: Long,
    @ColumnInfo(name = "folder_path") val folderPath: String
)

