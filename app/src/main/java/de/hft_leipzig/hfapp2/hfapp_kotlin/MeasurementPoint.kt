package de.hft_leipzig.hfapp2.hfapp_kotlin

import android.arch.persistence.room.*
import android.content.Context
import android.location.Location
import android.os.Build
import android.os.SystemClock
import android.telephony.*
import android.widget.TableRow
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.*

const val NAN =  2147483647

@Dao
interface MeasurementPointDao {
    @Query("SELECT * FROM measurementPoint")
    fun getAll(): List<MeasurementPoint>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(vararg measurementPoints: MeasurementPoint)

    @Query("SELECT DISTINCT sessionID FROM measurementPoint")
    fun getAllSessions(): List<MeasurementPoint>
}

@Entity(primaryKeys = ["timestamp", "sessionID", "ci", "pci"])
data class MeasurementPoint(val uid: Int) {
    var timestamp: Long = 0
    var sessionID: String = ""
    var datetime: String = ""
    var type: String = ""
    var status: String = NAN.toString()
    var band: Int = NAN
    var mcc: String = NAN.toString()
    var mnc: String = NAN.toString()
    var pci: Int = NAN
    var rsrp: Int = NAN
    var rsrq: Int = NAN
    var asu: Int = NAN
    var rssnr: Int = NAN
    var ta: Int = NAN
    var cqi: Int = NAN
    var ci: Int = NAN
    var locLatidute: Double = Double.NaN
    var locLongitude: Double = Double.NaN
    var locAltidute: Double = Double.NaN
    var locAccuracy: Float = Float.NaN
    @Ignore var location: Location? = null


    fun newLocation(_location: Location? = null) {
        if (_location == null) {
            return
        }
        location = _location
        locLatidute = _location.latitude
        locLongitude = _location.longitude
        locAltidute = _location.altitude
        locAccuracy = _location.accuracy
    }

    constructor(cellInfo: CellInfo, sessionID: String? = "test"): this(0) {
        parseCellInfo(cellInfo)
    }

    override fun toString(): String {
        return "MeasurementPoint"
    }

    fun strOrNan(valueToConv: Int): String {
        return if (valueToConv == NAN) "NaN" else valueToConv.toString()
    }


    fun strOrNan(valueToConv: String): String {
        return if (valueToConv == NAN.toString()) "NaN" else valueToConv
    }

    fun toTableRow(context: Context, rightPadding: Int = 20, fontSize: Float = 10.0f): TableRow {
        val row = TableRow(context)
        val layoutParams = TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT)
        row.layoutParams = layoutParams

        val columns: ArrayList<TextView> = ArrayList()

        columns.add(createTextViewCell(context, datetime, rightPadding, fontSize))
        columns.add(createTextViewCell(context, strOrNan(status), rightPadding, fontSize))
        columns.add(createTextViewCell(context, strOrNan(band), rightPadding, fontSize))
        columns.add(createTextViewCell(context, strOrNan(pci), rightPadding, fontSize))

        columns.add(createTextViewCell(context, strOrNan(rsrp), rightPadding, fontSize))
        columns.add(createTextViewCell(context, strOrNan(rsrq), rightPadding, fontSize))
        columns.add(createTextViewCell(context, strOrNan(rssnr), rightPadding, fontSize))
        columns.add(createTextViewCell(context, strOrNan(ta), rightPadding, fontSize))
        columns.add(createTextViewCell(context, strOrNan(cqi), rightPadding, fontSize))

        for (c in columns) {
            row.addView(c)
        }

        return row
    }

    private fun parseCellInfo(cellInfo: CellInfo) {
        val millisecondsSinceEvent = (SystemClock.elapsedRealtimeNanos() - cellInfo.timeStamp) / 1000000L
        val timeOfEvent = System.currentTimeMillis() - millisecondsSinceEvent
//        val sdf = SimpleDateFormat("HH:mm:ssZ")
//        val infoDate = Date(timeOfEvent)

        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
        sdf.timeZone = TimeZone.getTimeZone("UTC")

        timestamp = timeOfEvent
        datetime = sdf.format(Date())
        if (Build.VERSION.SDK_INT >= 28) {
            when (cellInfo.cellConnectionStatus) {
                CellInfo.CONNECTION_NONE -> status = "Not serving"
                CellInfo.CONNECTION_PRIMARY_SERVING -> status = "Signaling/Data"
                CellInfo.CONNECTION_SECONDARY_SERVING -> status = "Data"
                CellInfo.CONNECTION_UNKNOWN -> status = "Unknown"
            }
        }

        when (cellInfo) {
            is CellInfoLte -> {
                type = "LTE"
                band = cellInfo.cellIdentity.earfcn
                pci = cellInfo.cellIdentity.pci
                ta = cellInfo.cellSignalStrength.timingAdvance
                asu = cellInfo.cellSignalStrength.asuLevel
                ci = cellInfo.cellIdentity.ci

                if (Build.VERSION.SDK_INT >= 26) {
                    cqi = cellInfo.cellSignalStrength.cqi
                    rsrp = cellInfo.cellSignalStrength.rsrp
                    rsrq = cellInfo.cellSignalStrength.rsrq
                    rssnr = cellInfo.cellSignalStrength.rssnr
                }
                if (Build.VERSION.SDK_INT >= 28) {
                    mcc = cellInfo.cellIdentity.mccString
                    mnc = cellInfo.cellIdentity.mncString
                } else {
                    mcc = cellInfo.cellIdentity.mcc.toString()
                    mnc = cellInfo.cellIdentity.mnc.toString()
                }
            }
            is CellInfoWcdma ->  {
                type = "WCDMA"
                band = cellInfo.cellIdentity.uarfcn
                rsrp = cellInfo.cellSignalStrength.dbm
                asu = cellInfo.cellSignalStrength.asuLevel
                ci = cellInfo.cellIdentity.cid

                if (Build.VERSION.SDK_INT >= 28) {
                    mcc = cellInfo.cellIdentity.mccString
                    mnc = cellInfo.cellIdentity.mncString
                } else {
                    mcc = cellInfo.cellIdentity.mcc.toString()
                    mnc = cellInfo.cellIdentity.mnc.toString()
                }
            }
            is CellInfoGsm -> {
                type = "GSM"
                band = cellInfo.cellIdentity.arfcn
                rsrp = cellInfo.cellSignalStrength.dbm
                asu = cellInfo.cellSignalStrength.asuLevel
                ci = cellInfo.cellIdentity.cid

                if (Build.VERSION.SDK_INT >= 28) {
                    mcc = cellInfo.cellIdentity.mccString
                    mnc = cellInfo.cellIdentity.mncString
                } else {
                    mcc = cellInfo.cellIdentity.mcc.toString()
                    mnc = cellInfo.cellIdentity.mnc.toString()
                }
                if (Build.VERSION.SDK_INT >= 26) {
                    ta = cellInfo.cellSignalStrength.timingAdvance
                }
            }
            is CellInfoCdma -> {
                type = "CDMA"
                asu = cellInfo.cellSignalStrength.asuLevel
                rsrp = cellInfo.cellSignalStrength.dbm
                ci = cellInfo.cellIdentity.basestationId

            }
        }
    }
}