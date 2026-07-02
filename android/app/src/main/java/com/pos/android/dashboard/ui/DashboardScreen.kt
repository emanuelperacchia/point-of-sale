package com.pos.android.dashboard.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottom
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStart
import com.patrykandpatrick.vico.core.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.core.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries

import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onAlertClick: (type: String) -> Unit = {},
    onNavigateToInventory: () -> Unit = {},
    onNavigateToNotifications: () -> Unit = {},
    unreadNotificationCount: Int = 0,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.state.collectAsState()
    val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale("es", "AR")) }

    fun formatCurrency(value: Double?): String {
        return value?.let { currencyFormat.format(it) } ?: "-"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Dashboard", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = onNavigateToNotifications) {
                        BadgedBox(
                            badge = {
                                if (unreadNotificationCount > 0) {
                                    Badge { Text("$unreadNotificationCount") }
                                }
                            }
                        ) {
                            Icon(Icons.Default.Notifications, contentDescription = "Notificaciones")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = uiState.isRefreshing,
            onRefresh = { viewModel.refresh() },
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                uiState.isLoading && uiState.data == null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                uiState.error != null && uiState.data == null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.CloudOff,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.height(16.dp))
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
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // ── Indicador de caché ──
                        if (uiState.isFromCache && uiState.cachedAt != null) {
                            item {
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            Icons.Default.CloudOff,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                            tint = MaterialTheme.colorScheme.onTertiaryContainer
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            "Datos offline — ${uiState.cachedAt}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onTertiaryContainer
                                        )
                                    }
                                }
                            }
                        }

                        // ── Selector de período ──
                        item {
                            PeriodChips(
                                periods = uiState.periods,
                                selectedPeriod = uiState.selectedPeriod,
                                onPeriodSelected = viewModel::selectPeriod
                            )
                        }

                        // ── KPI Cards ──
                        item {
                            Text(
                                "Ventas",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        uiState.data?.sales?.let { sales ->
                            item {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    KpiCard(
                                        title = "Ventas",
                                        value = formatCurrency(sales.totalSales),
                                        variation = sales.salesVariation,
                                        icon = Icons.Default.TrendingUp,
                                        modifier = Modifier.weight(1f)
                                    )
                                    KpiCard(
                                        title = "Ticket prom.",
                                        value = formatCurrency(sales.averageTicket),
                                        variation = null,
                                        icon = Icons.Default.Receipt,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    KpiCard(
                                        title = "Transacciones",
                                        value = "${sales.transactionCount ?: 0}",
                                        variation = sales.transactionVariation,
                                        icon = Icons.Default.ShoppingCart,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }

                        // ── Finanzas ──
                        uiState.data?.financial?.let { fin ->
                            item {
                                Text(
                                    "Finanzas",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            item {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    KpiCard(
                                        title = "Saldo proyectado",
                                        value = formatCurrency(fin.projectedBalance),
                                        variation = null,
                                        icon = Icons.Default.AccountBalance,
                                        modifier = Modifier.weight(1f)
                                    )
                                    KpiCard(
                                        title = "Ctas. x cobrar venc.",
                                        value = formatCurrency(fin.overdueReceivables),
                                        variation = null,
                                        icon = Icons.Default.Warning,
                                        valueColor = if ((fin.overdueReceivables ?: 0.0) > 0)
                                            MaterialTheme.colorScheme.error
                                        else MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }

                        // ── Inventario ──
                        uiState.data?.inventory?.let { inv ->
                            item {
                                Text(
                                    "Inventario",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            item {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    KpiCard(
                                        title = "Stock crítico",
                                        value = "${inv.criticalStockCount ?: 0}",
                                        variation = null,
                                        icon = Icons.Default.Inventory2,
                                        valueColor = if ((inv.criticalStockCount ?: 0) > 0)
                                            MaterialTheme.colorScheme.error
                                        else MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.weight(1f)
                                    )
                                    KpiCard(
                                        title = "Valor stock",
                                        value = formatCurrency(inv.totalStockValue),
                                        variation = null,
                                        icon = Icons.Default.MonetizationOn,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }

                        // ── Gráfico de ventas (Vico) ──
                        uiState.data?.dailySales?.let { dailySales ->
                            if (dailySales.isNotEmpty()) {
                                item {
                                    Text(
                                        "Ventas últimos 7 días",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                item {
                                    SalesChartCard(dailySales = dailySales)
                                }
                            }
                        }

                        // ── Top 5 Productos ──
                        uiState.data?.topProducts?.let { products ->
                            if (products.isNotEmpty()) {
                                item {
                                    Text(
                                        "Top 5 Productos",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                items(products.take(5)) { product ->
                                    ProductRankingCard(product = product, currencyFormat = currencyFormat)
                                }
                            }
                        }

                        // ── Alertas ──
                        uiState.data?.alerts?.let { alerts ->
                            if (alerts.isNotEmpty()) {
                                item {
                                    Text(
                                        "Alertas activas",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                                items(alerts) { alert ->
                                    AlertCard(alert = alert, onAlertClick = onAlertClick)
                                }
                            }
                        }

                        // Espacio final
                        item { Spacer(modifier = Modifier.height(16.dp)) }
                    }
                }
            }
        }
    }
}

// ── Period Chips ──

@Composable
private fun PeriodChips(
    periods: List<DashboardUiState.PeriodOption>,
    selectedPeriod: String,
    onPeriodSelected: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        periods.forEach { period ->
            FilterChip(
                selected = selectedPeriod == period.value,
                onClick = { onPeriodSelected(period.value) },
                label = { Text(period.label, fontWeight = if (selectedPeriod == period.value) FontWeight.Bold else FontWeight.Normal) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

// ── KPI Card ──

@Composable
private fun KpiCard(
    title: String,
    value: String,
    variation: Double?,
    icon: ImageVector,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    icon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    title,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = valueColor
            )
            if (variation != null) {
                val isPositive = variation >= 0
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (isPositive) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = if (isPositive) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.error
                    )
                    Text(
                        "${String.format("%.1f", variation)}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isPositive) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

// ── Sales Chart (Vico) ──

@Composable
private fun SalesChartCard(
    dailySales: List<com.pos.android.dashboard.data.model.DailySalesPointDto>
) {
    val modelProducer = remember { CartesianChartModelProducer() }

    LaunchedEffect(dailySales) {
        modelProducer.runTransaction {
            lineSeries {
                series(dailySales.map { it.amount?.toFloat() ?: 0f })
            }
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Ventas diarias",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))

            CartesianChartHost(
                chart = rememberCartesianChart(
                    rememberLineCartesianLayer(),
                    startAxis = VerticalAxis.rememberStart(),
                    bottomAxis = HorizontalAxis.rememberBottom()
                ),
                modelProducer = modelProducer,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            )

            // Labels de días abajo
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                dailySales.forEach { point ->
                    Text(
                        text = point.date?.takeLast(5) ?: "",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// ── Product Ranking Card ──

@Composable
private fun ProductRankingCard(
    product: com.pos.android.dashboard.data.model.ProductRankingDto,
    currencyFormat: java.text.NumberFormat
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = product.productName ?: "N/A",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "SKU: ${product.productSku ?: "-"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${product.quantity?.toInt() ?: 0} vendidos",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                product.variation?.let { variazion ->
                    val isPositive = variazion >= 0
                    Text(
                        text = "${if (isPositive) "+" else ""}${String.format("%.1f", variazion)}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isPositive) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

// ── Alert Card ──

@Composable
private fun AlertCard(
    alert: com.pos.android.dashboard.data.model.DashboardAlertDto,
    onAlertClick: (String) -> Unit
) {
    val severityColor = when (alert.severity) {
        "HIGH" -> MaterialTheme.colorScheme.error
        "MEDIUM" -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.secondary
    }

    Card(
        onClick = { alert.actionLink?.let { onAlertClick(it) } },
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = severityColor.copy(alpha = 0.1f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Warning,
                contentDescription = null,
                tint = severityColor,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    alert.message ?: "",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                if (alert.count != null && alert.count > 0) {
                    Text(
                        "${alert.count} elemento(s)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Icon(
                Icons.Default.KeyboardArrowRight,
                contentDescription = "Ir",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
