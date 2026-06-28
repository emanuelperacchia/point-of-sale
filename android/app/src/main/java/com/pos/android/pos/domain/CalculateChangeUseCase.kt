package com.pos.android.pos.domain

import javax.inject.Inject

data class ChangeResult(
    val total: Double,
    val received: Double,
    val change: Double,
    val isExact: Boolean,
    val isInsufficient: Boolean
)

class CalculateChangeUseCase @Inject constructor() {

    operator fun invoke(total: Double, received: Double): ChangeResult {
        val change = received - total
        return ChangeResult(
            total = total,
            received = received,
            change = maxOf(0.0, change),
            isExact = change == 0.0,
            isInsufficient = change < 0
        )
    }
}
