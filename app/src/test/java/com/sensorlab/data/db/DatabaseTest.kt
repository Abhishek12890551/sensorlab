package com.sensorlab.data.db

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.sensorlab.data.db.dao.LabelPresetDao
import com.sensorlab.data.db.dao.MarkerDao
import com.sensorlab.data.db.dao.SessionDao
import com.sensorlab.data.db.dao.SessionSensorDao
import com.sensorlab.data.db.entity.LabelPresetEntity
import com.sensorlab.data.db.entity.MarkerEntity
import com.sensorlab.data.db.entity.SessionEntity
import com.sensorlab.data.db.entity.SessionSensorEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class DatabaseTest {

    private lateinit var db: AppDatabase
    private lateinit var sessionDao: SessionDao
    private lateinit var sessionSensorDao: SessionSensorDao
    private lateinit var markerDao: MarkerDao
    private lateinit var labelPresetDao: LabelPresetDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(
            context, AppDatabase::class.java
        ).allowMainThreadQueries().build()
        sessionDao = db.sessionDao()
        sessionSensorDao = db.sessionSensorDao()
        markerDao = db.markerDao()
        labelPresetDao = db.labelPresetDao()
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun insertAndReadSessionWithSensorsAndMarkers() = runBlocking {
        val session = SessionEntity(
            id = "session_123", name = "Test Session", label = "walking", notes = null,
            phonePosition = "pocket", startWallMs = 1000L, endWallMs = 5000L,
            bootOffsetNs = 1000000L, status = "complete", interruptionReason = null,
            sizeBytes = 1024, folderPath = "/path"
        )
        sessionDao.insertSession(session)

        val sensor = SessionSensorEntity(
            sessionId = "session_123", sensorKey = "accel_0", sensorType = 1,
            name = "Accelerometer", requestedHz = 50f, effectiveHz = 49.5f,
            sampleCount = 200, droppedCount = 0, minDtNs = 1000, maxDtNs = 2000, medianDtNs = 1500
        )
        sessionSensorDao.insertSensors(listOf(sensor))

        val marker = MarkerEntity(
            sessionId = "session_123", tNs = 2000000L, label = "jump", note = null
        )
        markerDao.insertMarker(marker)

        val loadedSession = sessionDao.getSessionById("session_123")
        assertEquals("Test Session", loadedSession?.name)

        val loadedSensors = sessionSensorDao.getSensorsForSession("session_123")
        assertEquals(1, loadedSensors.size)
        assertEquals("accel_0", loadedSensors[0].sensorKey)

        val loadedMarkers = markerDao.getMarkersForSession("session_123").first()
        assertEquals(1, loadedMarkers.size)
        assertEquals("jump", loadedMarkers[0].label)
    }

    @Test
    fun deleteSessionCascadesToSensorsAndMarkers() = runBlocking {
        val session = SessionEntity(
            id = "session_123", name = "Test", label = null, notes = null, phonePosition = null,
            startWallMs = 0L, endWallMs = null, bootOffsetNs = 0L, status = "recording",
            interruptionReason = null, sizeBytes = 0L, folderPath = ""
        )
        sessionDao.insertSession(session)
        sessionSensorDao.insertSensors(listOf(
            SessionSensorEntity("session_123", "accel", 1, "A", 1f, 1f, 1, 0, 1, 1, 1)
        ))
        markerDao.insertMarker(MarkerEntity(sessionId = "session_123", tNs = 0L, label = "M", note = null))

        assertEquals(1, sessionSensorDao.getSensorsForSession("session_123").size)
        assertEquals(1, markerDao.getMarkersForSession("session_123").first().size)

        sessionDao.deleteSession("session_123")

        assertEquals(0, sessionSensorDao.getSensorsForSession("session_123").size)
        assertEquals(0, markerDao.getMarkersForSession("session_123").first().size)
    }
    
    @Test
    fun labelPresetOrderingIsRespected() = runBlocking {
        labelPresetDao.insertPreset(LabelPresetEntity(name = "Second", color = 0, sortOrder = 2))
        labelPresetDao.insertPreset(LabelPresetEntity(name = "First", color = 0, sortOrder = 1))
        labelPresetDao.insertPreset(LabelPresetEntity(name = "Third", color = 0, sortOrder = 3))

        val presets = labelPresetDao.getAllPresets().first()
        assertEquals(3, presets.size)
        assertEquals("First", presets[0].name)
        assertEquals("Second", presets[1].name)
        assertEquals("Third", presets[2].name)
    }
}
