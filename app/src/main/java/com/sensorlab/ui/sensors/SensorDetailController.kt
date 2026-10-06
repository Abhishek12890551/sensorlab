package com.sensorlab.ui.sensors

import android.hardware.SensorManager
import com.sensorlab.core.model.RateStats
import com.sensorlab.core.model.SensorInfo
import com.sensorlab.data.sensors.SensorEventSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.max

sealed class PreviewState {
    object NotSupported : PreviewState()
    object Waiting : PreviewState()
    data class Active(val values: FloatArray) : PreviewState()
    data class Error(val message: String) : PreviewState()
    data class Paused(val message: String = "paused") : PreviewState()
}

class SensorDetailController(
    private val sensor: SensorInfo,
    private val source: SensorEventSource,
    private val coroutineScope: CoroutineScope
) {
    private val _previewState = MutableStateFlow<PreviewState>(
        if (sensor.reportingMode == 2 || sensor.reportingMode == 3) PreviewState.NotSupported else PreviewState.Waiting
    )
    val previewState: StateFlow<PreviewState> = _previewState

    private val _rateStats = MutableStateFlow<RateStats?>(null)
    val rateStats: StateFlow<RateStats?> = _rateStats
    val isTestingRate = MutableStateFlow(false)

    private var isPreviewing = false

    @Volatile private var eventCount = 0
    @Volatile private var lastEventLength = 0
    private var previewValues = FloatArray(16)

    @Volatile private var testCount = 0
    @Volatile private var testFirstTs = 0L
    @Volatile private var testLastTs = 0L
    @Volatile private var testMinInterval = Long.MAX_VALUE
    @Volatile private var testMaxInterval = Long.MIN_VALUE

    private var previewTimeoutJob: Job? = null
    private var previewUpdateJob: Job? = null
    private var rateTestJob: Job? = null

    fun startPreview() {
        if (isTestingRate.value) return
        if (isPreviewing) return
        if (sensor.reportingMode == 2 || sensor.reportingMode == 3) return

        isPreviewing = true
        
        stopInternal(pauseState = true)

        eventCount = 0
        lastEventLength = 0
        _previewState.value = PreviewState.Waiting
        
        val registered = source.register(sensor, SensorManager.SENSOR_DELAY_UI) { ts, values ->
            eventCount++
            lastEventLength = values.size
            val len = minOf(values.size, previewValues.size)
            System.arraycopy(values, 0, previewValues, 0, len)
        }

        if (!registered) {
            _previewState.value = PreviewState.Error("unavailable or needs a permission")
            isPreviewing = false
            return
        }

        previewTimeoutJob = coroutineScope.launch {
            delay(3000)
            if (isActive && eventCount == 0) {
                val msg = if (sensor.reportingMode == 0) "no events received (permission or sensor issue)" else "waiting for a change"
                _previewState.value = PreviewState.Error(msg)
            }
        }

        previewUpdateJob = coroutineScope.launch {
            while (isActive) {
                delay(100)
                if (eventCount > 0) {
                    val len = minOf(lastEventLength, previewValues.size)
                    _previewState.value = PreviewState.Active(previewValues.copyOfRange(0, len))
                }
            }
        }
    }

    fun startRateTest() {
        if (sensor.reportingMode != 0 || sensor.minDelayUs <= 0) return
        if (isTestingRate.value) return
        
        stopInternal(pauseState = false)
        
        _previewState.value = PreviewState.Paused("paused (rate test running)")
        isTestingRate.value = true
        testCount = 0
        testFirstTs = 0L
        testLastTs = 0L
        testMinInterval = Long.MAX_VALUE
        testMaxInterval = Long.MIN_VALUE

        val requestedPeriodUs = max(sensor.minDelayUs, 5000)
        val requestedHz = 1_000_000 / requestedPeriodUs

        val registered = source.register(sensor, requestedPeriodUs) { ts, values ->
            testCount++
            if (testCount == 1) {
                testFirstTs = ts
                testLastTs = ts
            } else {
                val interval = ts - testLastTs
                if (interval < testMinInterval) testMinInterval = interval
                if (interval > testMaxInterval) testMaxInterval = interval
                testLastTs = ts
            }
        }

        if (!registered) {
            _rateStats.value = RateStats(requestedHz, 0f, 0f, 0f, 0f, "unavailable or needs a permission")
            isTestingRate.value = false
            startPreview()
            return
        }

        rateTestJob = coroutineScope.launch {
            delay(5000)
            if (isActive) {
                source.unregister()
                _rateStats.value = RateStats.compute(
                    count = testCount,
                    firstTs = testFirstTs,
                    lastTs = testLastTs,
                    minIntervalNs = if (testMinInterval == Long.MAX_VALUE) 0L else testMinInterval,
                    maxIntervalNs = if (testMaxInterval == Long.MIN_VALUE) 0L else testMaxInterval,
                    requestedHz = requestedHz
                )
                isTestingRate.value = false
                isPreviewing = false
                startPreview()
            }
        }
    }

    private fun stopInternal(pauseState: Boolean) {
        source.unregister()
        previewTimeoutJob?.cancel()
        previewUpdateJob?.cancel()
        rateTestJob?.cancel()
        previewTimeoutJob = null
        previewUpdateJob = null
        rateTestJob = null
        if (pauseState && !isTestingRate.value && sensor.reportingMode != 2 && sensor.reportingMode != 3) {
            _previewState.value = PreviewState.Paused("paused")
        }
    }

    fun stop() {
        isPreviewing = false
        stopInternal(pauseState = true)
        if (isTestingRate.value) {
            isTestingRate.value = false
        }
    }
}