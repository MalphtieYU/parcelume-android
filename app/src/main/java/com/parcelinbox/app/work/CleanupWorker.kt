package com.parcelinbox.app.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.parcelinbox.app.ParcelInboxApplication
import java.util.concurrent.TimeUnit

class CleanupWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val application = applicationContext as ParcelInboxApplication
        val days = application.settings.retentionPolicy.days ?: return Result.success()
        val cutoff = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(days.toLong())
        application.database.deleteCompletedBefore(cutoff)
        application.repository.refresh()
        return Result.success()
    }
}

object CleanupScheduler {
    private const val WORK_NAME = "completed-parcel-cleanup"

    fun schedule(context: Context) {
        val request = PeriodicWorkRequestBuilder<CleanupWorker>(1, TimeUnit.DAYS).build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }
}
