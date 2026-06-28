package com.pos.android.pos.domain

import com.pos.android.pos.data.PosRepository
import javax.inject.Inject

class AddToCartUseCase @Inject constructor(
    private val posRepository: PosRepository
) {
    /**
     * Agrega un producto al carrito local sin llamar a la API.
     * Si el producto ya existe, incrementa la cantidad.
     *
     * @return Triple con (productId, nombre del producto, stock disponible) para mostrar feedback
     */
    suspend operator fun invoke(
        productId: Long,
        nombre: String,
        sku: String,
        precio: Double,
        stock: Int,
        cantidad: Int = 1
    ) {
        posRepository.addToCart(productId, nombre, sku, precio, stock, cantidad)
    }
}
