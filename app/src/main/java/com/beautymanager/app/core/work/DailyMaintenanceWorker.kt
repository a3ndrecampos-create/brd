package com.beautymanager.app.core.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.beautymanager.app.domain.usecase.GenerateRemindersUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Roda uma vez por dia (agendado em BeautyManagerApplication) para recalcular os
 * lembretes de recompra. Checagem de estoque baixo não precisa de worker: como é
 * uma query simples (quantity <= minStock), o Dashboard já mostra isso em tempo
 * real via Flow toda vez que a tela é aberta.
 */
@HiltWorker
class DailyMaintenanceWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val generateRemindersUseCase: GenerateRemindersUseCase
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            generateRemindersUseCase()
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        const val WORK_NAME = "daily_maintenance_reminders"
    }
}
