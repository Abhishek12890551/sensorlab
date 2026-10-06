package com.sensorlab.core.model

object SensorFormatting {
    fun formatMaxHz(minDelayUs: Int, reportingMode: Int): String {
        return when (reportingMode) {
            0 -> if (minDelayUs > 0) "${1_000_000 / minDelayUs} Hz" else "fastest"
            1 -> if (minDelayUs > 0) "on change (min interval ${minDelayUs / 1000} ms)" else "on change"
            2 -> "one-shot"
            3 -> "special trigger"
            else -> "unknown"
        }
    }
}

data class RateStats(
    val requestedHz: Int,
    val effectiveHz: Float,
    val minIntervalMs: Float,
    val meanIntervalMs: Float,
    val maxIntervalMs: Float,
    val message: String? = null
) {
    companion object {
        fun compute(count: Int, firstTs: Long, lastTs: Long, minIntervalNs: Long, maxIntervalNs: Long, requestedHz: Int): RateStats {
            if (count < 2) {
                return RateStats(requestedHz, 0f, 0f, 0f, 0f, "not enough events")
            }
            val elapsedNs = lastTs - firstTs
            if (elapsedNs <= 0) {
                return RateStats(requestedHz, 0f, 0f, 0f, 0f, "invalid timestamps (went backwards)")
            }
            val effectiveHz = (count - 1).toFloat() / (elapsedNs / 1_000_000_000f)
            val meanIntervalMs = (elapsedNs / (count - 1).toFloat()) / 1_000_000f
            return RateStats(
                requestedHz = requestedHz,
                effectiveHz = effectiveHz,
                minIntervalMs = minIntervalNs / 1_000_000f,
                meanIntervalMs = meanIntervalMs,
                maxIntervalMs = maxIntervalNs / 1_000_000f
            )
        }
    }
}