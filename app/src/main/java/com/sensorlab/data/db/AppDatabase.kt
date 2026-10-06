package com.sensorlab.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.sensorlab.data.db.dao.LabelPresetDao
import com.sensorlab.data.db.dao.MarkerDao
import com.sensorlab.data.db.dao.SessionDao
import com.sensorlab.data.db.dao.SessionSensorDao
import com.sensorlab.data.db.entity.LabelPresetEntity
import com.sensorlab.data.db.entity.MarkerEntity
import com.sensorlab.data.db.entity.SessionEntity
import com.sensorlab.data.db.entity.SessionSensorEntity

@Database(
    entities = [
        SessionEntity::class,
        SessionSensorEntity::class,
        MarkerEntity::class,
        LabelPresetEntity::class
    ],
    version = 1
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun sessionDao(): SessionDao
    abstract fun sessionSensorDao(): SessionSensorDao
    abstract fun markerDao(): MarkerDao
    abstract fun labelPresetDao(): LabelPresetDao
}

