package com.sensorlab.ui.sensors

import com.sensorlab.core.model.RawSensor
import com.sensorlab.data.sensors.FakeSensorSource
import com.sensorlab.data.sensors.SensorRepository
import com.sensorlab.data.sensors.SensorSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class SensorsViewModelTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testLoadingAndEmptyStates() = runTest {
        val repo = SensorRepository(FakeSensorSource(emptyList()), tempFolder.root, "1.0", testDispatcher)
        val vm = SensorsViewModel(repo)
        
        assertTrue(vm.uiState.value is SensorsUiState.Loading)
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(vm.uiState.value is SensorsUiState.Empty)
    }

    @Test
    fun testErrorState() = runTest {
        val fakeSource = object : SensorSource {
            override fun getSensors(): List<RawSensor> {
                throw IllegalStateException("Simulated hardware error")
            }
        }
        val repo = SensorRepository(fakeSource, tempFolder.root, "1.0", testDispatcher)
        val vm = SensorsViewModel(repo)
        
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(vm.uiState.value is SensorsUiState.Error)
    }

    @Test
    fun testFilteringAndSearch() = runTest {
        val s1 = RawSensor(1, "android.sensor.accelerometer", "Accel", "A", 1, 1f, 1f, 1f, 10, 10, 0, 0, 0, false, false)
        val s2 = RawSensor(2, "android.sensor.magnetic_field", "Mag", "A", 1, 1f, 1f, 1f, 10, 10, 0, 0, 0, false, false)
        val s3 = RawSensor(5, "android.sensor.light", "Light", "B", 1, 1f, 1f, 1f, 10, 10, 0, 0, 0, true, false)
        val s4 = RawSensor(999, "com.vendor.custom", "Custom", "C", 1, 1f, 1f, 1f, 10, 10, 0, 0, 0, false, false)
        
        val repo = SensorRepository(FakeSensorSource(listOf(s1, s2, s3, s4)), tempFolder.root, "1.0", testDispatcher)
        val vm = SensorsViewModel(repo)
        testDispatcher.scheduler.advanceUntilIdle()
        
        val success = vm.uiState.value as SensorsUiState.Success
        assertEquals(4, success.filteredSensors.size)

        vm.setFilter("Motion", "", false)
        assertEquals(1, (vm.uiState.value as SensorsUiState.Success).filteredSensors.size)
        assertEquals("accelerometer", (vm.uiState.value as SensorsUiState.Success).filteredSensors[0].key)

        vm.setFilter("Position", "", false)
        assertEquals(1, (vm.uiState.value as SensorsUiState.Success).filteredSensors.size)
        assertEquals("magnetic_field", (vm.uiState.value as SensorsUiState.Success).filteredSensors[0].key)

        vm.setFilter("Environment", "", false)
        assertEquals(1, (vm.uiState.value as SensorsUiState.Success).filteredSensors.size)
        assertEquals("light", (vm.uiState.value as SensorsUiState.Success).filteredSensors[0].key)

        vm.setFilter("Other", "", false)
        assertEquals(1, (vm.uiState.value as SensorsUiState.Success).filteredSensors.size)
        assertEquals("custom", (vm.uiState.value as SensorsUiState.Success).filteredSensors[0].key)

        vm.setFilter("All", "nomatch123", false)
        assertTrue((vm.uiState.value as SensorsUiState.Success).filteredSensors.isEmpty())

        vm.setFilter("All", "accel", false)
        assertEquals(1, (vm.uiState.value as SensorsUiState.Success).filteredSensors.size)
    }

    @Test
    fun testWakeUpAndSearchCombinations() = runTest {
        val s1 = RawSensor(1, "android.sensor.accelerometer", "Accel", "Google", 1, 1f, 1f, 1f, 10, 10, 0, 0, 0, false, false)
        val s2 = RawSensor(1, "android.sensor.accelerometer", "Accel Wake", "Google", 1, 1f, 1f, 1f, 10, 10, 0, 0, 0, true, false)
        val s3 = RawSensor(5, "android.sensor.light", "Light", "Samsung", 1, 1f, 1f, 1f, 10, 10, 0, 0, 0, true, false)
        
        val repo = SensorRepository(FakeSensorSource(listOf(s1, s2, s3)), tempFolder.root, "1.0", testDispatcher)
        val vm = SensorsViewModel(repo)
        testDispatcher.scheduler.advanceUntilIdle()
        
        vm.setFilter("All", "", true)
        assertEquals(2, (vm.uiState.value as SensorsUiState.Success).filteredSensors.size)

        vm.setFilter("Motion", "", true)
        val filteredMotion = (vm.uiState.value as SensorsUiState.Success).filteredSensors
        assertEquals(1, filteredMotion.size)
        assertEquals("Accel Wake", filteredMotion[0].name)

        vm.setFilter("All", "Light", true)
        val filteredLight = (vm.uiState.value as SensorsUiState.Success).filteredSensors
        assertEquals(1, filteredLight.size)
        assertEquals("Light", filteredLight[0].name)

        vm.setFilter("All", "Samsung", false)
        val filteredVendor = (vm.uiState.value as SensorsUiState.Success).filteredSensors
        assertEquals(1, filteredVendor.size)
        assertEquals("Light", filteredVendor[0].name)
    }
}