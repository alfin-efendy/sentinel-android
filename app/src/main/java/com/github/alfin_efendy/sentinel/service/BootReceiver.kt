package com.github.alfin_efendy.sentinel.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.github.alfin_efendy.sentinel.core.extensions.startForegroundServiceCompat
import com.github.alfin_efendy.sentinel.data.datastore.AppConfigDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Restores monitoring after device reboot if it was previously enabled.
 */
class BootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BootReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val validActions = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED
        )
        if (intent.action !in validActions) return

        Log.d(TAG, "Received ${intent.action} — checking saved config")

        val dataStore = AppConfigDataStore(context.applicationContext)
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val config = dataStore.readOnce()
                if (config.isEnabled && config.isValid) {
                    Log.i(TAG, "Restoring monitoring for: ${config.packageName}")
                    val serviceIntent = Intent(context, SentinelForegroundService::class.java).apply {
                        action = SentinelForegroundService.ACTION_START_MONITORING
                    }
                    context.startForegroundServiceCompat(serviceIntent)
                } else {
                    Log.d(TAG, "Monitoring was not enabled — skipping restore")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error reading config on boot: ${e.message}")
            } finally {
                pendingResult.finish()
            }
        }
    }
}
