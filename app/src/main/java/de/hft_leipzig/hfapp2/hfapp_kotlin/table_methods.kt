package de.hft_leipzig.hfapp2.hfapp_kotlin

import android.content.Context
import android.widget.TextView

fun createTextViewCell(context: Context, text: String, rightPadding: Int = 20, fontSize: Float = 10.0f): TextView {
    val tvNew = TextView(context)
    tvNew.setPadding(3,3,rightPadding,3)
    tvNew.text = text
    tvNew.textSize = fontSize

    return tvNew
}
