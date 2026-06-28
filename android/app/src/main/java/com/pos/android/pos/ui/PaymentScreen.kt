package com.pos.android.pos.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentScreen(
    onNavigateBack: () -> Unit,
    onSaleComplete: (saleId: Long?, wasOffline: Boolean) -> Unit,
    viewModel: PaymentViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // Manejar resultado
    LaunchedEffect(uiState.saleResult) {
        uiState.saleResult?.let { result ->
            if (result.isSuccess) {
                onSaleComplete(result.saleResponse?.id, false)
            } else if (result.isPendingSync) {
                onSaleComplete(null, true)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Cobrar") },
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
                .padding(16.dp)
        ) {
            // Total a cobrar
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Total a cobrar",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        "$${String.format("%.2f", uiState.total)}",
                        style = MaterialTheme.typography.displayLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Método de pago
            Text(
                "Método de pago",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PaymentMethodButton(
                    text = "Efectivo",
                    icon = Icons.Default.Money,
                    selected = uiState.selectedPayment == "EFECTIVO",
                    onClick = { viewModel.onPaymentMethodChanged("EFECTIVO") },
                    modifier = Modifier.weight(1f)
                )
                PaymentMethodButton(
                    text = "Tarjeta",
                    icon = Icons.Default.CreditCard,
                    selected = uiState.selectedPayment == "TARJETA",
                    onClick = { viewModel.onPaymentMethodChanged("TARJETA") },
                    modifier = Modifier.weight(1f)
                )
                PaymentMethodButton(
                    text = "Mixto",
                    icon = Icons.Default.AccountBalanceWallet,
                    selected = uiState.selectedPayment == "MIXTO",
                    onClick = { viewModel.onPaymentMethodChanged("MIXTO") },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Campo "Recibido" (solo para efectivo)
            if (uiState.selectedPayment == "EFECTIVO") {
                OutlinedTextField(
                    value = uiState.receivedText,
                    onValueChange = viewModel::onReceivedAmountChanged,
                    label = { Text("Recibido") },
                    prefix = { Text("$") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Done
                    ),
                    singleLine = true,
                    isError = uiState.isInsufficient,
                    modifier = Modifier.fillMaxWidth()
                )

                if (uiState.isInsufficient) {
                    Text(
                        "El monto recibido es menor al total",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Vuelto
                if (!uiState.isInsufficient && uiState.received > 0) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    "Vuelto",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                if (uiState.isExact) {
                                    Text(
                                        "Pago exacto",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                            }
                            Text(
                                "$${String.format("%.2f", uiState.change)}",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
            }

            // Error
            if (uiState.error != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    uiState.error!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Botón confirmar
            Button(
                onClick = { viewModel.processPayment() },
                enabled = !uiState.isProcessing,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                if (uiState.isProcessing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Icon(Icons.Default.ShoppingCartCheckout, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Confirmar pago — $${String.format("%.2f", uiState.total)}",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }
    }
}

@Composable
private fun PaymentMethodButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val containerColor = if (selected) MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.surfaceVariant
    val contentColor = if (selected) MaterialTheme.colorScheme.onPrimary
    else MaterialTheme.colorScheme.onSurfaceVariant

    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(text, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal) },
        icon = {
            Icon(
                icon,
                contentDescription = null,
                tint = contentColor
            )
        },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = containerColor,
            selectedLabelColor = contentColor
        ),
        modifier = modifier
    )
}
