package com.sensorlab.data.sensors

import com.sensorlab.core.model.SensorInfo

interface SensorEventSource {
    fun register(sensor: SensorInfo, delayUs: Int, onEvent: (timestamp: Long, values: FloatArray) -> Unit): Boolean
    fun unregister()
}