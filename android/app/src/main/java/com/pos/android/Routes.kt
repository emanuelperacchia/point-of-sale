package com.pos.android

/**
 * Rutas de navegación de la app.
 * Definidas centralizadamente para evitar refactors.
 *
 * Las rutas A2 y A3 se agregan en sus respectivos sprints.
 */
object Routes {
    const val LOGIN = "auth/login"
    const val BRANCH_SELECTOR = "auth/branch"
    const val HOME = "home"
    const val INVENTORY_SEARCH = "inventory/search"
    const val INVENTORY_DETAIL = "inventory/detail/{productId}"

    fun inventoryDetail(productId: Long) = "inventory/detail/$productId"
}
