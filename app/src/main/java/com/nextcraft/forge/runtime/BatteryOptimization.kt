package com.nextcraft.forge.runtime

import androidx.core.net.toUri
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings

/**
 * Long builds and agent sessions run for minutes to hours behind a foreground service, yet many ROMs still stop
 * apps that are "optimized" for battery. Being exempt from that is the one thing the app can ask for itself.
 */
object BatteryOptimization {
    fun isUnrestricted(context: Context): Boolean =
        context.getSystemService(PowerManager::class.java)?.isIgnoringBatteryOptimizations(context.packageName) == true

    /** The system dialog that asks the user to exempt this app (not resolvable on API 30+ without a manifest <queries>). */
    fun requestIntent(context: Context): Intent =
        Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, "package:${context.packageName}".toUri())

    /**
     * Tries the dialog first: resolveActivity says null on API 30+ without a <queries> entry even when it exists, so the
     * start itself decides. Some ROMs lack it (or refuse it), then the general battery-optimization list is opened.
     * Returns false when no settings screen exists at all (rare); the caller can fall back to a hint.
     */
    fun launch(context: Context, starter: (Intent) -> Unit): Boolean = try {
        starter(requestIntent(context))
        true
    } catch (_: ActivityNotFoundException) {
        launchSettingsList(starter)
    } catch (_: SecurityException) {
        launchSettingsList(starter)
    }

    private fun launchSettingsList(starter: (Intent) -> Unit): Boolean = try {
        starter(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
        true
    } catch (_: ActivityNotFoundException) {
        false
    } catch (_: SecurityException) {
        false
    }
}
