package com.pos.android.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.pos.android.dashboard.data.local.DashboardCacheDao
import com.pos.android.dashboard.data.local.DashboardCacheEntity
import com.pos.android.inventory.data.local.ProductDao
import com.pos.android.inventory.data.local.ProductEntity
import com.pos.android.notification.data.NotificationDao
import com.pos.android.notification.data.NotificationEntity
import com.pos.android.pos.data.local.CartItemDao
import com.pos.android.pos.data.local.CartItemEntity
import com.pos.android.pos.data.local.PendingSaleDao
import com.pos.android.pos.data.local.PendingSaleEntity

@Database(
    entities = [
        ProductEntity::class,
        CartItemEntity::class,
        PendingSaleEntity::class,
        DashboardCacheEntity::class,
        NotificationEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class PosDatabase : RoomDatabase() {

    abstract fun productDao(): ProductDao
    abstract fun cartItemDao(): CartItemDao
    abstract fun pendingSaleDao(): PendingSaleDao
    abstract fun dashboardCacheDao(): DashboardCacheDao
    abstract fun notificationDao(): NotificationDao

    companion object {
        const val DATABASE_NAME = "pos_database"
    }
}
