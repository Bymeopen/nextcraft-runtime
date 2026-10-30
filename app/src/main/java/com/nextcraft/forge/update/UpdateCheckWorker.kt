package com.nextcraft.forge.update

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.Worker
import androidx.work.WorkerParameters
import androidx.work.WorkManager
import com.nextcraft.forge.data.AppPreferences
import java.util.concurrent.TimeUnit

/** Checks for a newer build in the background and announces it; errors (no network, no release) just wait for the next round. */
class UpdateCheckWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result {
        val preferences = AppPreferences(applicationContext)
        if (!preferences.updateNotifications && !preferences.autoInstallUpdates) return Result.success()
        val info = runCatching { AppUpdater(applicationContext).check() }.getOrNull() ?: return Result.success()
        if (preferences.autoInstallUpdates && canInstallNow() && installInBackground(info)) return Result.success()
        if (preferences.updateNotifications) UpdateNotifier.notifyIfNew(applicationContext, info)
        return Result.success()
    }

    /** Only while no agent or build is actively running. */
    private fun canInstallNow(): Boolean {
        if (com.nextcraft.forge.runtime.RuntimeTaskController.hasActiveTask()) return false
        // The silent install needs root. Without it the APK would be downloaded in full every 15 minutes for nothing.
        // detect() is cached after the first call, so this only blocks (off the main thread) once per process.
        return com.nextcraft.forge.runtime.RuntimeEngines.detect(applicationContext) != null
    }

    /** Downloads (verified by [AppUpdater]) and starts the silent install. False means nothing changed. */
    private fun installInBackground(info: AppUpdateInfo): Boolean {
        val state = applicationContext.getSharedPreferences("nextcraft_update_worker", Context.MODE_PRIVATE)
        // A target that already failed to download/verify/install is not fetched again (up to 512 MB every 15 minutes)
        // until a newer versionCode is published.
        if (state.getLong(KEY_FAILED_VERSION, -1L) == info.versionCode) return false
        val installed = runCatching {
            val apk = AppUpdater(applicationContext).download(info) { _, _ -> }
            com.nextcraft.forge.update.SilentSelfUpdate.install(applicationContext, apk)
        }.getOrDefault(false)
        if (!installed) state.edit().putLong(KEY_FAILED_VERSION, info.versionCode).apply()
        return installed
    }

    companion object {
        private const val WORK_NAME = "app-update-check"
        private const val KEY_FAILED_VERSION = "failed_version_code"

        /** Every 15 minutes while the network is up; the same request is kept when scheduled again. */
        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<UpdateCheckWorker>(15, TimeUnit.MINUTES)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}
