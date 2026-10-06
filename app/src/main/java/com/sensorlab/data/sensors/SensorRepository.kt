package com.sensorlab.data.sensors

import android.os.Build
import com.sensorlab.core.model.DeviceInfo
import com.sensorlab.core.model.SensorInfo
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import javax.inject.Inject
import javax.inject.Named

class SensorRepository @Inject constructor(
    private val sensorSource: SensorSource,
    @Named("filesDir") private val filesDir: File,
    @Named("appVersion") private val appVersion: String,
    @Named("ioDispatcher") private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private val json = Json {
        allowSpecialFloatingPointValues = true
        ignoreUnknownKeys = true
    }

    suspend fun getDeviceInfo(): DeviceInfo = withContext(ioDispatcher) {
        val deviceJsonFile = File(filesDir, "device.json")
        var currentDevice: DeviceInfo? = null
        if (deviceJsonFile.exists()) {
            try {
                currentDevice = json.decodeFromString<DeviceInfo>(deviceJsonFile.readText())
            } catch (e: Exception) {
                // Parse failed, ignore
            }
        }

        if (currentDevice != null && 
            currentDevice.appVersion == appVersion && 
            currentDevice.buildFingerprint == Build.FINGERPRINT) {
            return@withContext currentDevice
        }

        val rawSensors = sensorSource.getSensors()
        val sensors = rawSensors.map { raw ->
            SensorInfo(
                key = SensorNaming.getSensorKey(raw, rawSensors),
                type = raw.type,
                typeName = raw.stringType,
                name = raw.name,
                vendor = raw.vendor,
                version = raw.version,
                maxRange = raw.maxRange,
                resolution = raw.resolution,
                powerMa = raw.powerMa,
                minDelayUs = raw.minDelayUs,
                maxDelayUs = raw.maxDelayUs,
                fifoMaxEventCount = raw.fifoMaxEventCount,
                fifoReservedEventCount = raw.fifoReservedEventCount,
                reportingMode = raw.reportingMode,
                isWakeUp = raw.isWakeUp,
                isDynamic = raw.isDynamic,
                category = SensorNaming.getCategory(raw.type)
            )
        }

        val newDevice = DeviceInfo(
            manufacturer = Build.MANUFACTURER ?: "Unknown",
            model = Build.MODEL ?: "Unknown",
            androidVersion = Build.VERSION.RELEASE ?: "Unknown",
            apiLevel = Build.VERSION.SDK_INT,
            buildFingerprint = Build.FINGERPRINT ?: "Unknown",
            appVersion = appVersion,
            sensors = sensors
        )

        val tempFile = File(filesDir, "device.json.tmp")
        try {
            val jsonString = json.encodeToString(newDevice)
            tempFile.writeText(jsonString)
            
            val success = tempFile.renameTo(deviceJsonFile)
            if (!success) {
                deviceJsonFile.writeText(jsonString)
                tempFile.delete()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        newDevice
    }
}