package com.sensorlab.data.sensors

import android.hardware.Sensor
import com.sensorlab.core.model.RawSensor

object SensorNaming {
    @Suppress("DEPRECATION")
    fun getCategory(type: Int): String {
        return when (type) {
            Sensor.TYPE_ACCELEROMETER,
            Sensor.TYPE_GYROSCOPE,
            Sensor.TYPE_GRAVITY,
            Sensor.TYPE_LINEAR_ACCELERATION,
            Sensor.TYPE_STEP_COUNTER,
            Sensor.TYPE_STEP_DETECTOR,
            Sensor.TYPE_SIGNIFICANT_MOTION,
            Sensor.TYPE_ACCELEROMETER_UNCALIBRATED,
            Sensor.TYPE_GYROSCOPE_UNCALIBRATED -> "Motion"

            Sensor.TYPE_MAGNETIC_FIELD,
            Sensor.TYPE_PROXIMITY,
            Sensor.TYPE_ORIENTATION,
            Sensor.TYPE_ROTATION_VECTOR,
            Sensor.TYPE_GAME_ROTATION_VECTOR,
            Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR,
            Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED -> "Position"

            Sensor.TYPE_LIGHT,
            Sensor.TYPE_PRESSURE,
            Sensor.TYPE_TEMPERATURE,
            Sensor.TYPE_AMBIENT_TEMPERATURE,
            Sensor.TYPE_RELATIVE_HUMIDITY -> "Environment"

            else -> "Other"
        }
    }

    @Suppress("DEPRECATION")
    fun getColumnNames(type: Int): List<String> {
        return when (type) {
            Sensor.TYPE_ACCELEROMETER,
            Sensor.TYPE_GRAVITY,
            Sensor.TYPE_LINEAR_ACCELERATION,
            Sensor.TYPE_GYROSCOPE,
            Sensor.TYPE_MAGNETIC_FIELD -> listOf("x", "y", "z")

            Sensor.TYPE_ACCELEROMETER_UNCALIBRATED,
            Sensor.TYPE_GYROSCOPE_UNCALIBRATED,
            Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED -> listOf("x", "y", "z", "bias_x", "bias_y", "bias_z")

            Sensor.TYPE_ROTATION_VECTOR,
            Sensor.TYPE_GAME_ROTATION_VECTOR,
            Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR -> listOf("x", "y", "z", "w", "heading_accuracy")

            Sensor.TYPE_LIGHT,
            Sensor.TYPE_PRESSURE,
            Sensor.TYPE_PROXIMITY,
            Sensor.TYPE_TEMPERATURE,
            Sensor.TYPE_AMBIENT_TEMPERATURE,
            Sensor.TYPE_RELATIVE_HUMIDITY -> listOf("value")

            Sensor.TYPE_STEP_COUNTER -> listOf("steps")
            Sensor.TYPE_HEART_RATE -> listOf("bpm")
            else -> (0..15).map { "v$it" }
        }
    }

    fun getSensorKey(sensor: RawSensor, allSensors: List<RawSensor>): String {
        val getBase = { s: RawSensor -> 
            if (s.stringType.isBlank()) "type_${s.type}" 
            else s.stringType.substringAfterLast('.') 
        }
        val baseName = getBase(sensor)
        
        val sameBaseName = allSensors.filter { getBase(it) == baseName }
            .sortedWith(compareBy({ it.vendor }, { it.name }))
        
        return if (sameBaseName.size > 1) {
            val index = sameBaseName.indexOfFirst { it === sensor }
            "${baseName}_$index"
        } else {
            baseName
        }
    }
}