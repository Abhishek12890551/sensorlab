package com.sensorlab.core.model

import kotlinx.serialization.Serializable

@Serializable
data class DeviceInfo(
    val manufacturer: String,
    val model: String,
    val androidVersion: String,
    val apiLevel: Int,
    val buildFingerprint: String,
    val appVersion: String,
    val sensors: List<SensorInfo>
)