package com.pos.android.shifts.domain

import com.pos.android.core.network.ApiResult
import com.pos.android.shifts.data.ShiftRepository
import com.pos.android.shifts.data.model.EmployeeSchedule
import javax.inject.Inject

class GetScheduleUseCase @Inject constructor(
    private val shiftRepository: ShiftRepository
) {
    suspend operator fun invoke(): ApiResult<List<EmployeeSchedule>> {
        return shiftRepository.getSchedule()
    }
}
