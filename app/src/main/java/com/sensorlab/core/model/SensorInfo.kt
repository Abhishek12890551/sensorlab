package com.sensorlab.core.model

import kotlinx.serialization.Serializable

@Serializable
data class SensorInfo(
    val key: String,
    val type: Int,
    val typeName: String,
    val name: String,
    val vendor: String,
    val version: Int,
    val maxRange: Float,
    val resolution: Float,
    val powerMa: Float,
    val minDelayUs: Int,
    val maxDelayUs: Int,
    val fifoMaxEventCount: Int,
    val fifoReservedEventCount: Int,
    val reportingMode: Int,
    val isWakeUp: Boolean,
    val isDynamic: Boolean,
    val category: String
)