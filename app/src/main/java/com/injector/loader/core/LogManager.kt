package com.injector.loader.core

import java.text.SimpleDateFormat
import java.util.*

class LogManager {
    private val logs = mutableListOf<LogEntry>()
    private val listeners = mutableListOf<(LogEntry) -> Unit>()
    private val dateFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    data class LogEntry(val timestamp: String, val level: String, val message: String)

    fun log(msg: String, level: String = "INFO") {
        val entry = LogEntry(dateFormat.format(Date()), level, msg)
        logs.add(entry)
        listeners.forEach { it(entry) }
    }

    fun info(msg: String) = log(msg, "INFO")
    fun debug(msg: String) = log(msg, "DEBUG")
    fun error(msg: String) = log(msg, "ERROR")
    fun success(msg: String) = log(msg, "✓")

    fun getLogs() = logs.toList()
    fun clearLogs() = logs.clear()
    fun addListener(listener: (LogEntry) -> Unit) = listeners.add(listener)

    companion object {
        private var instance: LogManager? = null
        fun getInstance() = instance ?: LogManager().also { instance = it }
    }
}
