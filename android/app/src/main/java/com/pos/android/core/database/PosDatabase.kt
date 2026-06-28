package com.pos.android.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.pos.android.inventory.data.local.ProductDao
import com.pos.android.inventory.data.local.ProductEntity
import com.pos.android.pos.data.local.CartItemDao
import com.pos.android.pos.data.local.CartItemEntity
import com.pos.android.pos.data.local.PendingSaleDao
import com.pos.android.pos.data.local.PendingSaleEntity

@Database(
    entities = [
        ProductEntity::class,
        CartItemEntity::class,
        PendingSaleEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class PosDatabase : RoomDatabase() {

    abstract fun productDao(): ProductDao
    abstract fun cartItemDao(): CartItemDao
    abstract fun pendingSaleDao(): PendingSaleDao

    companion object {
        const val DATABASE_NAME = "pos_database"
    }
}
