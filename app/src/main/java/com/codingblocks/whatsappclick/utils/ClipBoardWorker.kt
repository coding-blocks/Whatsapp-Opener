package com.codingblocks.whatsappclick.utils

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

class ClipBoardWorker(context: Context, params: WorkerParameters) : Worker(context, params) {

    companion object {
        private const val NAME_WORK_ONE_TIME_CLIPB_WORKER =
            "com.codingblocks.whatsappopener.NAME_WORK_ONE_TIME_CLIPB_WORKER"
        private const val NAME_WORK_PERIODIC_CLIPB_WORKER =
            "com.codingblocks.whatsappopener.NAME_WORK_PERIODIC_CLIPB_WORKER"

        fun scheduleOneTimeWork() {
            val oneTimeWork = OneTimeWorkRequest.Builder(ClipBoardWorker::class.java)
                .setInitialDelay(30, TimeUnit.SECONDS)
                .build()
            WorkManager.getInstance().enqueueUniqueWork(
                NAME_WORK_ONE_TIME_CLIPB_WORKER,
                ExistingWorkPolicy.REPLACE,
                oneTimeWork
            )
        }

        fun schedulePeriodicWork() {
            val periodicWork =
                PeriodicWorkRequest.Builder(ClipBoardWorker::class.java, 15, TimeUnit.MINUTES)
                    .build()
            WorkManager.getInstance().enqueueUniquePeriodicWork(
                NAME_WORK_PERIODIC_CLIPB_WORKER,
                ExistingPeriodicWorkPolicy.KEEP,
                periodicWork
            )
        }
    }

    override fun doWork(): Result {

        scheduleOneTimeWork()
        schedulePeriodicWork()

        return Result.success()
    }
}
