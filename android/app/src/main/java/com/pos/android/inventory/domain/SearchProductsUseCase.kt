package com.pos.android.inventory.domain

import com.pos.android.core.network.ApiResult
import com.pos.android.inventory.data.ProductRepository
import com.pos.android.inventory.data.model.ProductSearchResponse
import javax.inject.Inject

class SearchProductsUseCase @Inject constructor(
    private val productRepository: ProductRepository
) {
    suspend operator fun invoke(query: String): ApiResult<List<ProductSearchResponse>> {
        if (query.isBlank()) return ApiResult.success(emptyList())
        return productRepository.searchProducts(query)
    }
}
