package com.pos.android.core.sync

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.pos.android.core.network.connectivity.ConnectivityObserver
import com.pos.android.core.network.connectivity.ConnectivityStatus
import com.pos.android.pos.data.local.PendingSaleDao
import com.pos.android.pos.data.local.PendingSaleEntity
import com.pos.android.pos.data.PosRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val pendingSaleDao: PendingSaleDao,
    private val posRepository: PosRepository,
    private val connectivityObserver: ConnectivityObserver
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val TAG = "SyncWorker"
        const val MAX_RETRIES = 3
        const val WORK_NAME = "pos_sync_worker"
    }

    override suspend fun doWork(): Result {
        if (!connectivityObserver.isCurrentlyConnected()) {
            Log.w(TAG, "Sin conexión — sync cancelado")
            return Result.retry()
        }

        val pending = pendingSaleDao.getPending()
        if (pending.isEmpty()) {
            Log.d(TAG, "Sin ventas pendientes")
            return Result.success()
        }

        var allSuccess = true

        for (sale in pending) {
            when (val success = syncSale(sale)) {
                true -> pendingSaleDao.markCompleted(sale.id)
                false -> {
                    if (sale.intentos >= MAX_RETRIES - 1) {
                        pendingSaleDao.markError(sale.id, "Máximo de reintentos alcanzado")
                        Log.e(TAG, "Venta ${sale.id} en ERROR permanente tras $MAX_RETRIES intentos")
                    } else {
                        pendingSaleDao.markSyncing(sale.id, "PENDIENTE")
                        Log.w(TAG, "Venta ${sale.id} reintento ${sale.intentos + 1}/$MAX_RETRIES")
                    }
                    allSuccess = false
                }
            }
        }

        return if (allSuccess) Result.success() else Result.retry()
    }

    private suspend fun syncSale(sale: PendingSaleEntity): Boolean {
        return try {
            pendingSaleDao.markSyncing(sale.id)
            val result = posRepository.processOfflineSale(sale.bodyJson)
            result.isSuccess
        } catch (e: Exception) {
            Log.e(TAG, "Error sync venta ${sale.id}: ${e.message}")
            false
        }
    }
}
