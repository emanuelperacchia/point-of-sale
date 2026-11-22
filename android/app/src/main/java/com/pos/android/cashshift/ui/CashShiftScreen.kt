package com.pos.android.cashshift.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CashShiftScreen(
    onNavigateBack: () -> Unit,
    onNavigateToSalesHistory: () -> Unit = {},
    viewModel: CashShiftViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showOpenDialog by remember { mutableStateOf(false) }
    var showCloseDialog by remember { mutableStateOf(false) }
    var showMovementDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Caja") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Botón "Historial de Ventas" siempre visible
            TextButton(
                onClick = onNavigateToSalesHistory,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Receipt,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Historial de Ventas")
            }

            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                !uiState.isOpen -> {
                    // Sin turno abierto
                    NoActiveShiftContent(
                        onOpenShift = { showOpenDialog = true },
                        modifier = Modifier.fillMaxSize().padding(16.dp)
                    )
                }
                else -> {
                    // Turno activo — reporte
                    ActiveShiftContent(
                        uiState = uiState,
                        onRefresh = viewModel::loadActiveShift,
                        onCloseShift = { showCloseDialog = true },
                        onAddMovement = { showMovementDialog = true },
                        modifier = Modifier.fillMaxSize().padding(16.dp)
                    )
                }
            }
        }
    }

    // Diálogos
    if (showOpenDialog) {
        OpenShiftDialog(
            onConfirm = { monto ->
                viewModel.openShift(monto)
                showOpenDialog = false
            },
            onDismiss = { showOpenDialog = false }
        )
    }

    if (showCloseDialog) {
        CloseShiftDialog(
            onConfirm = { monto ->
                viewModel.closeShift(monto)
                showCloseDialog = false
            },
            onDismiss = { showCloseDialog = false }
        )
    }

    if (showMovementDialog) {
        MovementDialog(
            onConfirm = { tipo, monto, motivo ->
                viewModel.createMovement(tipo, monto, motivo)
                showMovementDialog = false
            },
            onDismiss = { showMovementDialog = false }
        )
    }

    // Snackbar para mensajes
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }
    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }
}

@Composable
private fun NoActiveShiftContent(
    onOpenShift: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.PointOfSale,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "No hay turno de caja abierto",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Abra un turno para comenzar a operar",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onOpenShift) {
            Icon(Icons.Default.PlayArrow, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Abrir turno")
        }
    }
}

@Composable
private fun ActiveShiftContent(
    uiState: CashShiftUiState,
    onRefresh: () -> Unit,
    onCloseShift: () -> Unit,
    onAddMovement: () -> Unit,
    modifier: Modifier = Modifier
) {
    val report = uiState.report

    LazyColumn(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Header — turno activo
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Turno activo", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("ID #${uiState.activeShift?.id}", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        // Resumen
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Resumen", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    ReportRow("Apertura", report?.montoApertura)
                    ReportRow("Ventas efectivo", report?.totalVentasEfectivo)
                    ReportRow("Ingresos manuales", report?.totalIngresos)
                    ReportRow("Retiros manuales", report?.totalRetiros)
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    ReportRow("Monto esperado", report?.montoEsperado, bold = true)
                }
            }
        }

        // Ventas por método de pago
        if (!report?.ventasPorMetodoPago.isNullOrEmpty()) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Ventas por método", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        report?.ventasPorMetodoPago?.forEach { (metodo, monto) ->
                            if (monto > 0) {
                                ReportRow(
                                    when (metodo) {
                                        "CASH" -> "Efectivo"
                                        "DEBIT_CARD" -> "Tarjeta débito"
                                        "CREDIT_CARD" -> "Tarjeta crédito"
                                        "TRANSFER" -> "Transferencia"
                                        "CUENTA_CORRIENTE" -> "Cuenta corriente"
                                        "ONLINE" -> "Online"
                                        else -> metodo
                                    },
                                    monto
                                )
                            }
                        }
                    }
                }
            }
        }

        // Movimientos
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Movimientos", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                TextButton(onClick = onAddMovement) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Agregar")
                }
            }
        }

        if (uiState.movements.isEmpty()) {
            item {
                Text(
                    "Sin movimientos registrados",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        } else {
            items(uiState.movements) { mov ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            if (mov.tipo == "RETIRO") Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                            contentDescription = null,
                            tint = if (mov.tipo == "RETIRO") MaterialTheme.colorScheme.error
                                   else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                if (mov.tipo == "RETIRO") "Retiro" else "Ingreso",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(mov.motivo.orEmpty(), style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(
                            "$${String.format("%.2f", mov.monto ?: 0.0)}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (mov.tipo == "RETIRO") MaterialTheme.colorScheme.error
                                    else MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        // Botón cerrar turno
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onCloseShift,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Icon(Icons.Default.Lock, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Cerrar turno", style = MaterialTheme.typography.titleMedium)
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun ReportRow(label: String, value: Double?, bold: Boolean = false) {
    if (value == null) return
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("$${String.format("%.2f", value)}",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal)
    }
}

@Composable
private fun OpenShiftDialog(
    onConfirm: (Double) -> Unit,
    onDismiss: () -> Unit
) {
    var montoText by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Abrir turno") },
        text = {
            Column {
                Text("Ingrese el monto inicial de caja:")
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = montoText,
                    onValueChange = { montoText = it; error = null },
                    label = { Text("Monto de apertura") },
                    prefix = { Text("$") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    isError = error != null,
                    modifier = Modifier.fillMaxWidth()
                )
                error?.let {
                    Text(it, color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val monto = montoText.toDoubleOrNull()
                if (monto == null || monto <= 0) {
                    error = "Ingrese un monto válido"
                } else {
                    onConfirm(monto)
                }
            }) { Text("Abrir") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@Composable
private fun CloseShiftDialog(
    onConfirm: (Double) -> Unit,
    onDismiss: () -> Unit
) {
    var montoText by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Cerrar turno") },
        text = {
            Column {
                Text("Ingrese el monto final de caja:")
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = montoText,
                    onValueChange = { montoText = it; error = null },
                    label = { Text("Monto de cierre") },
                    prefix = { Text("$") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    isError = error != null,
                    modifier = Modifier.fillMaxWidth()
                )
                error?.let {
                    Text(it, color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val monto = montoText.toDoubleOrNull()
                if (monto == null || monto < 0) {
                    error = "Ingrese un monto válido"
                } else {
                    onConfirm(monto)
                }
            }) { Text("Cerrar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@Composable
private fun MovementDialog(
    onConfirm: (String, Double, String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedTipo by remember { mutableStateOf("RETIRO") }
    var montoText by remember { mutableStateOf("") }
    var motivoText by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Registrar movimiento") },
        text = {
            Column {
                // Tipo
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = selectedTipo == "RETIRO",
                        onClick = { selectedTipo = "RETIRO" },
                        label = { Text("Retiro") },
                        leadingIcon = { Icon(Icons.Default.ArrowUpward, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )
                    FilterChip(
                        selected = selectedTipo == "INGRESO",
                        onClick = { selectedTipo = "INGRESO" },
                        label = { Text("Ingreso") },
                        leadingIcon = { Icon(Icons.Default.ArrowDownward, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = montoText,
                    onValueChange = { montoText = it; error = null },
                    label = { Text("Monto") },
                    prefix = { Text("$") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    isError = error != null,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = motivoText,
                    onValueChange = { motivoText = it; error = null },
                    label = { Text("Motivo") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                error?.let {
                    Text(it, color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val monto = montoText.toDoubleOrNull()
                if (monto == null || monto <= 0) {
                    error = "Ingrese un monto válido"
                } else if (motivoText.isBlank()) {
                    error = "Ingrese un motivo"
                } else {
                    onConfirm(selectedTipo, monto, motivoText)
                }
            }) { Text("Registrar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}
