package com.github.alfin_efendy.sentinel.service.engine

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log

/**
 * Launches the target app via deep link (priority 1) or launcher intent (priority 2).
 */
class AppLauncher(private val context: Context) {

    companion object {
        private const val TAG = "AppLauncher"
    }

    /**
     * @return true if a launch intent was successfully fired; false if the app is not found.
     */
    fun launch(packageName: String, deepLinkUrl: String?): Boolean {
        // Priority 1: deep link
        if (!deepLinkUrl.isNullOrBlank()) {
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(deepLinkUrl)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
                }
                context.startActivity(intent)
                Log.d(TAG, "Launched via deep link: $deepLinkUrl")
                return true
            } catch (e: ActivityNotFoundException) {
                Log.w(TAG, "Deep link not handled, falling back to launcher: $deepLinkUrl")
            } catch (e: Exception) {
                Log.w(TAG, "Deep link launch failed: ${e.message}, falling back to launcher")
            }
        }

        // Priority 2: launcher intent
        return launchByPackage(packageName)
    }

    private fun launchByPackage(packageName: String): Boolean {
        val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)
        return if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
            try {
                context.startActivity(launchIntent)
                Log.d(TAG, "Launched via launcher intent: $packageName")
                true
            } catch (e: Exception) {
                Log.e(TAG, "Launcher intent failed for $packageName: ${e.message}")
                false
            }
        } else {
            Log.e(TAG, "No launcher intent found for $packageName (uninstalled?)")
            false
        }
    }
}
