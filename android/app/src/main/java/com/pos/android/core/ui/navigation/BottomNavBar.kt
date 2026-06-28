package com.pos.android.core.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.vector.ImageVector

data class BottomNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val badgeCount: Int = 0
)

@Composable
fun PosBottomNavBar(
    items: List<BottomNavItem>,
    currentRoute: String?,
    onItemClick: (String) -> Unit
) {
    NavigationBar {
        items.forEach { item ->
            val selected = currentRoute == item.route

            NavigationBarItem(
                selected = selected,
                onClick = { onItemClick(item.route) },
                icon = {
                    if (item.badgeCount > 0) {
                        BadgedBox(badge = {
                            Badge { Text("${item.badgeCount}") }
                        }) {
                            Icon(item.icon, contentDescription = item.label)
                        }
                    } else {
                        Icon(item.icon, contentDescription = item.label)
                    }
                },
                label = { Text(item.label) }
            )
        }
    }
}
