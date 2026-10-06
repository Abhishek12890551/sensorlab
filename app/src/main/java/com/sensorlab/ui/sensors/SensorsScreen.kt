package com.sensorlab.ui.sensors

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.sensorlab.core.model.SensorFormatting
import com.sensorlab.core.model.SensorInfo
import com.sensorlab.data.sensors.SensorEventSource
import com.sensorlab.data.sensors.SensorNaming

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SensorsScreen(modifier: Modifier = Modifier, viewModel: SensorsViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedSensor by remember { mutableStateOf<SensorInfo?>(null) }

    Box(modifier = modifier.fillMaxSize()) {
        when (val state = uiState) {
            is SensorsUiState.Loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
            is SensorsUiState.Empty -> Text("No sensors found", Modifier.align(Alignment.Center))
            is SensorsUiState.Error -> {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = "Error: ${state.message}", color = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = { viewModel.loadSensors() }) {
                        Text("Retry")
                    }
                }
            }
            is SensorsUiState.Success -> {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    item {
                        Text(
                            text = "${state.deviceInfo.manufacturer} ${state.deviceInfo.model} | Android ${state.deviceInfo.androidVersion} | ${state.deviceInfo.sensors.size} Sensors",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                    }
                    
                    item {
                        OutlinedTextField(
                            value = state.searchQuery,
                            onValueChange = { viewModel.setFilter(state.currentCategory, it, state.showWakeUpOnly) },
                            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                            placeholder = { Text("Search") }
                        )
                    }
                    
                    item {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(bottom = 16.dp)
                        ) {
                            val categories = listOf("All", "Motion", "Position", "Environment", "Other")
                            categories.forEach { cat ->
                                FilterChip(
                                    selected = state.currentCategory == cat,
                                    onClick = { viewModel.setFilter(cat, state.searchQuery, state.showWakeUpOnly) },
                                    label = { Text(cat) }
                                )
                            }
                            FilterChip(
                                selected = state.showWakeUpOnly,
                                onClick = { viewModel.setFilter(state.currentCategory, state.searchQuery, !state.showWakeUpOnly) },
                                label = { Text("Wake-up") }
                            )
                        }
                    }

                    if (state.filteredSensors.isEmpty()) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().height(100.dp)) {
                                Text("No sensors match", Modifier.align(Alignment.Center))
                            }
                        }
                    } else {
                        items(state.filteredSensors, key = { it.key }) { sensor ->
                            SensorCard(sensor, onClick = { selectedSensor = sensor })
                        }
                    }
                }
            }
        }
    }

    selectedSensor?.let { sensor ->
        val eventSource = remember(sensor) { viewModel.createEventSource() }
        SensorDetailSheet(
            sensor = sensor,
            eventSource = eventSource,
            onDismiss = { selectedSensor = null }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SensorCard(sensor: SensorInfo, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(sensor.name, style = MaterialTheme.typography.titleMedium)
            Text(sensor.typeName, style = MaterialTheme.typography.bodySmall)
            Text(sensor.vendor, style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(8.dp))
            
            val maxHz = SensorFormatting.formatMaxHz(sensor.minDelayUs, sensor.reportingMode)

            Text(
                "Range: ${sensor.maxRange} | Res: ${sensor.resolution} | ${sensor.powerMa} mA | Max rate: $maxHz",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SensorDetailSheet(
    sensor: SensorInfo,
    eventSource: SensorEventSource,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val controller = remember(sensor) { SensorDetailController(sensor, eventSource, scope) }
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(sensor, lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                controller.stop()
            } else if (event == Lifecycle.Event.ON_START) {
                controller.startPreview()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            controller.stop()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(modifier = Modifier.padding(16.dp).fillMaxWidth().verticalScroll(rememberScrollState())) {
            Text(sensor.name, style = MaterialTheme.typography.titleLarge)
            Text(sensor.vendor, style = MaterialTheme.typography.titleMedium)
            Text("${sensor.typeName} (Type ${sensor.type})", style = MaterialTheme.typography.bodyMedium)
            
            Spacer(modifier = Modifier.height(16.dp))

            val modeText = when (sensor.reportingMode) {
                0 -> "continuous"
                1 -> "on change"
                2 -> "one-shot"
                3 -> "special trigger"
                else -> "unknown"
            }
            Text("Key: ${sensor.key}", style = MaterialTheme.typography.bodyMedium)
            Text("Version: ${sensor.version}", style = MaterialTheme.typography.bodyMedium)
            Text("Category: ${sensor.category}", style = MaterialTheme.typography.bodyMedium)
            Text("Reporting mode: $modeText", style = MaterialTheme.typography.bodyMedium)
            Text("Max rate: ${SensorFormatting.formatMaxHz(sensor.minDelayUs, sensor.reportingMode)}", style = MaterialTheme.typography.bodyMedium)
            Text("Min Delay: ${sensor.minDelayUs} us | Max Delay: ${sensor.maxDelayUs} us", style = MaterialTheme.typography.bodyMedium)
            Text("Max Range: ${sensor.maxRange}", style = MaterialTheme.typography.bodyMedium)
            Text("Resolution: ${sensor.resolution}", style = MaterialTheme.typography.bodyMedium)
            Text("Power: ${sensor.powerMa} mA", style = MaterialTheme.typography.bodyMedium)
            Text("FIFO Max: ${sensor.fifoMaxEventCount} | Reserved: ${sensor.fifoReservedEventCount}", style = MaterialTheme.typography.bodyMedium)
            Text("Wake-up: ${sensor.isWakeUp} | Dynamic: ${sensor.isDynamic}", style = MaterialTheme.typography.bodyMedium)
            
            Spacer(modifier = Modifier.height(16.dp))
            val columns = SensorNaming.getColumnNames(sensor.type)
            Text("Columns: ${columns.joinToString()}", style = MaterialTheme.typography.bodySmall)
            
            Spacer(modifier = Modifier.height(16.dp))
            Text("Live Preview", style = MaterialTheme.typography.titleMedium)
            
            val previewState by controller.previewState.collectAsState()
            
            when (val pState = previewState) {
                is PreviewState.NotSupported -> Text("preview not supported for this sensor type")
                is PreviewState.Waiting -> Text("waiting for events...")
                is PreviewState.Paused -> Text(pState.message)
                is PreviewState.Error -> Text(pState.message, color = MaterialTheme.colorScheme.error)
                is PreviewState.Active -> {
                    Column {
                        pState.values.forEachIndexed { index, value ->
                            val colName = columns.getOrNull(index) ?: "Value $index"
                            Text("$colName: $value")
                        }
                    }
                }
            }

            if (sensor.reportingMode == 0 && sensor.minDelayUs > 0) {
                Spacer(modifier = Modifier.height(16.dp))
                val isTesting by controller.isTestingRate.collectAsState()
                val stats by controller.rateStats.collectAsState()

                Button(
                    onClick = { controller.startRateTest() },
                    enabled = !isTesting
                ) {
                    Text(if (isTesting) "Testing Rate (5s)..." else "Test Effective Rate")
                }

                stats?.let { s ->
                    Spacer(modifier = Modifier.height(8.dp))
                    if (s.message != null) {
                        Text(s.message, color = MaterialTheme.colorScheme.error)
                    } else {
                        val limitMsg = if (sensor.minDelayUs > 5000) " (sensor limit)" else ""
                        Text("Requested: ${s.requestedHz} Hz$limitMsg")
                        Text("Effective: ${String.format("%.1f", s.effectiveHz)} Hz")
                        Text("Interval (ms) min: ${String.format("%.1f", s.minIntervalMs)} / mean: ${String.format("%.1f", s.meanIntervalMs)} / max: ${String.format("%.1f", s.maxIntervalMs)}")
                        Text("Note: the OS may override the requested rate", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}