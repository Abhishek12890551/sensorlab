package com.sensorlab.data.sensors

import com.sensorlab.core.model.RawSensor

interface SensorSource {
    fun getSensors(): List<RawSensor>
}