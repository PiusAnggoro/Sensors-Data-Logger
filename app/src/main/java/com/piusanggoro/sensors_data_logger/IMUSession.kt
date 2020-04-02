package com.piusanggoro.sensors_data_logger

import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.net.Uri
import android.os.Environment
import android.util.Log
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.security.KeyException
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

class IMUSession(private val mContext: MainActivity) : SensorEventListener {
    private val mSensorManager: SensorManager
    private val mSensors = HashMap<String, Sensor>()
    private var mInitialStepCount = -1f
    private var mFileStreamer: FileStreamer? = null
    private val mIsRecording = AtomicBoolean(false)
    private val mIsWritingFile = AtomicBoolean(false)
    val acceMeasure = FloatArray(3)
    val gyroMeasure = FloatArray(3)
    val magnetMeasure = FloatArray(3)

    fun registerSensors() {
        for (eachSensor in mSensors.values) {
            mSensorManager.registerListener(this, eachSensor, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    fun unregisterSensors() {
        for (eachSensor in mSensors.values) {
            mSensorManager.unregisterListener(this, eachSensor)
        }
    }

    fun startSession(streamFolder: String?) { // initialize text file streams
        if (streamFolder != null) {
            mFileStreamer = FileStreamer(mContext, streamFolder)
            try {
                mFileStreamer!!.addFile("acce", "acce.txt")
                mFileStreamer!!.addFile("gyro", "gyro.txt")
                mFileStreamer!!.addFile("linacce", "linacce.txt")
                mFileStreamer!!.addFile("gravity", "gravity.txt")
                mFileStreamer!!.addFile("magnet", "magnet.txt")
                mFileStreamer!!.addFile("rv", "rv.txt")
                mFileStreamer!!.addFile("game_rv", "game_rv.txt")
                mFileStreamer!!.addFile("magnetic_rv", "magnetic_rv.txt")
                mIsWritingFile.set(true)
            } catch (e: IOException) {
                mContext.showToast("Error occurs when creating output IMU files.")
                e.printStackTrace()
            }
        }
        mIsRecording.set(true)
    }

    fun stopSession() {
        mIsRecording.set(false)
        if (mIsWritingFile.get()) { // close all recorded text files
            try {
                mFileStreamer!!.endFiles()
            } catch (e: IOException) {
                mContext.showToast("Error occurs when finishing IMU text files.")
                e.printStackTrace()
            }

            /*try {
                val acceCalibFile = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toString() + "/acce_calib.txt")
                val outAcceCalibFile = File(mFileStreamer!!.outputFolder + "/acce_calib.txt")
                if (acceCalibFile.exists()) {
                    val istr = FileInputStream(acceCalibFile)
                    val ostr = FileOutputStream(outAcceCalibFile)
                    val ichn = istr.channel
                    val ochn = ostr.channel
                    ichn.transferTo(0, ichn.size(), ochn)
                    istr.close()
                    ostr.close()
                    val scanIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
                    scanIntent.data = Uri.fromFile(outAcceCalibFile)
                    mContext.sendBroadcast(scanIntent)
                }
            } catch (e: IOException) {
                mContext.showToast("Error occurs when copying accelerometer calibration text files.")
                e.printStackTrace()
            }*/

            mIsWritingFile.set(false)
            mFileStreamer = null
        }
        mInitialStepCount = -1f
    }

    override fun onSensorChanged(sensorEvent: SensorEvent) { // set some variables
        val isFileSaved = mIsRecording.get() && mIsWritingFile.get()
        val timestamp = sensorEvent.timestamp
        val eachSensor = sensorEvent.sensor

        try {
            when (eachSensor.type) {
                Sensor.TYPE_ACCELEROMETER -> {
                    acceMeasure[0] = sensorEvent.values[0]
                    acceMeasure[1] = sensorEvent.values[1]
                    acceMeasure[2] = sensorEvent.values[2]
                    if (isFileSaved) {
                        mFileStreamer!!.addRecord(timestamp, "acce", 3, sensorEvent.values)
                    }
                }
                Sensor.TYPE_GYROSCOPE -> {
                    gyroMeasure[0] = sensorEvent.values[0]
                    gyroMeasure[1] = sensorEvent.values[1]
                    gyroMeasure[2] = sensorEvent.values[2]
                    if (isFileSaved) {
                        mFileStreamer!!.addRecord(timestamp, "gyro", 3, sensorEvent.values)
                    }
                }
                Sensor.TYPE_LINEAR_ACCELERATION -> if (isFileSaved) {
                    mFileStreamer!!.addRecord(timestamp, "linacce", 3, sensorEvent.values)
                }
                Sensor.TYPE_GRAVITY -> if (isFileSaved) {
                    mFileStreamer!!.addRecord(timestamp, "gravity", 3, sensorEvent.values)
                }
                Sensor.TYPE_MAGNETIC_FIELD -> {
                    magnetMeasure[0] = sensorEvent.values[0]
                    magnetMeasure[1] = sensorEvent.values[1]
                    magnetMeasure[2] = sensorEvent.values[2]
                    if (isFileSaved) {
                        mFileStreamer!!.addRecord(timestamp, "magnet", 3, sensorEvent.values)
                    }
                }
                Sensor.TYPE_ROTATION_VECTOR -> if (isFileSaved) {
                    mFileStreamer!!.addRecord(timestamp, "rv", 4, sensorEvent.values)
                }
                Sensor.TYPE_GAME_ROTATION_VECTOR -> if (isFileSaved) {
                    mFileStreamer!!.addRecord(timestamp, "game_rv", 4, sensorEvent.values)
                }
                Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR -> if (isFileSaved) {
                    mFileStreamer!!.addRecord(timestamp, "magnetic_rv", 4, sensorEvent.values)
                }
            }
        } catch (e: IOException) {
            Log.d(LOG_TAG, "onSensorChanged: Something is wrong.")
            e.printStackTrace()
        } catch (e: KeyException) {
            Log.d(LOG_TAG, "onSensorChanged: Something is wrong.")
            e.printStackTrace()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}
    val isRecording: Boolean
        get() = mIsRecording.get()

    companion object {
        private val LOG_TAG = IMUSession::class.java.name
    }

    init {
        mSensorManager = mContext.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        mSensors["acce"] = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        mSensors["gyro"] = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        mSensors["linacce"] = mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
        mSensors["gravity"] = mSensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY)
        mSensors["magnet"] = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        mSensors["rv"] = mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        mSensors["game_rv"] = mSensorManager.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR)
        mSensors["magnetic_rv"] = mSensorManager.getDefaultSensor(Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR)
        registerSensors()
    }
}