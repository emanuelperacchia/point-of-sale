package com.pos.android

/**
 * Rutas de navegación de la app.
 * Definidas centralizadamente para evitar refactors.
 */
object Routes {
    // Auth
    const val LOGIN = "auth/login"
    const val BRANCH_SELECTOR = "auth/branch"

    // Home (Bottom Nav container)
    const val HOME = "home"

    // Dashboard
    const val DASHBOARD = "dashboard"

    // Notifications
    const val NOTIFICATIONS = "notifications"

    // POS
    const val POS = "pos"
    const val PAYMENT = "pos/payment/{total}"
    const val SCANNER = "pos/scanner"

    // Inventory
    const val INVENTORY_SEARCH = "inventory/search"
    const val INVENTORY_DETAIL = "inventory/detail/{productId}"

    // Attendance
    const val ATTENDANCE = "attendance"

    // Shifts
    const val SHIFTS = "shifts"

    // Cash Shift (Caja)
    const val CASH_SHIFT = "cash-shift"

    // Sales History
    const val SALES_HISTORY = "sales-history"
    const val SALE_DETAIL = "sales-history/{saleId}"

    // Categorías
    const val CATEGORIES = "categories"

    // Proveedores
    const val SUPPLIERS = "suppliers"

    // Precios
    const val PRICES = "prices"

    // Usuarios
    const val USERS = "users"

    // Helpers
    fun payment(total: Double) = "pos/payment/$total"
    fun inventoryDetail(productId: Long) = "inventory/detail/$productId"
    fun saleDetail(saleId: Long) = "sales-history/$saleId"

    // Helpers
    fun payment(total: Double) = "pos/payment/$total"
    fun inventoryDetail(productId: Long) = "inventory/detail/$productId"
    fun saleDetail(saleId: Long) = "sales-history/$saleId"
}
