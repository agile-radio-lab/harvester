package de.hft_leipzig.hfapp2.hfapp_kotlin

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.icu.util.Measure
import android.os.Build
import android.os.Bundle
import android.os.Debug
import android.support.annotation.RequiresApi
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.telephony.*
import android.telephony.CellInfo.*
import android.util.Log
import android.view.View
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import android.widget.Toast
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.util.*
import android.os.SystemClock

const val PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION = 0x2

class MainActivity : AppCompatActivity() {
    private lateinit var tm: TelephonyManager
    private var phoneStateListener: PhoneStateListener = object : PhoneStateListener() {
        override fun onSignalStrengthsChanged(signalStrength: SignalStrength) {
            super.onSignalStrengthsChanged(signalStrength)
            getAllCellInfo()
        }

        override fun onCellInfoChanged(cellInfo: MutableList<CellInfo>?) {
            super.onCellInfoChanged(cellInfo)
            getAllCellInfo()
        }
    }
    private var isStarted = false
    private val csvHeader = "timestamp,type,status,band,mcc,mnc,pci,rsrp,rsrq,rssnr,ta,cqi"

    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    getAllCellInfo()
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return
            }
            else -> {
                // Ignore all other requests.
            }
        }
    }

    private fun getPermissions() {
        val hasPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)

        if (hasPermission != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION), PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION)
        } else {
            getAllCellInfo()
        }
    }

    private fun createCellInfoTableHeader(): TableRow {
        val row = TableRow(this)
        val layoutParams = TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT)
        row.layoutParams = layoutParams

        val rightPadding = 20

        val tvTime = TextView(this)
        tvTime.setPadding(3,3,rightPadding,3)
        tvTime.text = resources.getString(R.string.meas_time_label)
        val tvType = TextView(this)
        tvType.setPadding(3,3,rightPadding,3)
        tvType.text = resources.getString(R.string.meas_type_label)
        val tvStatus  = TextView(this)
        tvStatus.setPadding(3,3,rightPadding,3)
        tvStatus.text = resources.getString(R.string.meas_status_label)
        val tvFreq = TextView(this)
        tvFreq.setPadding(3,3,rightPadding,3)
        tvFreq.text = resources.getString(R.string.meas_freq_label)
        val tvMcc  = TextView(this)
        tvMcc.setPadding(3,3,rightPadding,3)
        tvMcc.text = resources.getString(R.string.meas_mcc_label)
        val tvMnc  = TextView(this)
        tvMnc.setPadding(3,3,rightPadding,3)
        tvMnc.text = resources.getString(R.string.meas_mnc_label)
        val tvPci  = TextView(this)
        tvPci.setPadding(3,3,rightPadding,3)
        tvPci.text = resources.getString(R.string.meas_pci_label)

        val tvRsrp  = TextView(this)
        tvRsrp.setPadding(3,3,rightPadding,3)
        tvRsrp.text = resources.getString(R.string.meas_rsrp_label)
        val tvRsrq  = TextView(this)
        tvRsrq.setPadding(3,3,rightPadding,3)
        tvRsrq.text = resources.getString(R.string.meas_rsrq_label)
        val tvRssnr  = TextView(this)
        tvRssnr.setPadding(3,3,rightPadding,3)
        tvRssnr.text = resources.getString(R.string.meas_rssnr_label)
        val tvTa  = TextView(this)
        tvTa.setPadding(3,3,rightPadding,3)
        tvTa.text = resources.getString(R.string.meas_ta_label)
        val tvCqi  = TextView(this)
        tvCqi.setPadding(3,3,rightPadding,3)
        tvCqi.text = resources.getString(R.string.meas_cqi_label)

        row.addView(tvTime)
        row.addView(tvType)
        row.addView(tvStatus)
        row.addView(tvFreq)
        row.addView(tvMcc)
        row.addView(tvMnc)
        row.addView(tvPci)
        row.addView(tvRsrp)
        row.addView(tvRsrq)
        row.addView(tvRssnr)
        row.addView(tvTa)
        row.addView(tvCqi)

        return row
    }

    private fun createCellInfoTableRow(mp: MeasurementPoint): TableRow {
        val row = TableRow(this)
        val layoutParams = TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT)
        row.layoutParams = layoutParams

        val rightPadding = 20

        val tvTime = TextView(this)
        tvTime.setPadding(3,3,rightPadding,3)
        val tvType = TextView(this)
        tvType.setPadding(3,3,rightPadding,3)
        val tvStatus  = TextView(this)
        tvStatus.setPadding(3,3,rightPadding,3)
        val tvFreq = TextView(this)
        tvFreq.setPadding(3,3,rightPadding,3)
        val tvMcc  = TextView(this)
        tvMcc.setPadding(3,3,rightPadding,3)
        val tvMnc  = TextView(this)
        tvMnc.setPadding(3,3,rightPadding,3)
        val tvPci  = TextView(this)
        tvPci.setPadding(3,3,rightPadding,3)

        val tvRsrp  = TextView(this)
        tvRsrp.setPadding(3,3,rightPadding,3)
        val tvRsrq  = TextView(this)
        tvRsrq.setPadding(3,3,rightPadding,3)
        val tvRssnr  = TextView(this)
        tvRssnr.setPadding(3,3,rightPadding,3)
        val tvTa  = TextView(this)
        tvTa.setPadding(3,3,rightPadding,3)
        val tvCqi  = TextView(this)
        tvCqi.setPadding(3,3,rightPadding,3)

        tvTime.text = mp.datetime
        tvType.text = mp.strOrNan(mp.type)
        tvStatus.text = mp.strOrNan(mp.status)
        tvFreq.text = mp.strOrNan(mp.band)
        tvMcc.text = mp.strOrNan(mp.mcc)
        tvMnc.text = mp.strOrNan(mp.mnc)
        tvPci.text = mp.strOrNan(mp.pci)
        tvRsrp.text = mp.strOrNan(mp.rsrp)
        tvRsrq.text = mp.strOrNan(mp.rsrq)
        tvRssnr.text = mp.strOrNan(mp.rssnr)
        tvTa.text = mp.strOrNan(mp.ta)
        tvCqi.text = mp.strOrNan(mp.cqi)

        row.addView(tvTime)
        row.addView(tvType)
        row.addView(tvStatus)
        row.addView(tvFreq)
        row.addView(tvMcc)
        row.addView(tvMnc)
        row.addView(tvPci)
        row.addView(tvRsrp)
        row.addView(tvRsrq)
        row.addView(tvRssnr)
        row.addView(tvTa)
        row.addView(tvCqi)
        return row
    }

    private fun getAllCellInfo() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            getPermissions()
            return
        }
        val allCellInfo = tm.allCellInfo


        if (allCellInfo.size == 0) {
            return
        }

        val table: TableLayout = findViewById(R.id.tableResults)
        table.removeAllViews()
        val headerRow = createCellInfoTableHeader()
        table.addView(headerRow)
        for (cellInfo in allCellInfo) {
            if (cellInfo == null) {
                continue
            }

            val mp = MeasurementPoint(cellInfo)
            val newRow = createCellInfoTableRow(mp)
            table.addView(newRow)
        }
    }
//
//    fun toastMe(view: View) {
//        val myToast = Toast.makeText(this, "Hello Toast!", Toast.LENGTH_SHORT)
//        myToast.show()
//    }

    fun randomMe(view: View) {
//        val count: Int = getCount()
        val randomIntent = Intent(this, SecondActivity::class.java)
        randomIntent.putExtra(SecondActivity.TOTAL_COUNT, 1)
        startActivity(randomIntent)
    }

    fun start(view: View) {
        val buttonView = findViewById<TextView>(R.id.button_start)
        if (isStarted) {
            buttonView.text = resources.getString(R.string.button_start)
            tm.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE)
        } else {
            buttonView.text = resources.getString(R.string.button_stop)
            tm.listen(phoneStateListener, PhoneStateListener.LISTEN_CELL_INFO or PhoneStateListener.LISTEN_SIGNAL_STRENGTHS)
        }
        isStarted = !isStarted
    }

    fun getMeasurement(view: View) {
        getAllCellInfo()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tm = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
    }
}
