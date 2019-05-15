package de.hft_leipzig.hfapp2.hfapp_kotlin


data class Session(val uid: Int) {
    var sessionID: String? = ""

    constructor(_sessionID: String) : this(0) {
        sessionID = _sessionID
    }

    override fun equals(other: Any?): Boolean {
        return sessionID == other.toString()
    }

    override fun toString(): String {
        return sessionID!!
    }
}