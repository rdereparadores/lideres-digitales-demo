package com.example.pregon.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pregon.model.HomePresets
import com.example.pregon.ui.MainViewModel
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val currentHome by viewModel.currentHome.collectAsState()
    val demoState by viewModel.demoState.collectAsState()
    val dataSourceMode by viewModel.dataSourceMode.collectAsState()
    val realBackendUrl by viewModel.realBackendUrl.collectAsState()
    val connectionStatus by viewModel.connectionStatus.collectAsState()

    var showGpsNotice by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Ajustes y Demo",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // ==========================================
            // Section 1: Mi Casa
            // ==========================================
            item {
                Text(
                    text = "MI CASA",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("home_settings_card"),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = currentHome.name,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = currentHome.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.Home,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "Seleccionar barrio de Cáceres:",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            HomePresets.PRESETS.forEach { preset ->
                                val isSelected = preset.name == currentHome.name
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(
                                            if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                        )
                                        .clickable { viewModel.setHomePreset(preset) }
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(
                                            text = preset.name,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = preset.originStops.joinToString(" · ") { "${it.stopName} (${it.walkMin} min)" },
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedButton(
                            onClick = { showGpsNotice = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(imageVector = Icons.Default.MyLocation, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Usar mi ubicación GPS actual")
                        }

                        if (showGpsNotice) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "📍 Ubicación detectada en Cáceres. Asignado a Nuevo Cáceres.",
                                style = MaterialTheme.typography.labelSmall,
                                color = StatusOk
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Tu casa no sale del móvil (cálculo de paradas en el dispositivo)",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // ==========================================
            // Section 2: Panel de Demo (Clock & Scenarios)
            // ==========================================
            item {
                Text(
                    text = "PANEL DE DEMO (JURADO / EVALUACIÓN)",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("demo_panel_card"),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // Clock Banner
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                                .padding(14.dp)
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "RELOJ SIMULADO",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Box(
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .background(
                                                if (demoState.mode == "frozen") StatusWarning.copy(alpha = 0.2f)
                                                else StatusOk.copy(alpha = 0.2f)
                                            )
                                            .padding(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = if (demoState.mode == "frozen") "CONGELADO" else "EN MARCHA",
                                            color = if (demoState.mode == "frozen") StatusWarning else StatusOk,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    text = demoState.now.displayDateTime(),
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Time Controls Row (±15 min, ±1 día)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            OutlinedButton(
                                onClick = { viewModel.adjustClockMinutes(-15) },
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                            ) {
                                Text("-15 min", fontSize = 12.sp)
                            }
                            OutlinedButton(
                                onClick = { viewModel.adjustClockMinutes(15) },
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                            ) {
                                Text("+15 min", fontSize = 12.sp)
                            }
                            OutlinedButton(
                                onClick = { viewModel.adjustClockDays(-1) },
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                            ) {
                                Text("-1 día", fontSize = 12.sp)
                            }
                            OutlinedButton(
                                onClick = { viewModel.adjustClockDays(1) },
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                            ) {
                                Text("+1 día", fontSize = 12.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Clock Presets & Mode Toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = demoState.now.date == "2026-10-28" && demoState.now.minutes == 465,
                                onClick = { viewModel.setClockPreset("default") },
                                label = { Text("mié 28/10 07:45") }
                            )
                            FilterChip(
                                selected = demoState.mode == "running",
                                onClick = { viewModel.toggleClockMode() },
                                label = { Text(if (demoState.mode == "frozen") "Poner en marcha" else "Congelar") }
                            )
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 14.dp))

                        // Scenarios Toggles
                        Text(
                            text = "Escenarios de simulación (incidencias):",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        // Missing bus scenario
                        val isMissingBusL1 = demoState.scenarios.contains("missing_bus_L1")
                        ScenarioRow(
                            title = "Falta un autobús (L1 08:15 suprimido)",
                            subtitle = "Suprime la expedición de las 8:15 en L1. Siguiente bus al 95%.",
                            isActive = isMissingBusL1,
                            onToggle = { viewModel.toggleScenario("missing_bus_L1") }
                        )

                        // Rain scenario
                        val isRain = demoState.scenarios.contains("rain")
                        ScenarioRow(
                            title = "Lluvia intensa",
                            subtitle = "Velocidad reducida (+5 min) y 2 min extra andando.",
                            isActive = isRain,
                            onToggle = { viewModel.toggleScenario("rain") }
                        )

                        // Accident Ronda Norte scenario
                        val isAccident = demoState.scenarios.contains("accident_ronda_norte")
                        ScenarioRow(
                            title = "Accidente en Ronda Norte",
                            subtitle = "+8 min de retención en paradas de acceso al campus (S15+).",
                            isActive = isAccident,
                            onToggle = { viewModel.toggleScenario("accident_ronda_norte") }
                        )

                        // Exam day scenario
                        val isExamDay = demoState.scenarios.contains("exam_day")
                        ScenarioRow(
                            title = "Día de exámenes en Campus UEx",
                            subtitle = "Mayor afluencia a centros y saturación de aparcamiento.",
                            isActive = isExamDay,
                            onToggle = { viewModel.toggleScenario("exam_day") }
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Primary Demo Actions: "Simular mañana" & "Reiniciar demo"
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = { viewModel.simulateTomorrow() },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("simulate_tomorrow_button"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(imageVector = Icons.Default.FastForward, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Simular mañana", fontSize = 13.sp)
                            }

                            OutlinedButton(
                                onClick = { viewModel.resetDemo() },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("reset_demo_button"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(imageVector = Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Reiniciar demo", fontSize = 13.sp)
                            }
                        }
                    }
                }
            }

            // ==========================================
            // Section 3: Fuente de Datos
            // ==========================================
            item {
                Text(
                    text = "FUENTE DE DATOS",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.setDataSourceMode("simulated") }
                        ) {
                            RadioButton(
                                selected = dataSourceMode == "simulated",
                                onClick = { viewModel.setDataSourceMode("simulated") }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(text = "Simulada (sin backend)", fontWeight = FontWeight.SemiBold)
                                Text(
                                    text = "Motor determinista local en el dispositivo (prueba offline)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.setDataSourceMode("real") }
                        ) {
                            RadioButton(
                                selected = dataSourceMode == "real",
                                onClick = { viewModel.setDataSourceMode("real") }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(text = "Backend real (REST)", fontWeight = FontWeight.SemiBold)
                                Text(
                                    text = "Conecta con servidor API externo cuando esté disponible",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        AnimatedVisibility(visible = dataSourceMode == "real") {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 12.dp)
                            ) {
                                OutlinedTextField(
                                    value = realBackendUrl,
                                    onValueChange = { viewModel.setRealBackendUrl(it) },
                                    label = { Text("URL del backend") },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = { viewModel.testBackendConnection() },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("Guardar y probar conexión")
                                }
                                if (connectionStatus != null) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = connectionStatus!!,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ScenarioRow(
    title: String,
    subtitle: String,
    isActive: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Text(text = subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(
            checked = isActive,
            onCheckedChange = { onToggle() },
            modifier = Modifier.testTag("scenario_switch")
        )
    }
}
