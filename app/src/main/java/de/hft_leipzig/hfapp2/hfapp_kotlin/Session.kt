package de.hft_leipzig.hfapp2.hfapp_kotlin

import android.os.Parcelable
import android.os.Parcel




data class Session(val uid: Int): Parcelable {
    var sessionID: String? = ""
    var exportedStatus: Int? = 0

    constructor(_sessionID: String) : this(0) {
        sessionID = _sessionID
    }

    constructor(parcel: Parcel) : this(
        parcel.readString()!!)

    companion object CREATOR : Parcelable.Creator<Session> {
        override fun createFromParcel(parcel: Parcel): Session {
            return Session(parcel)
        }

        override fun newArray(size: Int): Array<Session?> {
            return arrayOfNulls(size)
        }
    }

    override fun equals(other: Any?): Boolean {
        return sessionID == other.toString()
    }

    override fun toString(): String {
        return sessionID!!
    }


    override fun writeToParcel(parcel: Parcel, i: Int) {
        parcel.writeString(sessionID)
    }

    override fun describeContents(): Int {
        return 0
    }
}