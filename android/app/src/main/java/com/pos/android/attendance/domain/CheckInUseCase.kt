package com.pos.android.attendance.domain

import com.pos.android.attendance.data.AttendanceRepository
import com.pos.android.attendance.data.model.AttendanceResponse
import com.pos.android.core.network.ApiResult
import javax.inject.Inject

class CheckInUseCase @Inject constructor(
    private val attendanceRepository: AttendanceRepository
) {
    suspend fun checkIn(): ApiResult<AttendanceResponse> {
        return attendanceRepository.checkIn()
    }

    suspend fun checkOut(): ApiResult<AttendanceResponse> {
        return attendanceRepository.checkOut()
    }
}
