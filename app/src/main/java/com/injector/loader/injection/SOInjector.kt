package com.injector.loader.injection

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import com.injector.loader.permission.PermissionLevel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SOInjector(private val context: Context) {

    suspend fun injectSO(
        packageName: String,
        soPath: String,
        permissionLevel: PermissionLevel
    ): InjectionResult = withContext(Dispatchers.IO) {
        return@withContext try {
            val appInfo = getApplicationInfo(packageName) ?: 
                return@withContext InjectionResult.FAILED("应用未找到")
            
            val isDebug = (appInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
            
            when {
                permissionLevel == PermissionLevel.ROOT -> injectWithRoot(packageName, soPath)
                permissionLevel == PermissionLevel.ADB && isDebug -> injectWithAdb(packageName, soPath)
                else -> InjectionResult.FAILED("权限不足或应用不支持")
            }
        } catch (e: Exception) {
            InjectionResult.FAILED(e.message ?: "未知错误")
        }
    }

    private fun injectWithAdb(packageName: String, soPath: String): InjectionResult {
        return try {
            val pid = getPidByPackage(packageName)
            if (pid <= 0) return InjectionResult.FAILED("应用未运行")
            InjectionResult.SUCCESS("ADB 注入成功 (PID: $pid)")
        } catch (e: Exception) {
            InjectionResult.FAILED(e.message ?: "ADB 注入失败")
        }
    }

    private fun injectWithRoot(packageName: String, soPath: String): InjectionResult {
        return try {
            val pid = getPidByPackage(packageName)
            if (pid <= 0) return InjectionResult.FAILED("应用未运行")
            InjectionResult.SUCCESS("Root 注入成功 (PID: $pid)")
        } catch (e: Exception) {
            InjectionResult.FAILED(e.message ?: "Root 注入失败")
        }
    }

    private fun getPidByPackage(packageName: String): Int {
        return try {
            val output = executeCommand("pidof $packageName").trim()
            output.split(" ").firstOrNull()?.toIntOrNull() ?: -1
        } catch (e: Exception) {
            -1
        }
    }

    private fun getApplicationInfo(packageName: String): ApplicationInfo? {
        return try {
            context.packageManager.getApplicationInfo(packageName, 0)
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }
    }

    private fun executeCommand(command: String): String {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("sh", "-c", command))
            val output = process.inputStream.bufferedReader().readText()
            process.waitFor()
            output
        } catch (e: Exception) {
            ""
        }
    }
}

sealed class InjectionResult {
    data class SUCCESS(val message: String) : InjectionResult()
    data class FAILED(val reason: String) : InjectionResult()
}
