package com.sensorlab.core.model

data class RawSensor(
    val type: Int,
    val stringType: String,
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
    val isDynamic: Boolean
)