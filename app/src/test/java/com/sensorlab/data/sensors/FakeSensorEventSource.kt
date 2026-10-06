package com.sensorlab.data.sensors

import com.sensorlab.core.model.SensorInfo

class FakeSensorEventSource(private val failRegistration: Boolean = false) : SensorEventSource {
    var isRegistered = false
        private set
    var registeredDelayUs = 0
        private set
    var registerCount = 0
        private set
    
    private var callback: ((Long, FloatArray) -> Unit)? = null

    override fun register(
        sensorInfo: SensorInfo,
        delayUs: Int,
        onEvent: (timestamp: Long, values: FloatArray) -> Unit
    ): Boolean {
        if (failRegistration) return false
        isRegistered = true
        registeredDelayUs = delayUs
        registerCount++
        callback = onEvent
        return true
    }

    override fun unregister() {
        isRegistered = false
        callback = null
    }

    fun emit(timestamp: Long, values: FloatArray) {
        callback?.invoke(timestamp, values)
    }
}