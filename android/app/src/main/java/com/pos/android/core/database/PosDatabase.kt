package com.pos.android.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.pos.android.inventory.data.local.ProductDao
import com.pos.android.inventory.data.local.ProductEntity

@Database(
    entities = [ProductEntity::class],
    version = 1,
    exportSchema = false
)
abstract class PosDatabase : RoomDatabase() {

    abstract fun productDao(): ProductDao

    companion object {
        const val DATABASE_NAME = "pos_database"
    }
}
