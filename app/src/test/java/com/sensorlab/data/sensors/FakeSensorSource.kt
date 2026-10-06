package com.sensorlab.data.sensors

import com.sensorlab.core.model.RawSensor

class FakeSensorSource(var fakeSensors: List<RawSensor>) : SensorSource {
    override fun getSensors() = fakeSensors
}