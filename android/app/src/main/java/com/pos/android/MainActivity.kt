package com.pos.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.pos.android.attendance.ui.AttendanceScreen
import com.pos.android.auth.data.model.AuthResponse
import com.pos.android.auth.ui.BranchSelectorScreen
import com.pos.android.auth.ui.LoginScreen
import com.pos.android.core.security.TokenStorage
import com.pos.android.core.ui.navigation.BottomNavItem
import com.pos.android.core.ui.navigation.PosBottomNavBar
import com.pos.android.core.ui.theme.POSTheme
import com.pos.android.inventory.ui.ProductDetailScreen
import com.pos.android.inventory.ui.ProductSearchScreen
import com.pos.android.inventory.ui.scanner.BarcodeScannerScreen
import com.pos.android.pos.ui.PosScreen
import com.pos.android.pos.ui.PosViewModel
import com.pos.android.pos.ui.PaymentScreen
import com.pos.android.shifts.ui.ShiftScreen
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var tokenStorage: TokenStorage

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            POSTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    POSNavHost(tokenStorage = tokenStorage)
                }
            }
        }
    }
}

@Composable
fun POSNavHost(tokenStorage: TokenStorage) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Shared PosViewModel between POS screens
    val posViewModel: PosViewModel = hiltViewModel()
    val posUiState by posViewModel.uiState.collectAsState()

    // Escaner: leer código cuando volvemos
    val scannedCode = navController.currentBackStackEntry
        ?.savedStateHandle
        ?.get<String>("scannedCode")
    LaunchedEffect(scannedCode) {
        if (scannedCode != null) {
            posViewModel.onScannedCode(scannedCode)
            navController.currentBackStackEntry
                ?.savedStateHandle
                ?.remove<String>("scannedCode")
        }
    }

    val bottomNavItems = remember(posUiState.pendingSaleCount) {
        listOf(
            BottomNavItem(Routes.POS, "POS", Icons.Default.PointOfSale, badgeCount = posUiState.pendingSaleCount),
            BottomNavItem(Routes.INVENTORY_SEARCH, "Stock", Icons.Default.Inventory2),
            BottomNavItem(Routes.ATTENDANCE, "Asistencia", Icons.Default.Fingerprint),
            BottomNavItem(Routes.SHIFTS, "Turnos", Icons.Default.CalendarMonth)
        )
    }

    val showBottomBar = currentRoute in bottomNavItems.map { it.route }

    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (showBottomBar) {
                PosBottomNavBar(
                    items = bottomNavItems,
                    currentRoute = currentRoute,
                    onItemClick = { route ->
                        navController.navigate(route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.LOGIN,
            modifier = Modifier.padding(innerPadding)
        ) {
            // ── Auth ──
            composable(Routes.LOGIN) {
                LoginScreen(
                    onLoginSuccess = {
                        navController.navigate(Routes.POS) {
                            popUpTo(Routes.LOGIN) { inclusive = true }
                        }
                    },
                    onBranchSelectionRequired = { authResponse ->
                        navController.currentBackStackEntry
                            ?.savedStateHandle
                            ?.set("authResponse", authResponse)
                        navController.navigate(Routes.BRANCH_SELECTOR)
                    }
                )
            }

            composable(Routes.BRANCH_SELECTOR) {
                val authResponse = navController.previousBackStackEntry
                    ?.savedStateHandle
                    ?.get<AuthResponse>("authResponse")

                if (authResponse != null) {
                    BranchSelectorScreen(
                        authResponse = authResponse,
                        onBranchSelected = { branch ->
                            tokenStorage.activeBranchId = branch.id
                            tokenStorage.activeBranchName = branch.nombre
                            navController.navigate(Routes.POS) {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    )
                }
            }

            // ── POS ──
            composable(Routes.POS) {
                PosScreen(
                    onNavigateToPayment = { total ->
                        navController.navigate(Routes.payment(total))
                    },
                    onNavigateToScanner = {
                        navController.navigate(Routes.SCANNER)
                    }
                )
            }

            composable(
                route = Routes.PAYMENT,
                arguments = listOf(navArgument("total") { type = NavType.FloatType })
            ) {
                PaymentScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onSaleComplete = { saleId, wasOffline ->
                        navController.navigate(Routes.POS) {
                            popUpTo(Routes.POS) { inclusive = true }
                        }
                        if (wasOffline) {
                            snackbarHostState.showSnackbar(
                                "Venta guardada sin conexión. Se sincronizará automáticamente."
                            )
                        } else {
                            snackbarHostState.showSnackbar("Venta #$saleId completada")
                        }
                    }
                )
            }

            composable(Routes.SCANNER) {
                BarcodeScannerScreen(
                    onBarcodeScanned = { code ->
                        navController.previousBackStackEntry
                            ?.savedStateHandle
                            ?.set("scannedCode", code)
                        navController.popBackStack()
                    },
                    onDismiss = { navController.popBackStack() }
                )
            }

            // ── Inventory ──
            composable(Routes.INVENTORY_SEARCH) {
                ProductSearchScreen(
                    onProductClick = { productId ->
                        navController.navigate(Routes.inventoryDetail(productId))
                    }
                )
            }

            composable(
                route = Routes.INVENTORY_DETAIL,
                arguments = listOf(navArgument("productId") { type = NavType.LongType })
            ) {
                ProductDetailScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // ── Attendance ──
            composable(Routes.ATTENDANCE) {
                AttendanceScreen()
            }

            // ── Shifts ──
            composable(Routes.SHIFTS) {
                ShiftScreen()
            }
        }
    }
}
