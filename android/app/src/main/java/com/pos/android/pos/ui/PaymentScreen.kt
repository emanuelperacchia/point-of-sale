package com.pos.android.pos.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.pos.android.pos.data.model.ClientSearchResponse
import com.pos.android.pos.data.model.InvoiceResponse
import com.pos.android.pos.data.model.PromotionResponse

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentScreen(
    onNavigateBack: () -> Unit,
    onSaleComplete: (saleId: Long?, wasOffline: Boolean) -> Unit,
    viewModel: PaymentViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // Navegar solo si fue offline (no hay comprobante para mostrar)
    LaunchedEffect(uiState.saleResult) {
        uiState.saleResult?.let { result ->
            if (result.isPendingSync) {
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
        if (uiState.saleResult?.isSuccess == true) {
            // ── Pantalla de confirmación post-venta ──
            SaleConfirmationContent(
                invoice = uiState.invoice,
                isLoadingInvoice = uiState.isLoadingInvoice,
                wasOffline = false,
                onNewSale = {
                    viewModel.clearSaleResult()
                    onSaleComplete(uiState.saleResult?.saleResponse?.id, false)
                },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
            )
        } else {
            // ── Pantalla de pago ──
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

                Spacer(modifier = Modifier.height(16.dp))

                // ── Cliente ──
                ClientSearchSection(
                    searchQuery = uiState.searchQuery,
                    clientResults = uiState.clientResults,
                    isSearching = uiState.isSearchingClient,
                    selectedClient = uiState.selectedClient,
                    onQueryChanged = viewModel::onClientSearchQueryChanged,
                    onClientSelected = viewModel::onClientSelected,
                    onClearClient = viewModel::onClearClient
                )

                Spacer(modifier = Modifier.height(12.dp))

                // ── Cupón ──
                CouponSection(
                    couponCode = uiState.couponCode,
                    promotions = uiState.promotions,
                    onCouponChanged = viewModel::onCouponCodeChanged
                )

                Spacer(modifier = Modifier.height(16.dp))

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

                Spacer(modifier = Modifier.height(16.dp))

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

                    Spacer(modifier = Modifier.height(12.dp))

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
}

// ── Cliente ──

@Composable
private fun ClientSearchSection(
    searchQuery: String,
    clientResults: List<ClientSearchResponse>,
    isSearching: Boolean,
    selectedClient: ClientSearchResponse?,
    onQueryChanged: (String) -> Unit,
    onClientSelected: (ClientSearchResponse) -> Unit,
    onClearClient: () -> Unit
) {
    if (selectedClient != null) {
        // Cliente seleccionado — mostrar chip
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(selectedClient.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                    Text(
                        buildString {
                            selectedClient.documentType?.let { append("$it ") }
                            selectedClient.documentNumber?.let { append(it) }
                            selectedClient.condicionIva?.let { append(" · $it") }
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                    )
                }
                IconButton(onClick = onClearClient, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Quitar cliente", modifier = Modifier.size(18.dp))
                }
            }
        }
        return
    }

    // Búsqueda de cliente
    Text(
        "Cliente (opcional)",
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(modifier = Modifier.height(4.dp))

    OutlinedTextField(
        value = searchQuery,
        onValueChange = onQueryChanged,
        placeholder = { Text("Buscar por nombre...") },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        trailingIcon = {
            if (isSearching) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            }
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        modifier = Modifier.fillMaxWidth()
    )

    // Resultados de búsqueda
    AnimatedVisibility(visible = clientResults.isNotEmpty()) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 200.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            LazyColumn {
                items(clientResults) { client ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onClientSelected(client) }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(client.name, style = MaterialTheme.typography.bodyMedium)
                            Text(
                                buildString {
                                    client.documentType?.let { append("$it ") }
                                    client.documentNumber?.let { append(it) }
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── Cupón ──

@Composable
private fun CouponSection(
    couponCode: String,
    promotions: List<PromotionResponse>,
    onCouponChanged: (String) -> Unit
) {
    Text(
        "Cupón de descuento (opcional)",
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(modifier = Modifier.height(4.dp))

    OutlinedTextField(
        value = couponCode,
        onValueChange = onCouponChanged,
        placeholder = { Text("Ingresar código...") },
        leadingIcon = { Icon(Icons.Default.ConfirmationNumber, contentDescription = null) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Uri,
            imeAction = ImeAction.Done
        ),
        modifier = Modifier.fillMaxWidth()
    )

    // Promociones activas
    if (promotions.isNotEmpty()) {
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Promociones activas",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            promotions.take(3).forEach { promo ->
                SuggestionChip(
                    onClick = { /* Solo informativo */ },
                    label = {
                        Text(
                            buildString {
                                append(promo.nombre.orEmpty())
                                promo.valor?.let { append(" (${it}%)") }
                            },
                            style = MaterialTheme.typography.labelSmall
                        )
                    },
                    icon = {
                        Icon(
                            Icons.Default.LocalOffer,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                )
            }
        }
    }
}

// ── Confirmación post-venta ──

@Composable
private fun SaleConfirmationContent(
    invoice: InvoiceResponse?,
    isLoadingInvoice: Boolean,
    wasOffline: Boolean,
    onNewSale: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        // Header éxito
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
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Venta completada",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Comprobante
        if (isLoadingInvoice) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Cargando comprobante...", style = MaterialTheme.typography.bodyMedium)
                }
            }
        } else if (invoice != null) {
            InvoiceCard(invoice)
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Text(
                    "Comprobante no disponible en este momento",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Botón nueva venta
        Button(
            onClick = onNewSale,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Nueva venta", style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun InvoiceCard(invoice: InvoiceResponse) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Comprobante fiscal",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))

            InvoiceRow("Tipo", invoice.tipoComprobante.orEmpty())
            InvoiceRow("Punto de venta", invoice.puntoVenta?.toString().orEmpty())
            InvoiceRow("Número", invoice.numero?.toString().orEmpty())
            InvoiceRow("CAE", invoice.cae.orEmpty())

            invoice.receptorNombre?.let {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                InvoiceRow("Cliente", it)
                invoice.receptorDocumento?.let { doc ->
                    InvoiceRow("Documento", doc)
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            invoice.estado?.let { InvoiceRow("Estado", it) }
            invoice.createdAt?.let { InvoiceRow("Emisión", it) }
        }
    }
}

@Composable
private fun InvoiceRow(label: String, value: String) {
    if (value.isBlank()) return
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}

// ── Payment Method Button ──

@Composable
private fun PaymentMethodButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(text, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal) },
        leadingIcon = {
            Icon(
                icon,
                contentDescription = null,
                tint = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
            selectedLabelColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
        ),
        modifier = modifier
    )
}
