package com.sensorlab.ui.sensors

import com.sensorlab.core.model.SensorInfo
import com.sensorlab.data.sensors.FakeSensorEventSource
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SensorDetailControllerTest {
    
    private val baseSensor = SensorInfo(
        "accel", 1, "android.sensor.accelerometer", "Accel", "Google", 1,
        10f, 0.1f, 1f, 10000, 20000, 0, 0, 0, false, false, "Motion"
    )

    @Test
    fun testRegistrationAndUnregisterOnStop() = runTest {
        val fakeSource = FakeSensorEventSource()
        val controller = SensorDetailController(baseSensor, fakeSource, this)
        
        controller.startPreview()
        assertTrue(fakeSource.isRegistered)
        
        controller.stop()
        assertTrue(!fakeSource.isRegistered)
    }

    @Test
    fun testRegistrationFailure() = runTest {
        val fakeSource = FakeSensorEventSource(failRegistration = true)
        val controller = SensorDetailController(baseSensor, fakeSource, this)
        
        controller.startPreview()
        
        val state = controller.previewState.value
        assertTrue(state is PreviewState.Error)
        assertEquals("unavailable or needs a permission", (state as PreviewState.Error).message)
        controller.stop()
    }

    @Test
    fun test3SecondNoEventTimeoutContinuous() = runTest {
        val fakeSource = FakeSensorEventSource()
        val controller = SensorDetailController(baseSensor.copy(reportingMode = 0), fakeSource, this)
        
        controller.startPreview()
        advanceTimeBy(3500)
        
        val state = controller.previewState.value
        assertTrue(state is PreviewState.Error)
        assertEquals("no events received (permission or sensor issue)", (state as PreviewState.Error).message)
        controller.stop()
    }
    
    @Test
    fun test3SecondNoEventTimeoutOnChange() = runTest {
        val fakeSource = FakeSensorEventSource()
        val controller = SensorDetailController(baseSensor.copy(reportingMode = 1), fakeSource, this)
        
        controller.startPreview()
        advanceTimeBy(3500)
        
        val state = controller.previewState.value
        assertTrue(state is PreviewState.Error)
        assertEquals("waiting for a change", (state as PreviewState.Error).message)
        controller.stop()
    }

    @Test
    fun testRateComputedFromFakeEvents() = runTest {
        val fakeSource = FakeSensorEventSource()
        val controller = SensorDetailController(baseSensor.copy(reportingMode = 0, minDelayUs = 10000), fakeSource, this)
        
        controller.startRateTest()
        assertTrue(fakeSource.isRegistered)
        assertEquals(10000, fakeSource.registeredDelayUs)

        fakeSource.emit(0L, floatArrayOf(0f))
        fakeSource.emit(10_000_000L, floatArrayOf(0f))
        fakeSource.emit(20_000_000L, floatArrayOf(0f))

        advanceTimeBy(5500)
        
        assertTrue(!controller.isTestingRate.value)
        val stats = controller.rateStats.value!!
        assertEquals(100f, stats.effectiveHz, 0.1f)
        controller.stop()
    }

    @Test
    fun testOnStopThenRestartWorks() = runTest {
        val fakeSource = FakeSensorEventSource()
        val controller = SensorDetailController(baseSensor, fakeSource, this)
        controller.startPreview()
        assertTrue(fakeSource.isRegistered)
        controller.stop()
        assertTrue(!fakeSource.isRegistered)
        assertTrue(controller.previewState.value is PreviewState.Paused)
        
        controller.startPreview()
        assertTrue(fakeSource.isRegistered)
        controller.stop()
    }

    @Test
    fun testPreviewRowCountFollowsEventLength() = runTest {
        val fakeSource = FakeSensorEventSource()
        val controller = SensorDetailController(baseSensor, fakeSource, this)
        controller.startPreview()
        
        fakeSource.emit(0L, floatArrayOf(1f, 2f))
        advanceTimeBy(200)
        
        val state1 = controller.previewState.value as PreviewState.Active
        assertEquals(2, state1.values.size)
        
        fakeSource.emit(0L, floatArrayOf(1f, 2f, 3f, 4f))
        advanceTimeBy(200)
        
        val state2 = controller.previewState.value as PreviewState.Active
        assertEquals(4, state2.values.size)
        
        controller.stop()
    }

    @Test
    fun testStartPreviewTwiceDoesNotRegisterTwice() = runTest {
        val fakeSource = FakeSensorEventSource()
        val controller = SensorDetailController(baseSensor, fakeSource, this)
        controller.startPreview()
        controller.startPreview()
        assertEquals(1, fakeSource.registerCount)
        controller.stop()
    }

    @Test
    fun testStartPreviewDuringRateTestDoesNotBreakTest() = runTest {
        val fakeSource = FakeSensorEventSource()
        val controller = SensorDetailController(baseSensor.copy(reportingMode = 0, minDelayUs = 10000), fakeSource, this)
        controller.startRateTest()
        
        fakeSource.emit(0L, floatArrayOf(0f))
        fakeSource.emit(10_000_000L, floatArrayOf(0f))
        
        controller.startPreview() // Should be ignored
        assertTrue(controller.isTestingRate.value)
        
        fakeSource.emit(20_000_000L, floatArrayOf(0f))
        
        advanceTimeBy(5500)
        val stats = controller.rateStats.value!!
        assertEquals(100f, stats.effectiveHz, 0.1f)
        controller.stop()
    }

    @Test
    fun testStopDuringRateTestResetsIsTestingRate() = runTest {
        val fakeSource = FakeSensorEventSource()
        val controller = SensorDetailController(baseSensor.copy(reportingMode = 0, minDelayUs = 10000), fakeSource, this)
        controller.startRateTest()
        advanceTimeBy(1000)
        assertTrue(controller.isTestingRate.value)
        controller.stop()
        assertTrue(!fakeSource.isRegistered)
        assertTrue(!controller.isTestingRate.value)
        advanceTimeBy(5000)
        assertEquals(null, controller.rateStats.value)
    }

    @Test
    fun testRateTestCompletesRegistersAgainForPreview() = runTest {
        val fakeSource = FakeSensorEventSource()
        val controller = SensorDetailController(baseSensor.copy(reportingMode = 0, minDelayUs = 10000), fakeSource, this)
        controller.startRateTest()
        val initialRegisterCount = fakeSource.registerCount
        advanceTimeBy(5500)
        assertTrue(!controller.isTestingRate.value)
        assertTrue(fakeSource.isRegistered)
        assertEquals(initialRegisterCount + 1, fakeSource.registerCount)
        controller.stop()
    }

    @Test
    fun testStateShowsPausedMessageDuringRateTest() = runTest {
        val fakeSource = FakeSensorEventSource()
        val controller = SensorDetailController(baseSensor.copy(reportingMode = 0, minDelayUs = 10000), fakeSource, this)
        controller.startRateTest()
        val state = controller.previewState.value
        assertTrue(state is PreviewState.Paused)
        assertEquals("paused (rate test running)", (state as PreviewState.Paused).message)
        controller.stop()
    }
}