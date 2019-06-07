package de.hft_leipzig.hfapp2.hfapp_kotlin

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat

const val PERMISSIONS_REQUEST_ALL = 0x1
const val PERMISSIONS_REQUEST_WRITE_EXTERNAL = 0x2

val ALL_PERMISSIONS = arrayOf(
    android.Manifest.permission.ACCESS_COARSE_LOCATION,
    android.Manifest.permission.ACCESS_FINE_LOCATION,
    android.Manifest.permission.READ_PHONE_STATE
)


fun hasPermissions(context: Context?, permissions: Array<String>): Boolean {
    if (context != null) {
        for (permission in permissions) {
            if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                return false
            }
        }
    }
    return true
}

fun getPermissions(context: Context?, permissions: Array<String>, requestID: Int): Boolean {
    if (!hasPermissions(context, permissions)) {
        ActivityCompat.requestPermissions(context as Activity, permissions, requestID)
        return false
    }
    return true
}