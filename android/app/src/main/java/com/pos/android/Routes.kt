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

    // Helpers
    fun payment(total: Double) = "pos/payment/$total"
    fun inventoryDetail(productId: Long) = "inventory/detail/$productId"
}
