package com.pos.android.pos.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.pos.android.inventory.data.model.ProductSearchResponse
import com.pos.android.pos.data.local.CartItemEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PosScreen(
    onNavigateToPayment: (Double) -> Unit,
    onNavigateToScanner: () -> Unit,
    viewModel: PosViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showSearch by remember { mutableStateOf(true) }

    // Snackbar
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(uiState.message) {
        uiState.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "POS",
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    // Badge de ventas pendientes
                    if (uiState.pendingSaleCount > 0) {
                        BadgedBox(badge = {
                            Badge { Text("${uiState.pendingSaleCount}") }
                        }) {
                            Icon(Icons.Default.SyncProblem, contentDescription = "Pendientes")
                        }
                    }
                    IconButton(onClick = onNavigateToScanner) {
                        Icon(Icons.Default.QrCodeScanner, contentDescription = "Escanear")
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
            // ── Barra de búsqueda ──
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = viewModel::onSearchQueryChanged,
                placeholder = { Text("Buscar producto por nombre o SKU...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (uiState.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Limpiar")
                        }
                    }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = {
                    // Los resultados ya se cargan con el onChange
                }),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            )

            // Resultados de búsqueda (solo si hay query)
            if (uiState.searchQuery.isNotEmpty()) {
                SearchResultsList(
                    results = uiState.searchResults,
                    isLoading = uiState.isSearching,
                    onProductClick = viewModel::addToCart,
                    modifier = Modifier.weight(1f)
                )
            } else {
                // Carrito
                CartContent(
                    cartItems = uiState.cartItems,
                    cartTotal = uiState.cartTotal,
                    onQuantityChange = viewModel::updateQuantity,
                    onRemoveItem = viewModel::removeItem,
                    onCheckout = { onNavigateToPayment(uiState.cartTotal) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun SearchResultsList(
    results: List<ProductSearchResponse>,
    isLoading: Boolean,
    onProductClick: (ProductSearchResponse) -> Unit,
    modifier: Modifier = Modifier
) {
    if (isLoading) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else if (results.isEmpty()) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Sin resultados", style = MaterialTheme.typography.bodyLarge)
        }
    } else {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(results, key = { it.id }) { product ->
                ProductSearchCard(product = product, onClick = { onProductClick(product) })
            }
        }
    }
}

@Composable
private fun ProductSearchCard(
    product: ProductSearchResponse,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "SKU: ${product.sku}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "$${String.format("%.2f", product.price)}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Stock: ${product.stock}",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (product.stock > 0) MaterialTheme.colorScheme.onSurfaceVariant
                    else MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun CartContent(
    cartItems: List<CartItemEntity>,
    cartTotal: Double,
    onQuantityChange: (Long, Int) -> Unit,
    onRemoveItem: (CartItemEntity) -> Unit,
    onCheckout: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        if (cartItems.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.ShoppingCart,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Buscá productos para empezar",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            // Lista del carrito
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(cartItems, key = { it.id }) { item ->
                    CartItemCard(
                        item = item,
                        onQuantityChange = { qty -> onQuantityChange(item.id, qty) },
                        onRemove = { onRemoveItem(item) }
                    )
                }
            }

            // Resumen + botón cobrar
            Surface(
                tonalElevation = 3.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "Total",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            "$${String.format("%.2f", cartTotal)}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = onCheckout,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    ) {
                        Icon(Icons.Default.ShoppingCartCheckout, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Cobrar $${String.format("%.2f", cartTotal)}")
                    }
                }
            }
        }
    }
}

@Composable
private fun CartItemCard(
    item: CartItemEntity,
    onQuantityChange: (Int) -> Unit,
    onRemove: () -> Unit
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
                    text = item.nombre,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "$${String.format("%.2f", item.precio)} c/u",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (item.descuento > 0) {
                    Text(
                        text = "Desc: -$${String.format("%.2f", item.descuento)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            // Control de cantidad
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { if (item.cantidad > 1) onQuantityChange(item.cantidad - 1) else onRemove() },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Default.Remove, contentDescription = "Restar", modifier = Modifier.size(18.dp))
                }

                Text(
                    text = "${item.cantidad}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                IconButton(
                    onClick = { onQuantityChange(item.cantidad + 1) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Sumar", modifier = Modifier.size(18.dp))
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = "$${String.format("%.2f", item.subtotal)}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            IconButton(onClick = onRemove, modifier = Modifier.size(32.dp)) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Eliminar",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
