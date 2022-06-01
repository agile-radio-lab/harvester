package de.hft_leipzig.hfapp2.hfapp_kotlin

import androidx.room.*
import android.content.Context
import android.location.Location
import android.os.Build
import android.os.SystemClock
import android.telephony.*
import android.util.Log
import android.widget.TableRow
import android.widget.TextView
import androidx.annotation.RequiresApi
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

const val NAN =  2147483647
const val NAN_F =  2147483647f

data class MeasurementMapPoint(var locLatitude: Double, var locLongitude: Double)

@Dao
interface MeasurementPointDao {
    @Query("SELECT * FROM measurementPoint")
    fun getAll(): List<MeasurementPoint>

    @Query("SELECT * FROM measurementPoint WHERE sessionID=:sessionID")
    fun getAllBySessionID(sessionID: String): List<MeasurementPoint>

    @Query("SELECT * FROM measurementPoint WHERE sessionID IN(:sessionIDs)")
    fun getAllBySessionID(sessionIDs: Array<String>): List<MeasurementPoint>

    @Query("SELECT * FROM measurementPoint WHERE sessionID=:sessionID AND mcc!='0'")
    fun getPrimaryBySessionID(sessionID: String): List<MeasurementPoint>

    @Query("SELECT locLatitude, locLongitude FROM measurementPoint WHERE sessionID=:sessionID AND mcc!='0'")
    fun getMapPointsBySessionID(sessionID: String): List<MeasurementMapPoint>

    @Query("SELECT locLatitude, locLongitude FROM measurementPoint WHERE sessionID=:sessionID AND mcc!='0'  ORDER BY RANDOM() LIMIT 50")
    fun getRandomMapPointsBySessionID(sessionID: String): List<MeasurementMapPoint>

    @Query("SELECT DISTINCT uid, sessionID, exportedStatus FROM measurementPoint")
    fun getAllSessions(): List<Session>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(vararg measurementPoints: MeasurementPoint)

    @Query("DELETE FROM measurementPoint WHERE sessionID=:sessionID")
    fun deleteBySessionID(sessionID: String)

    @Query("DELETE FROM measurementPoint WHERE sessionID IN(:sessionIDs)")
    fun deleteBySessionID(sessionIDs: Array<String>)

    @Query("UPDATE measurementPoint SET exportedStatus=exportedStatus|:exportedStatus WHERE sessionID IN(:sessionIDs)")
    fun orExportStatusBySessionID(exportedStatus: Int, sessionIDs: Array<String>)

    @Query("UPDATE measurementPoint SET exportedStatus=exportedStatus|:exportedStatus WHERE sessionID=:sessionID")
    fun orExportStatusBySessionID(exportedStatus: Int, sessionID: String)

    @Query("UPDATE measurementPoint SET exportedStatus=:exportedStatus WHERE sessionID=:sessionID")
    fun setExportStatusBySessionID(exportedStatus: Int, sessionID: String)

    @Query("UPDATE measurementPoint SET exportedStatus=:exportedStatus WHERE sessionID IN(:sessionIDs)")
    fun setExportStatusBySessionID(exportedStatus: Int, sessionIDs: Array<String>)
}

@Entity(primaryKeys = ["timestamp", "sessionID", "ci", "pci"])
data class MeasurementPoint(val uid: Int) {
    var timestamp: Long = 0
    var sessionID: String = ""
    var datetime: String = ""
    var exportedStatus: Int = 0
    var type: String = ""
    var status: String = NAN.toString()
    var band: Int = NAN
    var mcc: String = NAN.toString()
    var mnc: String = NAN.toString()
    var pci: Int = NAN
    var rsrp: Int = NAN
    var rsrq: Int = NAN
    var asu: Int = NAN
    var rssnr: Double = NAN.toDouble()
    var lvl: Int = NAN
    var ta: Int = NAN
    var cqi: Int = NAN
    var ci: Int = NAN
    var locLatitude: Double = NAN_F.toDouble()
    var locLongitude: Double = NAN_F.toDouble()
    var locAltitude: Double = NAN_F.toDouble()
    var locAccuracy: Float = NAN_F
    var locSpeed: Float = NAN_F
    var locSpeedAcc: Float = NAN_F
    var rssi: Int = NAN
    var bw: Double = NAN.toDouble()
    var dbm: Int = NAN

    @Ignore var location: Location? = null

    fun toCSVRow(sep: String? = ";"): String {
        var res = timestamp.toString() + sep
        res += sessionID + sep
        res += datetime + sep
        res += type + sep
        res += status + sep
        res += band.toString() + sep
        res += mcc + sep
        res += mnc + sep
        res += pci.toString() + sep
        res += rsrp.toString() + sep
        res += rsrq.toString() + sep
        res += asu.toString() + sep
        res += rssnr.toString() + sep
        res += ta.toString() + sep
        res += cqi.toString() + sep
        res += ci.toString() + sep
        res += locLatitude.toString() + sep
        res += locLongitude.toString() + sep
        res += locAltitude.toString() + sep
        res += locAccuracy.toString() + sep
        res += locSpeed.toString() + sep
        res += locSpeedAcc.toString() + sep
        res += rssi.toString() + sep
        res += bw.toString()
        return res
    }

    fun newLocation(_location: Location? = null) {
        if (_location == null) {
            return
        }
        location = _location
        locLatitude = _location.latitude
        locLongitude = _location.longitude
        locAltitude = _location.altitude
        locAccuracy = _location.accuracy
        if (_location.hasSpeed()) {
            locSpeed = _location.speed
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (_location.hasSpeedAccuracy()) {
                locSpeedAcc = _location.speedAccuracyMetersPerSecond
            }
        }
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

    fun strOrNan(valueToConv: Double): String {
        return if (valueToConv == NAN.toDouble()) "NaN" else valueToConv.toString()
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

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun parseCellInfo(cellInfo: CellInfo) {
        val millisecondsSinceEvent = (SystemClock.elapsedRealtimeNanos() - cellInfo.timeStamp) / 1000000L
        val timeOfEvent = System.currentTimeMillis() - millisecondsSinceEvent

        exportedStatus = 0
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
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

        if (status == "Not serving") {
            status = if (cellInfo.isRegistered) {
                "Registered"
            } else {
                "Not Registered"
            }
        }

        when (cellInfo) {
            is CellInfoNr -> {
                type = "NR"
                if (cellInfo.cellSignalStrength is CellSignalStrengthNr) {
                    rsrp = (cellInfo.cellSignalStrength as CellSignalStrengthNr).ssRsrp
                    rsrq = (cellInfo.cellSignalStrength as CellSignalStrengthNr).ssRsrq
                    rssnr = (cellInfo.cellSignalStrength as CellSignalStrengthNr).ssSinr
                    asu = (cellInfo.cellSignalStrength as CellSignalStrengthNr).asuLevel
                    pci = (cellInfo.cellIdentity as CellIdentityNr).pci
                    band = (cellInfo.cellIdentity as CellIdentityNr).nrarfcn
                    mcc = (cellInfo.cellIdentity as CellIdentityNr).mccString.toString()
                    mnc = (cellInfo.cellIdentity as CellIdentityNr).mncString.toString()
                }
            }
            is CellInfoLte -> {
                Log.i("measurements","cellinfo: "+cellInfo.toString())
                type = "LTE"
                band = cellInfo.cellIdentity.earfcn
                pci = cellInfo.cellIdentity.pci
                asu = cellInfo.cellSignalStrength.asuLevel
                ci = cellInfo.cellIdentity.ci
                ta = cellInfo.cellSignalStrength.timingAdvance
                dbm = cellInfo.cellSignalStrength.dbm

                if (Build.VERSION.SDK_INT >= 26) {
                    rsrp = cellInfo.cellSignalStrength.rsrp
                    rsrq = cellInfo.cellSignalStrength.rsrq
                    //rssnr = cellInfo.cellSignalStrength.rssnr.toDouble()
                    cqi = cellInfo.cellSignalStrength.cqi
                }
                if (Build.VERSION.SDK_INT >= 29) {
                    rssi = cellInfo.cellSignalStrength.rssi
                }
                if (Build.VERSION.SDK_INT >= 28) {
                    bw = cellInfo.cellIdentity.bandwidth/1000.0
                    mcc = if (cellInfo.cellIdentity.mccString != null) {
                        cellInfo.cellIdentity.mccString!!
                    } else {
                        NAN.toString()
                    }
                    mnc = if (cellInfo.cellIdentity.mncString != null) {
                        cellInfo.cellIdentity.mncString!!
                    } else {
                        NAN.toString()
                    }
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
                    mcc = if (cellInfo.cellIdentity.mccString != null) {
                        cellInfo.cellIdentity.mccString!!
                    } else {
                        NAN.toString()
                    }
                    mnc = if (cellInfo.cellIdentity.mncString != null) {
                        cellInfo.cellIdentity.mncString!!
                    } else {
                        NAN.toString()
                    }
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
                    mcc = if (cellInfo.cellIdentity.mccString != null) {
                        cellInfo.cellIdentity.mccString!!
                    } else {
                        NAN.toString()
                    }
                    mnc = if (cellInfo.cellIdentity.mncString != null) {
                        cellInfo.cellIdentity.mncString!!
                    } else {
                        NAN.toString()
                    }
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