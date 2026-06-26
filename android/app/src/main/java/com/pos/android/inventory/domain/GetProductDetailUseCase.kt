package com.pos.android.inventory.domain

import com.pos.android.core.network.ApiResult
import com.pos.android.inventory.data.ProductRepository
import com.pos.android.inventory.data.model.PriceResolutionResponse
import com.pos.android.inventory.data.model.ProductDto
import javax.inject.Inject

data class ProductDetail(
    val product: ProductDto,
    val price: PriceResolutionResponse?
)

class GetProductDetailUseCase @Inject constructor(
    private val productRepository: ProductRepository
) {
    suspend operator fun invoke(productId: Long): ApiResult<ProductDetail> {
        val productResult = productRepository.getProductById(productId)
        val priceResult = productRepository.getProductPrice(productId)

        return when (productResult) {
            is ApiResult.Success -> {
                val price = priceResult.getOrNull()
                ApiResult.success(ProductDetail(productResult.data, price))
            }
            is ApiResult.Error -> productResult
        }
    }
}
