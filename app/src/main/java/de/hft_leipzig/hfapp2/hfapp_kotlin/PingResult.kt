package de.hft_leipzig.hfapp2.hfapp_kotlin

import androidx.room.*
import android.location.Location
import android.os.Build
import java.text.SimpleDateFormat
import java.util.*
import java.util.regex.Pattern
import kotlin.math.roundToLong

const val PING_REGEX = "(?:\\[(?<ts>[0-9.]+)] )?(?<size>[0-9]+) bytes from (?<ip>[0-9.]+): icmp_seq=(?<seq>[0-9]+) ttl=(?<ttl>[0-9]+)(?: time=(?<rtt>[0-9.]+) (?<rttmetric>\\w+))?"
const val CSV_HEADER_PING = "timestamp,sessionID,datetime,target,ttl,seq,rtt,rtt_metric,size,lat,lon,alt,acc,speed,speed_acc"
@Dao
interface PingResultDao {
    @Query("SELECT * FROM pingResult")
    fun getAll(): List<PingResult>

    @Query("SELECT * FROM pingResult WHERE sessionID=:sessionID")
    fun getAllBySessionID(sessionID: String): List<PingResult>

    @Query("SELECT * FROM pingResult WHERE sessionID IN(:sessionIDs)")
    fun getAllBySessionID(sessionIDs: Array<String>): List<PingResult>

    @Query("SELECT DISTINCT uid, sessionID, exportedStatus FROM PingResult")
    fun getAllSessions(): List<Session>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(vararg pingResults: PingResult)

    @Query("DELETE FROM pingResult WHERE sessionID=:sessionID")
    fun deleteBySessionID(sessionID: String)

    @Query("DELETE FROM pingResult WHERE sessionID IN(:sessionIDs)")
    fun deleteBySessionID(sessionIDs: Array<String>)

    @Query("UPDATE pingResult SET exportedStatus=exportedStatus|:exportedStatus WHERE sessionID IN(:sessionIDs)")
    fun orExportStatusBySessionID(exportedStatus: Int, sessionIDs: Array<String>)

    @Query("UPDATE pingResult SET exportedStatus=exportedStatus|:exportedStatus WHERE sessionID=:sessionID")
    fun orExportStatusBySessionID(exportedStatus: Int, sessionID: String)

    @Query("UPDATE pingResult SET exportedStatus=:exportedStatus WHERE sessionID=:sessionID")
    fun setExportStatusBySessionID(exportedStatus: Int, sessionID: String)

    @Query("UPDATE pingResult SET exportedStatus=:exportedStatus WHERE sessionID IN(:sessionIDs)")
    fun setExportStatusBySessionID(exportedStatus: Int, sessionIDs: Array<String>)
}

@Entity(primaryKeys = ["timestamp", "sessionID", "seq"])
data class PingResult(val uid: Int) {
    var timestamp: Long = 0
    var sessionID: String = ""
    var datetime: String = ""
    var exportedStatus: Int = 0
    var target: String = ""
    var ttl: Int = NAN
    var seq: Int = NAN
    var rtt: Float = NAN_F
    var rttMetric: String = ""
    var size: Int = NAN
    var locLatitude: Double = NAN_F.toDouble()
    var locLongitude: Double = NAN_F.toDouble()
    var locAltitude: Double = NAN_F.toDouble()
    var locAccuracy: Float = NAN_F
    var locSpeed: Float = NAN_F
    var locSpeedAcc: Float = NAN_F
    @Ignore var location: Location? = null

    fun toCSVRow(sep: String? = ","): String {
        var res = timestamp.toString() + sep
        res += sessionID + sep
        res += datetime + sep
        res += target + sep

        res += ttl.toString() + sep
        res += seq.toString() + sep
        res += rtt.toString() + sep
        res += rttMetric + sep
        res += size.toString() + sep

        res += locLatitude.toString() + sep
        res += locLongitude.toString() + sep
        res += locAltitude.toString() + sep
        res += locAccuracy.toString() + sep
        res += locSpeed.toString() + sep
        res += locSpeedAcc.toString() + sep
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

    constructor(pingString: String, sessionID: String? = "test"): this(0) {
        this.sessionID = sessionID?:""
        parsePingString(pingString)
    }

    private fun parsePingString(s: String) {
        val re = Pattern.compile(
            PING_REGEX,
            Pattern.CASE_INSENSITIVE.or(Pattern.DOTALL))

        val res = re.matcher(s)
        res.find()

        timestamp = (res.group(1).toDouble()*1000).roundToLong()
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        datetime = sdf.format(timestamp)

        size = res.group(2).toInt()
        target = res.group(3)
        seq = res.group(4).toInt()
        ttl = res.group(5).toInt()

        rtt = if (res.group(6) != null) res.group(6).toFloat() else NAN_F
        rttMetric = if (res.group(7) != null) res.group(7) else NAN.toString()
    }

    override fun toString(): String {
        return "PingResult"
    }

    fun strOrNan(valueToConv: Int): String {
        return if (valueToConv == NAN) "NaN" else valueToConv.toString()
    }

    fun strOrNan(valueToConv: String): String {
        return if (valueToConv == NAN.toString()) "NaN" else valueToConv
    }
}