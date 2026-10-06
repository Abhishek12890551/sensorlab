package com.sensorlab.data.sensors

import android.os.Build
import com.sensorlab.core.model.RawSensor
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.util.ReflectionHelpers
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class SensorRepositoryTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun testDeviceJsonWrittenFirstRun() = runTest {
        val fakeSource = FakeSensorSource(listOf(
            RawSensor(1, "android.sensor.accelerometer", "Accel", "V", 1, Float.NaN, Float.POSITIVE_INFINITY, 1f, 1000, 1000, 0, 0, 0, false, false)
        ))
        val repo = SensorRepository(fakeSource, tempFolder.root, "1.0", StandardTestDispatcher(testScheduler))
        
        val device = repo.getDeviceInfo()
        assertEquals(1, device.sensors.size)
        assertTrue(File(tempFolder.root, "device.json").exists())
        assertTrue(device.sensors[0].maxRange.isNaN())
        assertTrue(device.sensors[0].resolution.isInfinite())
    }

    @Test
    fun testDeviceJsonRefreshedOnVersionChange() = runTest {
        val fakeSource = FakeSensorSource(emptyList())
        val repo1 = SensorRepository(fakeSource, tempFolder.root, "1.0", StandardTestDispatcher(testScheduler))
        repo1.getDeviceInfo()
        
        val repo2 = SensorRepository(fakeSource, tempFolder.root, "2.0", StandardTestDispatcher(testScheduler))
        val device2 = repo2.getDeviceInfo()
        assertEquals("2.0", device2.appVersion)
    }

    @Test
    fun testSameVersionAndFingerprintUsesCache() = runTest {
        val fakeSource = FakeSensorSource(listOf(
            RawSensor(1, "accel", "A", "V", 1, 1f, 1f, 1f, 1, 1, 0, 0, 0, false, false)
        ))
        val repo1 = SensorRepository(fakeSource, tempFolder.root, "1.0", StandardTestDispatcher(testScheduler))
        repo1.getDeviceInfo()

        val fakeSource2 = FakeSensorSource(emptyList())
        val repo2 = SensorRepository(fakeSource2, tempFolder.root, "1.0", StandardTestDispatcher(testScheduler))
        val device2 = repo2.getDeviceInfo()
        assertEquals(1, device2.sensors.size)
    }

    @Test
    fun testChangedFingerprintRefreshes() = runTest {
        val fakeSource = FakeSensorSource(listOf(
            RawSensor(1, "accel", "A", "V", 1, 1f, 1f, 1f, 1, 1, 0, 0, 0, false, false)
        ))
        val repo1 = SensorRepository(fakeSource, tempFolder.root, "1.0", StandardTestDispatcher(testScheduler))
        repo1.getDeviceInfo()

        ReflectionHelpers.setStaticField(Build::class.java, "FINGERPRINT", "new_fingerprint")

        val fakeSource2 = FakeSensorSource(emptyList())
        val repo2 = SensorRepository(fakeSource2, tempFolder.root, "1.0", StandardTestDispatcher(testScheduler))
        val device2 = repo2.getDeviceInfo()
        assertEquals(0, device2.sensors.size)
    }

    @Test
    fun testCorruptJsonRegenerates() = runTest {
        val jsonFile = File(tempFolder.root, "device.json")
        jsonFile.writeText("{ invalid json ")
        
        val fakeSource = FakeSensorSource(emptyList())
        val repo = SensorRepository(fakeSource, tempFolder.root, "1.0", StandardTestDispatcher(testScheduler))
        val device = repo.getDeviceInfo()
        assertEquals(0, device.sensors.size)
    }

    @Test
    fun testZeroSensorsHandled() = runTest {
        val repo = SensorRepository(FakeSensorSource(emptyList()), tempFolder.root, "1.0", StandardTestDispatcher(testScheduler))
        val device = repo.getDeviceInfo()
        assertEquals(0, device.sensors.size)
    }

    @Test
    fun testFallbackAndExceptionOnWrite() = runTest {
        val fakeSource = FakeSensorSource(listOf(
            RawSensor(1, "accel", "A", "V", 1, 1f, 1f, 1f, 1, 1, 0, 0, 0, false, false)
        ))
        val invalidDir = File("/invalid_path_that_should_fail")
        val repo = SensorRepository(fakeSource, invalidDir, "1.0", StandardTestDispatcher(testScheduler))
        
        val device = repo.getDeviceInfo()
        assertEquals(1, device.sensors.size)
    }
}