package com.sensorlab.data.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Handler
import android.os.HandlerThread
import com.sensorlab.core.model.SensorInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class AndroidSensorEventSource @Inject constructor(
    @ApplicationContext private val context: Context
) : SensorEventSource {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private var handlerThread: HandlerThread? = null
    private var listener: SensorEventListener? = null

    override fun register(
        sensorInfo: SensorInfo,
        delayUs: Int,
        onEvent: (timestamp: Long, values: FloatArray) -> Unit
    ): Boolean {
        val sensor = sensorManager.getSensorList(Sensor.TYPE_ALL).firstOrNull { 
            it.type == sensorInfo.type && it.name == sensorInfo.name && it.vendor == sensorInfo.vendor 
        } ?: return false

        handlerThread = HandlerThread("SensorThread_${sensorInfo.key}").apply { start() }
        val handler = Handler(handlerThread!!.looper)
        
        listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                onEvent(event.timestamp, event.values)
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
        
        return try {
            val success = sensorManager.registerListener(listener, sensor, delayUs, handler)
            if (!success) {
                cleanup()
            }
            success
        } catch (e: SecurityException) {
            cleanup()
            false
        }
    }

    override fun unregister() {
        cleanup()
    }

    private fun cleanup() {
        listener?.let { sensorManager.unregisterListener(it) }
        listener = null
        handlerThread?.quitSafely()
        handlerThread = null
    }
}