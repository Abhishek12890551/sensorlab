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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sensorlab.core.model.SensorInfo

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SensorsScreen(modifier: Modifier = Modifier, viewModel: SensorsViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()

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
                            SensorCard(sensor)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SensorCard(sensor: SensorInfo) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(sensor.name, style = MaterialTheme.typography.titleMedium)
            Text(sensor.typeName, style = MaterialTheme.typography.bodySmall)
            Text(sensor.vendor, style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(8.dp))
            
            val maxHz = if (sensor.minDelayUs < 0) "one-shot"
                        else if (sensor.minDelayUs == 0) "on-change"
                        else "${1_000_000 / sensor.minDelayUs} Hz"

            Text(
                "Range: ${sensor.maxRange} | Res: ${sensor.resolution} | ${sensor.powerMa} mA | $maxHz",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}