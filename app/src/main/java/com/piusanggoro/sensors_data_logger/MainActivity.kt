package com.piusanggoro.sensors_data_logger

import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.PowerManager
import android.os.PowerManager.WakeLock
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import java.io.IOException
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

class MainActivity : AppCompatActivity() {
    private val mConfig = IMUConfig()
    private var mIMUSession: IMUSession? = null
    private val mHandler = Handler()
    private val mIsRecording = AtomicBoolean(false)
    private var mWakeLock: WakeLock? = null
    private var mLabelAccelDataX: TextView? = null
    private var mLabelAccelDataY: TextView? = null
    private var mLabelAccelDataZ: TextView? = null
    private var mLabelGyroDataX: TextView? = null
    private var mLabelGyroDataY: TextView? = null
    private var mLabelGyroDataZ: TextView? = null
    private var mLabelMagnetDataX: TextView? = null
    private var mLabelMagnetDataY: TextView? = null
    private var mLabelMagnetDataZ: TextView? = null
    private var mStartStopButton: Button? = null
    private var mLabelInterfaceTime: TextView? = null
    private val mInterfaceTimer = Timer()
    private var mSecondCounter = 0
    private var powerManager: PowerManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initializeViews()
        mIMUSession = IMUSession(this)

        powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        mWakeLock = powerManager!!.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "sensors_data_logger:wakelocktag")
        mWakeLock!!.acquire()

        displayIMUSensorMeasurements()
        mLabelInterfaceTime!!.setText(R.string.ready_title)
    }

    override fun onResume() {
        super.onResume()
        if (!hasPermissions(this, *REQUIRED_PERMISSIONS)) {
            requestPermissions(REQUIRED_PERMISSIONS, REQUEST_CODE_ANDROID)
        }
        //updateConfig()
    }

    override fun onPause() {
        super.onPause()
    }

    override fun onDestroy() {
        if (mIsRecording.get()) {
            stopRecording()
        }
        if (mWakeLock!!.isHeld) {
            mWakeLock!!.release()
        }
        mIMUSession!!.unregisterSensors()
        super.onDestroy()
    }

    fun startStopRecording(view: View?) {
        if (!mIsRecording.get()) {
            startRecording()
            mSecondCounter = 0
            mInterfaceTimer.schedule(object : TimerTask() {
                override fun run() {
                    mSecondCounter += 1
                    mLabelInterfaceTime!!.text = interfaceIntTime(mSecondCounter)
                }
            }, 0, 1000)
        } else {
            stopRecording()
            mInterfaceTimer.cancel()
            mLabelInterfaceTime!!.setText(R.string.ready_title)
        }
    }

    private fun startRecording() {
        var outputFolder: String? = null
        try {
            val folder = OutputDirectoryManager(mConfig.folderPrefix, mConfig.suffix)
            outputFolder = folder.outputDirectory
            if (outputFolder != null) {
                mConfig.outputFolder = outputFolder
            }
        } catch (e: IOException) {
            showAlertAndStop("Cannot create output folder.")
            e.printStackTrace()
        }
        mIMUSession!!.startSession(outputFolder)
        mIsRecording.set(true)
        runOnUiThread {
            mStartStopButton!!.isEnabled = true
            mStartStopButton!!.setText(R.string.stop_title)
        }
        showToast("Mulai merekam!")
    }

    protected fun stopRecording() {
        mHandler.post {
            mIMUSession!!.stopSession()
            mIsRecording.set(false)
            showToast("Rekaman berhenti!")
            resetUI()
        }
    }

    /*private fun updateConfig() {
        val MICRO_TO_SEC = 1000
    }*/

    fun showAlertAndStop(text: String?) {
        runOnUiThread {
            AlertDialog.Builder(this@MainActivity)
                    .setTitle(text)
                    .setCancelable(false)
                    .setPositiveButton(android.R.string.yes) { dialogInterface, i -> stopRecording() }.show()
        }
    }

    fun showToast(text: String?) {
        runOnUiThread { Toast.makeText(this@MainActivity, text, Toast.LENGTH_SHORT).show() }
    }

    private fun resetUI() {
        runOnUiThread {
            mStartStopButton!!.isEnabled = true
            mStartStopButton!!.setText(R.string.start_title)
        }
    }

    override fun onBackPressed() {
        if (!mIsRecording.get()) {
            super.onBackPressed()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (requestCode != REQUEST_CODE_ANDROID) {
            return
        }
        for (grantResult in grantResults) {
            if (grantResult == PackageManager.PERMISSION_DENIED) {
                showToast("Permission not granted")
                finish()
                return
            }
        }
    }

    private fun initializeViews() {
        mLabelAccelDataX = findViewById<View>(R.id.label_accel_X) as TextView
        mLabelAccelDataY = findViewById<View>(R.id.label_accel_Y) as TextView
        mLabelAccelDataZ = findViewById<View>(R.id.label_accel_Z) as TextView
        mLabelGyroDataX = findViewById<View>(R.id.label_gyro_X) as TextView
        mLabelGyroDataY = findViewById<View>(R.id.label_gyro_Y) as TextView
        mLabelGyroDataZ = findViewById<View>(R.id.label_gyro_Z) as TextView
        mLabelMagnetDataX = findViewById<View>(R.id.label_magnet_X) as TextView
        mLabelMagnetDataY = findViewById<View>(R.id.label_magnet_Y) as TextView
        mLabelMagnetDataZ = findViewById<View>(R.id.label_magnet_Z) as TextView
        mStartStopButton = findViewById<View>(R.id.button_start_stop) as Button
        mLabelInterfaceTime = findViewById<View>(R.id.label_interface_time) as TextView
    }

    private fun displayIMUSensorMeasurements() {
        val acce_data = mIMUSession!!.acceMeasure
        val gyro_data = mIMUSession!!.gyroMeasure
        val magnet_data = mIMUSession!!.magnetMeasure
        runOnUiThread {
            mLabelAccelDataX!!.text = String.format(Locale.US, "%.3f", acce_data[0])
            mLabelAccelDataY!!.text = String.format(Locale.US, "%.3f", acce_data[1])
            mLabelAccelDataZ!!.text = String.format(Locale.US, "%.3f", acce_data[2])
            mLabelGyroDataX!!.text = String.format(Locale.US, "%.3f", gyro_data[0])
            mLabelGyroDataY!!.text = String.format(Locale.US, "%.3f", gyro_data[1])
            mLabelGyroDataZ!!.text = String.format(Locale.US, "%.3f", gyro_data[2])
            mLabelMagnetDataX!!.text = String.format(Locale.US, "%.3f", magnet_data[0])
            mLabelMagnetDataY!!.text = String.format(Locale.US, "%.3f", magnet_data[1])
            mLabelMagnetDataZ!!.text = String.format(Locale.US, "%.3f", magnet_data[2])
        }
        val displayInterval: Long = 100
        mHandler.postDelayed({ displayIMUSensorMeasurements() }, displayInterval)
    }

    private fun interfaceIntTime(second: Int): String {
        if (second < 0) {
            showAlertAndStop("Second cannot be negative.")
        }
        var input = second
        val hours = input / 3600
        input = input % 3600
        val mins = input / 60
        val secs = input % 60
        return String.format(Locale.US, "%02d:%02d:%02d", hours, mins, secs)
    }

    companion object {
        private const val REQUEST_CODE_ANDROID = 1001
        private val REQUIRED_PERMISSIONS = arrayOf(
                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION
        )

        private fun hasPermissions(context: Context, vararg permissions: String): Boolean {
            for (permission in permissions) {
                if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false
                }
            }
            return true
        }
    }
}