package com.pos.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.pos.android.auth.data.model.AuthResponse
import com.pos.android.auth.ui.BranchSelectorScreen
import com.pos.android.auth.ui.LoginScreen
import com.pos.android.core.security.TokenStorage
import com.pos.android.core.ui.theme.POSTheme
import com.pos.android.inventory.ui.ProductDetailScreen
import com.pos.android.inventory.ui.ProductSearchScreen
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

    NavHost(
        navController = navController,
        startDestination = Routes.LOGIN
    ) {
        // ── Auth ──
        composable(Routes.LOGIN) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Routes.HOME) {
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
                        navController.navigate(Routes.HOME) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }
        }

        // ── Home ──
        composable(Routes.HOME) {
            ProductSearchScreen(
                onProductClick = { productId ->
                    navController.navigate(Routes.inventoryDetail(productId))
                }
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
    }
}
