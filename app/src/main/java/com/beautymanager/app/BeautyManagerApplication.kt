package com.beautymanager.app

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.beautymanager.app.core.work.DailyMaintenanceWorker
import dagger.hilt.android.HiltAndroidApp
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * Ponto de entrada do app. Habilita injeção de dependência (Hilt) em todo o grafo e
 * configura a WorkManager para workers com Hilt — usado pelo
 * DailyMaintenanceWorker, que roda uma vez por dia para recalcular os lembretes
 * de recompra.
 */
@HiltAndroidApp
class BeautyManagerApplication : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        scheduleDailyMaintenance()
    }

    private fun scheduleDailyMaintenance() {
        val request = PeriodicWorkRequestBuilder<DailyMaintenanceWorker>(1, TimeUnit.DAYS).build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            DailyMaintenanceWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }
}
