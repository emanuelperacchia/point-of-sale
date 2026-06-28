package com.pos.android.pos.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CartItemDao {

    @Query("SELECT * FROM cart_items ORDER BY id ASC")
    fun observeAll(): Flow<List<CartItemEntity>>

    @Query("SELECT * FROM cart_items ORDER BY id ASC")
    suspend fun getAll(): List<CartItemEntity>

    @Query("SELECT * FROM cart_items WHERE product_id = :productId LIMIT 1")
    suspend fun getByProductId(productId: Long): CartItemEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: CartItemEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<CartItemEntity>)

    @Query("UPDATE cart_items SET cantidad = :cantidad WHERE id = :id")
    suspend fun updateQuantity(id: Long, cantidad: Int)

    @Delete
    suspend fun delete(item: CartItemEntity)

    @Query("DELETE FROM cart_items")
    suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM cart_items")
    fun count(): Flow<Int>
}
