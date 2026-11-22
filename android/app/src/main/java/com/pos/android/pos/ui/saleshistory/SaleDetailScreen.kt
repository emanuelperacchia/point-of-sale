package com.pos.android.pos.ui.saleshistory

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pos.android.pos.data.model.PaymentResponse
import com.pos.android.pos.data.model.SaleItemResponse
import com.pos.android.pos.data.model.SaleResponse

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SaleDetailScreen(
    saleId: Long,
    onBack: () -> Unit,
    viewModel: SalesHistoryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(saleId) {
        viewModel.loadSaleDetail(saleId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detalle de Venta") },
                navigationIcon = {
                    IconButton(onClick = {
                        viewModel.clearDetail()
                        onBack()
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            val saleDetail = uiState.selectedSale
            when {
                uiState.isLoadingDetail -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                saleDetail != null -> {
                    SaleDetailContent(sale = saleDetail)
                }
                else -> {
                    Text(
                        text = "No se pudo cargar el detalle",
                        modifier = Modifier.align(Alignment.Center),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun SaleDetailContent(sale: SaleResponse) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // ── Encabezado ──
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Venta #${sale.id}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    StatusBadge(sale.status)
                }

                Spacer(modifier = Modifier.height(8.dp))

                sale.createdAt?.let { date ->
                    Text(
                        text = "Fecha: ${formatDate(date)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                // Subtotales
                SummaryRow("Subtotal", sale.subtotal ?: 0.0)
                SummaryRow("IVA", sale.taxAmount ?: 0.0)

                if ((sale.discount ?: 0.0) > 0) {
                    SummaryRow(
                        "Descuento",
                        sale.discount ?: 0.0,
                        color = Color(0xFF2E7D32)
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        SummaryRow(
            "Total",
            sale.total ?: 0.0,
            bold = true,
            amountStyle = MaterialTheme.typography.titleLarge
        )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ── Productos ──
        if (!sale.items.isNullOrEmpty()) {
            Text(
                text = "Productos",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    sale.items.forEachIndexed { index, item ->
                        SaleItemRow(item)
                        if (index < sale.items.lastIndex) {
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 8.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // ── Pagos ──
        if (!sale.payments.isNullOrEmpty()) {
            Text(
                text = "Pagos",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    sale.payments.forEachIndexed { index, payment ->
                        PaymentRow(payment)
                        if (index < sale.payments.lastIndex) {
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 8.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryRow(
    label: String,
    amount: Double,
    color: Color = MaterialTheme.colorScheme.onSurface,
    bold: Boolean = false,
    amountStyle: TextStyle = MaterialTheme.typography.bodyLarge
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal
        )
        Text(
            text = formatCurrency(amount),
            style = amountStyle,
            color = color,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Medium
        )
    }
}

@Composable
private fun SaleItemRow(item: SaleItemResponse) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.ShoppingCart,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(8.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.productName ?: "Producto #${item.productId}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "${item.quantity} x ${formatCurrency(item.unitPrice ?: 0.0)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Text(
            text = formatCurrency(item.subtotal ?: 0.0),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun PaymentRow(payment: PaymentResponse) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Payment,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = formatPaymentMethod(payment.paymentMethod),
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )

        Text(
            text = formatCurrency(payment.amount),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

private fun formatPaymentMethod(method: String): String {
    return when (method.uppercase()) {
        "CASH" -> "Efectivo"
        "DEBIT_CARD" -> "Tarjeta de Débito"
        "CREDIT_CARD" -> "Tarjeta de Crédito"
        "TRANSFER" -> "Transferencia"
        "QR" -> "QR"
        "MERCADOPAGO" -> "Mercado Pago"
        else -> method
    }
}
