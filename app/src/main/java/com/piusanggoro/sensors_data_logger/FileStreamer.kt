package com.piusanggoro.sensors_data_logger

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.security.KeyException
import java.util.*

open class FileStreamer (private val mContext: Context, val outputFolder: String) {
    private val mFileWriters = HashMap<String, BufferedWriter?>()

    @Throws(IOException::class)
    fun addFile(writerId: String, fileName: String) {
        if (mFileWriters.containsKey(writerId)) {
            Log.w(LOG_TAG, "tambah File: $writerId sudah ada.")
            return
        }

        val fileTimestamp = Calendar.getInstance()
        val timeHeader = "# Dibuat pada " + fileTimestamp.time.toString() + " di STMIK AKAKOM\n"
        val newWriter = createFile(outputFolder + "/" + fileName, timeHeader)
        mFileWriters[writerId] = newWriter
    }

    @Throws(IOException::class)
    private fun createFile(path: String, timeHeader: String?): BufferedWriter {
        val file = File(path)
        val writer = BufferedWriter(FileWriter(file))
        val scanIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
        scanIntent.data = Uri.fromFile(file)
        mContext.sendBroadcast(scanIntent)
        if (timeHeader != null && timeHeader.length != 0) {
            writer.append(timeHeader)
            writer.flush()
        }
        return writer
    }

    fun getFileWriter(writerId: String?): BufferedWriter? {
        return mFileWriters[writerId]
    }

    @Throws(IOException::class, KeyException::class)
    fun addRecord(timestamp: Long, writerId: String, numValues: Int, values: FloatArray) { // execute the block with only one thread
        synchronized(this) {
            val writer = getFileWriter(writerId)
                    ?: throw KeyException("tambah rekaman: $writerId tidak ketemu.")
            val stringBuilder = StringBuilder()
            stringBuilder.append(timestamp)
            for (i in 0 until numValues) {
                stringBuilder.append(String.format(Locale.US, " %.6f", values[i]))
            }
            stringBuilder.append(" \n")
            writer.write(stringBuilder.toString())
        }
    }

    @Throws(IOException::class)
    open fun endFiles() { // execute the block with only one thread
        synchronized(this) {
            for (eachWriter in mFileWriters.values) {
                eachWriter!!.flush()
                eachWriter.close()
            }
        }
    }

    companion object {
        private val LOG_TAG = FileStreamer::class.java.name
    }
}