package com.injector.loader.core

import java.io.File

data class FileItem(val path: String, val name: String, val isDir: Boolean, val size: Long = 0)

class FileManager {
    fun listFiles(dir: String = "/sdcard"): List<FileItem> {
        val file = File(dir)
        if (!file.isDirectory) return emptyList()
        return (file.listFiles() ?: emptyArray()).map {
            FileItem(it.absolutePath, it.name, it.isDirectory, if (it.isFile) it.length() else 0)
        }.sortedWith(compareBy({ !it.isDir }, { it.name }))
    }

    fun getSOFiles(dir: String) = listFiles(dir).filter { it.isDir || it.name.endsWith(".so") }

    fun formatSize(bytes: Long) = when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        else -> "${bytes / (1024 * 1024)} MB"
    }

    fun exists(path: String) = File(path).exists()
    fun getSize(path: String) = if (File(path).exists()) File(path).length() else 0
}
