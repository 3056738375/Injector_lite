package com.injector.loader.permission

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku

class PermissionManager(private val context: Context) {

    suspend fun checkAdbPermission(): Boolean = withContext(Dispatchers.IO) {
        try {
            return@withContext Shizuku.checkSelfPermission() == 0
        } catch (e: Exception) {
            false
        }
    }

    suspend fun requestAdbPermission() = withContext(Dispatchers.IO) {
        try {
            Shizuku.requestPermission(0)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun checkRootPermission(): Boolean = withContext(Dispatchers.IO) {
        executeCommand("su -c 'id'").contains("uid=0")
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

    suspend fun getEffectivePermission(): PermissionLevel = withContext(Dispatchers.IO) {
        return@withContext when {
            checkRootPermission() -> PermissionLevel.ROOT
            checkAdbPermission() -> PermissionLevel.ADB
            else -> PermissionLevel.NONE
        }
    }
}

enum class PermissionLevel {
    NONE, ADB, ROOT
}
