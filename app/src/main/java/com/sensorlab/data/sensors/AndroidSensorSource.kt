package com.sensorlab.data.sensors

import android.hardware.Sensor
import android.hardware.SensorManager
import com.sensorlab.core.model.RawSensor
import javax.inject.Inject

class AndroidSensorSource @Inject constructor(
    private val sensorManager: SensorManager
) : SensorSource {
    override fun getSensors(): List<RawSensor> {
        return sensorManager.getSensorList(Sensor.TYPE_ALL).map { s ->
            RawSensor(
                type = s.type,
                stringType = s.stringType,
                name = s.name,
                vendor = s.vendor,
                version = s.version,
                maxRange = s.maximumRange,
                resolution = s.resolution,
                powerMa = s.power,
                minDelayUs = s.minDelay,
                maxDelayUs = s.maxDelay,
                fifoMaxEventCount = s.fifoMaxEventCount,
                fifoReservedEventCount = s.fifoReservedEventCount,
                reportingMode = s.reportingMode,
                isWakeUp = s.isWakeUpSensor,
                isDynamic = s.isDynamicSensor
            )
        }
    }
}