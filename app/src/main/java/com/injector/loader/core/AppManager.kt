package com.injector.loader.core

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class AppItem(
    val packageName: String,
    val appName: String,
    val icon: Drawable?,
    val isDebug: Boolean,
    val isSystem: Boolean
)

class AppManager(private val context: Context) {

    suspend fun getInstalledApps(showSystem: Boolean = false): List<AppItem> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val apps = mutableListOf<AppItem>()

        try {
            val packages = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            
            for (appInfo in packages) {
                val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                val isDebug = (appInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0

                if (!showSystem && isSystem) continue

                val appName = pm.getApplicationLabel(appInfo).toString()
                val icon = pm.getApplicationIcon(appInfo)

                apps.add(AppItem(appInfo.packageName, appName, icon, isDebug, isSystem))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        apps.sortedBy { it.appName }
    }
}
