package com.pos.android.shifts.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.pos.android.shifts.data.model.EmployeeSchedule
import com.pos.android.shifts.data.model.ShiftInfo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShiftScreen(
    viewModel: ShiftViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val days = listOf("Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Turnos", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refrescar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionsIconTint = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Encabezado de días
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp)
            ) {
                // Espacio para nombre
                Spacer(modifier = Modifier.weight(1.5f))
                days.forEach { day ->
                    Text(
                        text = day,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            HorizontalDivider()

            when {
                uiState.isLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                uiState.error != null -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                uiState.error!!,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = { viewModel.refresh() }) {
                                Text("Reintentar")
                            }
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(uiState.schedule, key = { it.employeeId }) { employee ->
                            EmployeeShiftRow(
                                employee = employee,
                                days = days
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmployeeShiftRow(
    employee: EmployeeSchedule,
    days: List<String>
) {
    // Mapa de día → shift
    val shiftMap = employee.shifts?.associateBy { it.diaSemana } ?: emptyMap()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Nombre empleado
            Text(
                text = employee.employeeName.split(" ").firstOrNull() ?: employee.employeeName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1.5f)
            )

            // Turnos por día
            days.forEachIndexed { index, _ ->
                val dayOfWeek = when (index) {
                    0 -> 1 // Lunes
                    1 -> 2
                    2 -> 3
                    3 -> 4
                    4 -> 5
                    5 -> 6
                    6 -> 7
                    else -> 0
                }
                val shift = shiftMap[dayOfWeek]
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    if (shift != null) {
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            tonalElevation = 1.dp
                        ) {
                            Text(
                                text = shift.shiftName?.take(3) ?: "T",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
