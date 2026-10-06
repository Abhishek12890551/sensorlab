package com.sensorlab.data.sensors

import android.hardware.Sensor
import com.sensorlab.core.model.RawSensor
import org.junit.Assert.assertEquals
import org.junit.Test

class SensorNamingTest {

    private fun createRaw(type: Int, stringType: String, name: String = "Test", vendor: String = "Vendor"): RawSensor {
        return RawSensor(type, stringType, name, vendor, 1, 1f, 1f, 1f, 10000, 20000, 0, 0, 0, false, false)
    }

    @Test
    fun testStableNamingWithDuplicates() {
        val s1 = createRaw(1, "com.vendor.accel", "Accel A", "Vendor 1")
        val s2 = createRaw(1, "org.other.accel", "Accel B", "Vendor 2")
        val s3 = createRaw(99, "", "Custom", "Vendor 3")
        val s4 = createRaw(99, "", "Custom", "Vendor 4")
        val s5 = createRaw(5, "android.sensor.light", "Light", "Google")
        val s6 = createRaw(5, "android.sensor.light", "Light", "Google")
        
        val list = listOf(s1, s2, s3, s4, s5, s6)
        
        val key1 = SensorNaming.getSensorKey(s1, list)
        val key2 = SensorNaming.getSensorKey(s2, list)
        val key3 = SensorNaming.getSensorKey(s3, list)
        val key4 = SensorNaming.getSensorKey(s4, list)
        val key5 = SensorNaming.getSensorKey(s5, list)
        val key6 = SensorNaming.getSensorKey(s6, list)

        assertEquals("accel_0", key1)
        assertEquals("accel_1", key2)
        assertEquals("type_99_0", key3)
        assertEquals("type_99_1", key4)
        assertEquals("light_0", key5)
        assertEquals("light_1", key6)

        val allKeys = setOf(key1, key2, key3, key4, key5, key6)
        assertEquals(6, allKeys.size)
    }

    @Test
    fun testNamingUnknownType() {
        val s1 = createRaw(99, "com.vendor.custom_sensor")
        val key = SensorNaming.getSensorKey(s1, listOf(s1))
        assertEquals("custom_sensor", key)
    }

    @Suppress("DEPRECATION")
    @Test
    fun testCategories() {
        assertEquals("Motion", SensorNaming.getCategory(Sensor.TYPE_ACCELEROMETER))
        assertEquals("Motion", SensorNaming.getCategory(Sensor.TYPE_GYROSCOPE))
        assertEquals("Motion", SensorNaming.getCategory(Sensor.TYPE_GRAVITY))
        assertEquals("Motion", SensorNaming.getCategory(Sensor.TYPE_LINEAR_ACCELERATION))
        assertEquals("Motion", SensorNaming.getCategory(Sensor.TYPE_STEP_COUNTER))
        assertEquals("Motion", SensorNaming.getCategory(Sensor.TYPE_STEP_DETECTOR))
        assertEquals("Motion", SensorNaming.getCategory(Sensor.TYPE_SIGNIFICANT_MOTION))
        assertEquals("Motion", SensorNaming.getCategory(Sensor.TYPE_ACCELEROMETER_UNCALIBRATED))
        assertEquals("Motion", SensorNaming.getCategory(Sensor.TYPE_GYROSCOPE_UNCALIBRATED))

        assertEquals("Position", SensorNaming.getCategory(Sensor.TYPE_MAGNETIC_FIELD))
        assertEquals("Position", SensorNaming.getCategory(Sensor.TYPE_PROXIMITY))
        assertEquals("Position", SensorNaming.getCategory(Sensor.TYPE_ORIENTATION))
        assertEquals("Position", SensorNaming.getCategory(Sensor.TYPE_ROTATION_VECTOR))
        assertEquals("Position", SensorNaming.getCategory(Sensor.TYPE_GAME_ROTATION_VECTOR))
        assertEquals("Position", SensorNaming.getCategory(Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR))
        assertEquals("Position", SensorNaming.getCategory(Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED))

        assertEquals("Environment", SensorNaming.getCategory(Sensor.TYPE_LIGHT))
        assertEquals("Environment", SensorNaming.getCategory(Sensor.TYPE_PRESSURE))
        assertEquals("Environment", SensorNaming.getCategory(Sensor.TYPE_TEMPERATURE))
        assertEquals("Environment", SensorNaming.getCategory(Sensor.TYPE_AMBIENT_TEMPERATURE))
        assertEquals("Environment", SensorNaming.getCategory(Sensor.TYPE_RELATIVE_HUMIDITY))

        assertEquals("Other", SensorNaming.getCategory(Sensor.TYPE_HEART_RATE))
        assertEquals("Other", SensorNaming.getCategory(999))
    }

    @Suppress("DEPRECATION")
    @Test
    fun testColumns() {
        val xyz = listOf("x", "y", "z")
        assertEquals(xyz, SensorNaming.getColumnNames(Sensor.TYPE_ACCELEROMETER))
        assertEquals(xyz, SensorNaming.getColumnNames(Sensor.TYPE_GRAVITY))
        assertEquals(xyz, SensorNaming.getColumnNames(Sensor.TYPE_LINEAR_ACCELERATION))
        assertEquals(xyz, SensorNaming.getColumnNames(Sensor.TYPE_GYROSCOPE))
        assertEquals(xyz, SensorNaming.getColumnNames(Sensor.TYPE_MAGNETIC_FIELD))

        val uncal = listOf("x", "y", "z", "bias_x", "bias_y", "bias_z")
        assertEquals(uncal, SensorNaming.getColumnNames(Sensor.TYPE_ACCELEROMETER_UNCALIBRATED))
        assertEquals(uncal, SensorNaming.getColumnNames(Sensor.TYPE_GYROSCOPE_UNCALIBRATED))
        assertEquals(uncal, SensorNaming.getColumnNames(Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED))

        val rot = listOf("x", "y", "z", "w", "heading_accuracy")
        assertEquals(rot, SensorNaming.getColumnNames(Sensor.TYPE_ROTATION_VECTOR))
        assertEquals(rot, SensorNaming.getColumnNames(Sensor.TYPE_GAME_ROTATION_VECTOR))
        assertEquals(rot, SensorNaming.getColumnNames(Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR))

        val v = listOf("value")
        assertEquals(v, SensorNaming.getColumnNames(Sensor.TYPE_LIGHT))
        assertEquals(v, SensorNaming.getColumnNames(Sensor.TYPE_PRESSURE))
        assertEquals(v, SensorNaming.getColumnNames(Sensor.TYPE_PROXIMITY))
        assertEquals(v, SensorNaming.getColumnNames(Sensor.TYPE_TEMPERATURE))
        assertEquals(v, SensorNaming.getColumnNames(Sensor.TYPE_AMBIENT_TEMPERATURE))
        assertEquals(v, SensorNaming.getColumnNames(Sensor.TYPE_RELATIVE_HUMIDITY))

        assertEquals(listOf("steps"), SensorNaming.getColumnNames(Sensor.TYPE_STEP_COUNTER))
        assertEquals(listOf("bpm"), SensorNaming.getColumnNames(Sensor.TYPE_HEART_RATE))

        assertEquals((0..15).map { "v$it" }, SensorNaming.getColumnNames(999))
    }
}