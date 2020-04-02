package com.piusanggoro.sensors_data_logger

import android.os.Environment
import android.util.Log
import java.io.File
import java.io.FileNotFoundException
import java.text.SimpleDateFormat
import java.util.*

class OutputDirectoryManager {
    var outputDirectory: String? = null
        private set

    constructor(prefix: String?, suffix: String?) {
        update(prefix, suffix)
    }

    @Throws(FileNotFoundException::class)
    private fun update(prefix: String? = null, suffix: String? = null) {
        val currentTime = Calendar.getInstance()
        val formatter = SimpleDateFormat("yyyyMMddhhmmss")
        val externalDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        var folderName = formatter.format(currentTime.time)

        if (prefix != null) {
            folderName = prefix + folderName
        }

        if (suffix != null) {
            folderName = folderName + suffix
        }

        val outputDirectory = File(externalDirectory.absolutePath + "/" + folderName)
        if (!outputDirectory.exists()) {
            if (!outputDirectory.mkdir()) {
                Log.e(LOG_TAG, "update: folder bermasalah")
                throw FileNotFoundException()
            }
        }
        this.outputDirectory = outputDirectory.absolutePath
        Log.i(LOG_TAG, "update: folder: " + outputDirectory.absolutePath)
    }

    companion object {
        private val LOG_TAG = OutputDirectoryManager::class.java.name
    }
}