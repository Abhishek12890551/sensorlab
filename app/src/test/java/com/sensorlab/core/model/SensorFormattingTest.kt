package com.sensorlab.core.model

import org.junit.Assert.assertEquals
import org.junit.Test

class SensorFormattingTest {

    @Test
    fun testFormatMaxHz() {
        assertEquals("125 Hz", SensorFormatting.formatMaxHz(8000, 0))
        assertEquals("fastest", SensorFormatting.formatMaxHz(0, 0))
        assertEquals("on change (min interval 200 ms)", SensorFormatting.formatMaxHz(200000, 1))
        assertEquals("on change", SensorFormatting.formatMaxHz(0, 1))
        assertEquals("one-shot", SensorFormatting.formatMaxHz(10000, 2))
        assertEquals("special trigger", SensorFormatting.formatMaxHz(0, 3))
    }

    @Test
    fun testRateStatsPerfect100Hz() {
        val stats = RateStats.compute(
            count = 501,
            firstTs = 0L,
            lastTs = 5_000_000_000L,
            minIntervalNs = 10_000_000L,
            maxIntervalNs = 10_000_000L,
            requestedHz = 100
        )
        assertEquals(100f, stats.effectiveHz, 0.1f)
        assertEquals(10f, stats.minIntervalMs, 0.1f)
        assertEquals(10f, stats.meanIntervalMs, 0.1f)
        assertEquals(10f, stats.maxIntervalMs, 0.1f)
    }

    @Test
    fun testRateStatsZeroEvents() {
        val stats = RateStats.compute(0, 0, 0, 0, 0, 100)
        assertEquals("not enough events", stats.message)
    }

    @Test
    fun testRateStatsSingleEvent() {
        val stats = RateStats.compute(1, 1000L, 1000L, 0, 0, 100)
        assertEquals("not enough events", stats.message)
    }

    @Test
    fun testRateStatsBackwardsTimestamps() {
        val stats = RateStats.compute(10, 1000L, 500L, 0, 0, 100)
        assertEquals("invalid timestamps (went backwards)", stats.message)
    }

    @Test
    fun testRateStatsJittered() {
        val stats = RateStats.compute(3, 0L, 20_000_000L, 5_000_000L, 15_000_000L, 100)
        assertEquals(100f, stats.effectiveHz, 0.1f)
        assertEquals(5f, stats.minIntervalMs, 0.1f)
        assertEquals(10f, stats.meanIntervalMs, 0.1f)
        assertEquals(15f, stats.maxIntervalMs, 0.1f)
    }
}