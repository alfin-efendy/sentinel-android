package com.github.alfin_efendy.sentinel.data.repository

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import com.github.alfin_efendy.sentinel.domain.model.AppInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PackageInfoRepository(private val context: Context) {

    /** Returns all installed apps that have a launcher activity, sorted by label. */
    suspend fun getInstalledApps(): List<AppInfo> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val launcherIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        pm.queryIntentActivities(launcherIntent, PackageManager.GET_META_DATA)
            .map { resolveInfo ->
                val packageName = resolveInfo.activityInfo.packageName
                AppInfo(
                    packageName = packageName,
                    label = resolveInfo.loadLabel(pm).toString(),
                    icon = try { resolveInfo.loadIcon(pm) } catch (e: Exception) { null }
                )
            }
            .sortedBy { it.label.lowercase() }
    }
}
