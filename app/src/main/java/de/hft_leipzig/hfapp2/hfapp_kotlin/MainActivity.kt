package de.hft_leipzig.hfapp2.hfapp_kotlin

import android.app.ActivityManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.*
import androidx.appcompat.app.AppCompatActivity
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import com.facebook.stetho.Stetho
import android.view.View
import android.widget.*
import org.tensorflow.contrib.android.TensorFlowInferenceInterface
import kotlin.collections.ArrayList
import kotlinx.android.synthetic.main.measurement_row.*

class MainActivity : AppCompatActivity() {
    var myService: MeasurementService? = null
    var isBound = false
    lateinit var tfInterface: TensorFlowInferenceInterface

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

    private fun predict(mp: MeasurementPoint): Int {
        tfInterface.feed("dense_9_input", floatArrayOf(mp.rsrp.toFloat(),mp.rsrq.toFloat()), 1, 2)
        tfInterface.run(arrayOf("dense_10/Softmax"))
        val output = floatArrayOf(-1f, -1f, -1f, -1f, -1f)
        tfInterface.fetch("dense_10/Softmax", output)
        var maxIdx = -1
        var maxVal = -1f
        var curIdx = 0
        for (i in output) {
            if (i > maxVal) {
                maxVal = i
                maxIdx = curIdx
            }
            curIdx += 1
        }
        return maxIdx
    }

    private fun getSnrIndex(sinrVal: Double): Int {
        val range: ArrayList<Int> = arrayListOf(-13,  -1,  11,  23,  35)
        var idx = 0
        for (r in range) {
            if (sinrVal <= r) {
                return idx
            }
            idx += 1
        }
        return idx
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
            Log.i("measurements","val measurements = "+measurements.toString())
            for (mp in measurements!!) {
                if (mp.mcc != "0" && mp.mnc != "0" && mp.mnc != NAN.toString() && mp.mnc != NAN.toString()) {
                    if (mp.ta != NAN){
                        Log.i("measurements_achtung","ACHTUNG HERE IS TAAA!!!!: "+mp.ta.toString()+" ON THE "+mp.band.toString()+" BAND")
                    }
                    if (mp.rssi == NAN){
                        continue
                    }
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
                    val tvTa = findViewById<TextView>(R.id.tvTa)
                    val tvRssnr = findViewById<TextView>(R.id.tvRssnr)
                    val tvRssi = findViewById<TextView>(R.id.tvRssi)
                    val tvRsrp = findViewById<TextView>(R.id.tvRsrp)
                    val tvRsrq = findViewById<TextView>(R.id.tvRsrq)
//                    val tvDbm = findViewById<TextView>(R.id.tvDbm)
                    val tvBw = findViewById<TextView>(R.id.tvBw)
                    val tvLvl = findViewById<TextView>(R.id.tvLvl)

                    tvCqi.text = mp.strOrNan(mp.cqi)
                    if (mp.cqi != NAN) {
                        tvTa.text = mp.strOrNan(mp.cqi)
                    }
                    tvRssnr.text = mp.strOrNan(mp.rssnr)
                    if (mp.ta != NAN) {
                        tvTa.text = mp.strOrNan(mp.ta)
                    }
                    tvRssi.text = mp.strOrNan(mp.rssi)
                    tvRsrp.text = mp.strOrNan(mp.rsrp)
                    tvRsrq.text = mp.strOrNan(mp.rsrq)
//                    tvDbm.text = mp.strOrNan(mp.dbm)
                    tvBw.text = mp.strOrNan(mp.bw)
                    tvLvl.text = mp.strOrNan(mp.lvl)
                    
                    val idxActual = getSnrIndex(mp.rssnr)
                    val idxPredicted = predict(mp)
                    if (mp.rssnr < NAN) {
                        val tvActualClass = findViewById<TextView>(R.id.tvActualClass)
                        val tvPredictedClass = findViewById<TextView>(R.id.tvPredictedClass)
                        tvActualClass.text = idxActual.toString()
                        tvPredictedClass.text = idxPredicted.toString()
                    }
                }
                val layoutFactory: LayoutInflater = layoutInflater
                val rowView = layoutFactory.inflate(R.layout.measurement_row, table, false)
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
                Toast.makeText(this, "Try to save again", Toast.LENGTH_LONG).show()
                return
            }
            else -> { }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        Stetho.initializeWithDefaults(this)

        tfInterface = TensorFlowInferenceInterface(assets, "my_model.pb")

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
        if (getPermissions(this, ALL_PERMISSIONS, PERMISSIONS_REQUEST_ALL)) {
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
            item.itemId == R.id.about -> {
                val intent = Intent(applicationContext, AboutActivity::class.java)
                startActivity(intent)
            }
            item.itemId == R.id.settings -> {
                val intent = Intent(this, PreferenceActivity::class.java)
                startActivity(intent)

            }
        }
        return true
    }
}
