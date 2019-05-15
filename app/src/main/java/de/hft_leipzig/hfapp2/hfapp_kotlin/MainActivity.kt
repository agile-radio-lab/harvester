package de.hft_leipzig.hfapp2.hfapp_kotlin

import android.app.ActivityManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.*
import android.support.v4.app.ActivityCompat
import android.support.v7.app.AppCompatActivity
import java.util.*
import android.support.v4.widget.SwipeRefreshLayout
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import com.facebook.stetho.Stetho
import android.view.View
import android.widget.*
import kotlinx.android.synthetic.main.measurement_row.*

const val PERMISSIONS_REQUEST_ALL = 0x1
const val PERMISSIONS_REQUEST_WRITE_EXTERNAL = 0x2

var ALL_PERMISSIONS = arrayOf(
    android.Manifest.permission.ACCESS_COARSE_LOCATION,
    android.Manifest.permission.ACCESS_FINE_LOCATION
)

class MainActivity : AppCompatActivity() {
    var myService: MeasurementService? = null
    var isBound = false

    private val mainHandler = Handler()
    private lateinit var backgroundTask: Runnable

    private val myConnection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            val binder = service as MeasurementService.MyLocalBinder
            myService = binder.getService()
            isBound = true
        }

        override fun onServiceDisconnected(name: ComponentName) {
            isBound = false
        }
    }

    private fun hasPermissions(context: Context?, permissions: Array<String>): Boolean {
        if (context != null) {
            for (permission in permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false
                }
            }
        }
        return true
    }

    private fun getPermissions(permissions: Array<String>, requestID: Int): Boolean {
        if (!hasPermissions(this, permissions)) {
            ActivityCompat.requestPermissions(this, permissions, requestID)
            return false
        }
        return true
    }

    private fun createCellInfoTableHeader(rightPadding: Int = 20, fontSize: Float = 10.0f): TableRow {
        val row = TableRow(this)
        val layoutParams = TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT)
        row.layoutParams = layoutParams

        val columns: ArrayList<TextView> = ArrayList()
        columns.add(createTextViewCell(this, getString(R.string.meas_time_label), rightPadding, fontSize))
        columns.add(createTextViewCell(this, getString(R.string.meas_status_label), rightPadding, fontSize))
        columns.add(createTextViewCell(this, getString(R.string.meas_freq_label), rightPadding, fontSize))
        columns.add(createTextViewCell(this, getString(R.string.meas_pci_label), rightPadding, fontSize))

        columns.add(createTextViewCell(this, getString(R.string.meas_rsrp_label), rightPadding, fontSize))
        columns.add(createTextViewCell(this, getString(R.string.meas_rsrq_label), rightPadding, fontSize))
        columns.add(createTextViewCell(this, getString(R.string.meas_rssnr_label), rightPadding, fontSize))
        columns.add(createTextViewCell(this, getString(R.string.meas_ta_label), rightPadding, fontSize))
        columns.add(createTextViewCell(this, getString(R.string.meas_cqi_label), rightPadding, fontSize))

        for (c in columns) {
            row.addView(c)
        }
        return row
    }

    private fun getMeasurements() {
        if (!hasPermissions(this, ALL_PERMISSIONS)) {
            return
        }
        val table: TableLayout = findViewById(R.id.tableResults)
        table.removeAllViews()
        val factory: LayoutInflater = layoutInflater
        val headerRow = factory.inflate(R.layout.measurement_row, table, false)
        table.addView(headerRow)

        if (isServiceRunning(MeasurementService::class.java)) {
            val tvElapsed: TextView = findViewById(R.id.tvElapsed)
            if (myService?.isRecording!!) {
                tvElapsed.text = myService?.elapsed()
            }

            val measurements = myService?.lastMeasurements
            for (mp in measurements!!) {
                if (mp.mcc != "0" && mp.mnc != "0" && mp.mnc != NAN.toString() && mp.mnc != NAN.toString()) {
                    val tvNetwork = findViewById<TextView>(R.id.tvNetwork)
                    val tvLatitude = findViewById<TextView>(R.id.tvLatitude)
                    val tvLongitude = findViewById<TextView>(R.id.tvLongitude)
                    val tvAccuracy = findViewById<TextView>(R.id.tvAccuracy)
                    val tvSpeed = findViewById<TextView>(R.id.tvSpeed)
                    val tvTime = findViewById<TextView>(R.id.tvTime)

                    tvTime.text = mp.datetime
                    tvNetwork.text = getString(R.string.meas_network_value, mp.strOrNan(mp.mcc), mp.strOrNan(mp.mnc))
                    if (mp.location != null) {
                        tvLatitude.text = getString(R.string.meas_latitude_value, mp.location?.latitude)
                        tvLongitude.text = getString(R.string.meas_longitude_value, mp.location?.longitude)
                        tvAccuracy.text = getString(R.string.meas_accuracy_value, mp.location?.accuracy)
                        tvSpeed.text = getString(R.string.meas_accuracy_value, mp.location?.speed)
                    }

                    val tvCqi = findViewById<TextView>(R.id.tvCqi)
                    val tvRssnr = findViewById<TextView>(R.id.tvRssnr)
                    val tvTa = findViewById<TextView>(R.id.tvTa)
                    tvCqi.text = mp.strOrNan(mp.cqi)
                    tvRssnr.text = mp.strOrNan(mp.rssnr)
                    tvTa.text = mp.strOrNan(mp.ta)
                }
                val factory: LayoutInflater = layoutInflater
                val rowView = factory.inflate(R.layout.measurement_row, table, false)
                val tvType = rowView.findViewById(R.id.tvType) as TextView
                val tvBand = rowView.findViewById(R.id.tvBand) as TextView
                val tvPci = rowView.findViewById(R.id.tvPci) as TextView
                val tvRsrp = rowView.findViewById(R.id.tvRsrp) as TextView
                val tvRsrq = rowView.findViewById(R.id.tvRsrq) as TextView
                val tvAsu = rowView.findViewById(R.id.tvAsu) as TextView
                tvType.text = mp.strOrNan(mp.type)
                tvBand.text = mp.strOrNan(mp.band)
                tvPci.text = mp.strOrNan(mp.pci)
                tvRsrp.text = mp.strOrNan(mp.rsrp)
                tvRsrq.text = mp.strOrNan(mp.rsrq)
                tvAsu.text = mp.strOrNan(mp.asu)
                table.addView(rowView)
            }
        }
    }

    private fun isServiceRunning(serviceClass: Class<*>): Boolean {
        val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        for (service in activityManager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.name == service.service.className) {
                return true
            }
        }
        return false
    }

    private fun startOrBindMeasurementService() {
        val serviceClass = MeasurementService::class.java
        val serviceIntent = Intent(applicationContext, serviceClass)
        if (!isServiceRunning(serviceClass)) {
            startService(serviceIntent)
            bindService(serviceIntent, myConnection, Context.BIND_AUTO_CREATE)
        } else {
            Toast.makeText(this, "Service is already running.", Toast.LENGTH_LONG).show()
            bindService(serviceIntent, myConnection, Context.BIND_AUTO_CREATE)
        }
    }

    private fun updateRecordingButtons() {
        if (!isServiceRunning(MeasurementService::class.java)) {
            Toast.makeText(this, "Service is not running.", Toast.LENGTH_LONG).show()
            return
        }

        val recordButton: ImageButton = findViewById(R.id.ibtnRecord)
        if (myService?.isRecording!!) {
            recordButton.setImageResource(R.drawable.ic_baseline_pause_24px)
        } else {
            recordButton.setImageResource(R.drawable.ic_baseline_fiber_manual_record_24px)
        }
    }

    fun toggleRecording(@Suppress("UNUSED_PARAMETER") view: View) {
        if (!isServiceRunning(MeasurementService::class.java)) {
            Toast.makeText(this, "Service is not running.", Toast.LENGTH_LONG).show()
            return
        }
        myService?.toggleRecording()
        updateRecordingButtons()
    }

    fun stopRecording(@Suppress("UNUSED_PARAMETER") view: View) {
        if (!isServiceRunning(MeasurementService::class.java)) {
            Toast.makeText(this, "Service is not running.", Toast.LENGTH_LONG).show()
            return
        }
        myService?.stopMeasurement()
        updateRecordingButtons()
    }

    fun openMeasurementList(@Suppress("UNUSED_PARAMETER") view: View) {
        val randomIntent = Intent(this, SecondActivity::class.java)
        startActivity(randomIntent)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            PERMISSIONS_REQUEST_ALL -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    startOrBindMeasurementService()
                }
                return
            }
            PERMISSIONS_REQUEST_WRITE_EXTERNAL -> {
                Toast.makeText(this, "Try saving file again", Toast.LENGTH_LONG).show()
                return
            }
            else -> { }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        Stetho.initializeWithDefaults(this)

        val pullToRefresh = findViewById<SwipeRefreshLayout>(R.id.pullToRefresh)
        pullToRefresh.setOnRefreshListener {
            getMeasurements()
            pullToRefresh.isRefreshing = false
        }

        backgroundTask = Runnable{
            getMeasurements()
            mainHandler.postDelayed(
                backgroundTask,
                1000
            )
        }
        if (getPermissions(ALL_PERMISSIONS, PERMISSIONS_REQUEST_ALL)) {
            startOrBindMeasurementService()
        }
    }

    override fun onPause() {
        mainHandler.removeCallbacks(backgroundTask)
        super.onPause()
    }

    override fun onResume() {
        mainHandler.postDelayed(backgroundTask, 1000)
        Handler().postDelayed({
            updateRecordingButtons()
        }, 2000)
        super.onResume()
    }

    override fun onDestroy() {
        val serviceClass = MeasurementService::class.java
        val serviceIntent = Intent(applicationContext, serviceClass)
        try {
            unbindService(myConnection)
        } catch (e: IllegalArgumentException) {
            Log.w("MainActivity", "Error Unbinding Service.")
        }
        if (isServiceRunning(MeasurementService::class.java)) {
            stopService(serviceIntent)
        }
        super.onDestroy()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        super.onOptionsItemSelected(item)
        when {
            item.itemId == R.id.save -> {
                if (isServiceRunning(MeasurementService::class.java)) {
                    if (!hasPermissions(this, arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE))) {
                        getPermissions(arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE),
                            PERMISSIONS_REQUEST_WRITE_EXTERNAL)
                        return false
                    }
                    myService?.saveMeasurement()
                }

            }
            item.itemId == R.id.status -> {
                if (isServiceRunning(MeasurementService::class.java)) {
                    Toast.makeText(this, "Service is running.", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(this, "Service is stopped.", Toast.LENGTH_LONG).show()
                }
            }
        }
        return true
    }
}
