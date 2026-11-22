package com.pos.android.prices.ui

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
import com.pos.android.prices.data.model.PriceDto
import kotlinx.coroutines.flow.collectAsState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PriceScreen(
    onNavigateBack: () -> Unit,
    viewModel: PriceViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(uiState.message) {
        uiState.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }
    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(
                SnackbarHostState.SnackbarData(text = Text(it), actionLabel = "Cerrar")
            )
            viewModel.clearError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Precios por Sucursal", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = viewModel::openCreateForm) {
                        Icon(Icons.Default.Add, contentDescription = "Nuevo precio")
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
        Column(
            modifier = Modifier.fillMaxSize().padding(padding)
        ) {
            // Mensaje de error
            uiState.error?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }

            // Tabla de precios
            Column(
                modifier = Modifier.weight(1f)
            ) {
                if (uiState.isLoading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else if (uiState.prices.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.AttachMoney,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Sin precios locales",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(uiState.prices, key = { it.id }) { price ->
                            PriceRow(
                                price = price,
                                onEdit = viewModel::openEditForm,
                                onDelete = viewModel::deletePrice
                            )
                        }
                    }
                }
            }
        }

        // Formulario flotante para crear/editar precio
        if (uiState.showForm) {
            PriceForm(
                price = uiState.editingPrice,
                productId = uiState.formProductId,
                precio = uiState.formPrecio,
                vigenciaDesde = uiState.formVigenciaDesde,
                vigenciaHasta = uiState.formVigenciaHasta,
                onClose = { viewModel.closeForm() },
                onSave = viewModel::savePrice,
                onSaveEdit = { viewModel.updatePrice(1L, uiState.editingPrice?.id ?: 0) }
            )
        }
    }
}

@Composable
private fun PriceRow(
    price: PriceDto,
    onEdit: (PriceDto) -> Unit,
    onDelete: (PriceDto) -> Unit
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
                    text = price.productName ?: "Producto",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = price.productSku ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }

            Column(horizontalArrangement = Arrangement.End) {
                Text(
                    text = "${price.precio.toString().format("%.2f")}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = if (price.activo) "Activo" else "Inactivo",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = if (price.activo) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                )
            }

            Row(horizontalArrangement = Arrangement.End) {
                IconButton(
                    onClick = { onEdit(price) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Editar",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(
                    onClick = { onDelete(price) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Eliminar",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
private fun PriceForm(
    price: PriceDto?,
    productId: Long,
    precio: Double,
    vigenciaDesde: String,
    vigenciaHasta: String,
    onClose: () -> Unit,
    onSave: () -> Unit,
    onSaveEdit: () -> Unit
) {
    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .clip(RoundedCornerShape(8.dp))
        ) {
            Text(
                text = if (price == null) "Nuevo Precio Local" else "Editar Precio",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))

            TextField(
                value = productId.toString(),
                onValueChange = { viewModel.updateFormProductId(it.toIntOrNull() ?: 0) },
                label = { Text("Producto") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(8.dp))

            TextField(
                value = precio.toString(),
                onValueChange = { viewModel.updateFormPrecio(it.toDoubleOrNull() ?: 0.0) },
                label = { Text("Precio Local") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(8.dp))

            TextField(
                value = vigenciaDesde,
                onValueChange = { viewModel.updateFormVigenciaDesde(it) },
                label = { Text("Vigencia Desde") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(8.dp))

            TextField(
                value = vigenciaHasta,
                onValueChange = { viewModel.updateFormVigenciaHasta(it) },
                label = { Text("Vigencia Hasta") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Button(
                    onClick = onClose,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancelar")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = if (price == null) onSave else onSaveEdit,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Guardar")
                }
            }
        }
    }
}
