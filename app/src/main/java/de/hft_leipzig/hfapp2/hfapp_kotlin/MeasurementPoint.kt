package de.hft_leipzig.hfapp2.hfapp_kotlin

import android.os.Build
import android.os.SystemClock
import android.telephony.*
import java.text.SimpleDateFormat
import java.util.*

const val NAN =  2147483647

class MeasurementPoint {
    var timestamp: Long = 0
    var datetime: String = ""
    var type: String = ""
    var status: String = NAN.toString()
    var band: Int = NAN
    var mcc: String = NAN.toString()
    var mnc: String = NAN.toString()
    var pci: Int = NAN
    var rsrp: Int = NAN
    var rsrq: Int = NAN
    var rssnr: Int = NAN
    var ta: Int = NAN
    var cqi: Int = NAN

    constructor(cellInfo: CellInfo) {
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

    fun toCsvRow(sep: String? = ","): String {
        var res = ""
        res += timestamp.toString() + sep
        res += type + sep
        res += status + sep
        res += mcc.toString() + sep
        res += band.toString() + sep
        res += mnc.toString() + sep
        res += pci.toString() + sep
        res += rsrp.toString() + sep
        res += rsrq.toString() + sep
        res += rssnr.toString() + sep
        res += ta.toString() + sep
        res += cqi.toString() + sep

        return res
    }

    fun parseCellInfo(cellInfo: CellInfo) {
        val millisecondsSinceEvent = (SystemClock.elapsedRealtimeNanos() - cellInfo.timeStamp) / 1000000L
        val timeOfEvent = System.currentTimeMillis() - millisecondsSinceEvent
        val sdf = SimpleDateFormat("HH:mm:ss.SSS")
        val infoDate = Date(timeOfEvent)

        timestamp = timeOfEvent
        datetime = sdf.format(infoDate)

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
            }
        }
    }
}