package com.sensorlab.ui.sensors

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sensorlab.core.model.DeviceInfo
import com.sensorlab.core.model.SensorInfo
import com.sensorlab.data.sensors.SensorRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Provider
import com.sensorlab.data.sensors.SensorEventSource

sealed class SensorsUiState {
    object Loading : SensorsUiState()
    object Empty : SensorsUiState()
    data class Error(val message: String) : SensorsUiState()
    data class Success(
        val deviceInfo: DeviceInfo,
        val filteredSensors: List<SensorInfo>,
        val currentCategory: String,
        val searchQuery: String,
        val showWakeUpOnly: Boolean
    ) : SensorsUiState()
}

@HiltViewModel
class SensorsViewModel @Inject constructor(
    private val sensorRepository: SensorRepository,
    private val eventSourceProvider: Provider<SensorEventSource>
) : ViewModel() {

    fun createEventSource(): SensorEventSource = eventSourceProvider.get()

    private val _uiState = MutableStateFlow<SensorsUiState>(SensorsUiState.Loading)
    val uiState: StateFlow<SensorsUiState> = _uiState.asStateFlow()

    private var fullDeviceInfo: DeviceInfo? = null

    init {
        loadSensors()
    }

    fun loadSensors() {
        _uiState.value = SensorsUiState.Loading
        viewModelScope.launch {
            try {
                val device = sensorRepository.getDeviceInfo()
                fullDeviceInfo = device
                if (device.sensors.isEmpty()) {
                    _uiState.value = SensorsUiState.Empty
                } else {
                    _uiState.value = SensorsUiState.Success(
                        deviceInfo = device,
                        filteredSensors = device.sensors,
                        currentCategory = "All",
                        searchQuery = "",
                        showWakeUpOnly = false
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.value = SensorsUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun setFilter(category: String, search: String, wakeUpOnly: Boolean) {
        val device = fullDeviceInfo ?: return
        
        val filtered = device.sensors.filter { sensor ->
            val matchCategory = category == "All" || sensor.category == category
            val matchWakeup = !wakeUpOnly || sensor.isWakeUp
            val matchSearch = search.isBlank() || 
                sensor.name.contains(search, ignoreCase = true) ||
                sensor.vendor.contains(search, ignoreCase = true) ||
                sensor.typeName.contains(search, ignoreCase = true)
            matchCategory && matchWakeup && matchSearch
        }

        if (filtered.isEmpty() && device.sensors.isNotEmpty()) {
            _uiState.value = SensorsUiState.Success(device, emptyList(), category, search, wakeUpOnly)
        } else if (filtered.isEmpty()) {
            _uiState.value = SensorsUiState.Empty
        } else {
            _uiState.value = SensorsUiState.Success(device, filtered, category, search, wakeUpOnly)
        }
    }
}