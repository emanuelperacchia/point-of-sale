package com.pos.android.pos.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cart_items")
data class CartItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "product_id") val productId: Long,
    @ColumnInfo(name = "nombre") val nombre: String,
    @ColumnInfo(name = "sku") val sku: String,
    @ColumnInfo(name = "precio") val precio: Double,
    @ColumnInfo(name = "stock_disponible") val stockDisponible: Int,
    @ColumnInfo(name = "cantidad") val cantidad: Int,
    @ColumnInfo(name = "descuento") val descuento: Double = 0.0,
    @ColumnInfo(name = "imagen_url") val imagenUrl: String? = null
) {
    val subtotal: Double get() = (precio * cantidad) - descuento
}
