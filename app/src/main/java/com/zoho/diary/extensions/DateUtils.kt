package com.zoho.diary.extensions

import android.annotation.SuppressLint
import java.text.SimpleDateFormat
import java.util.*

@SuppressLint("SimpleDateFormat")
fun Date.findYear(): Int{
    val sf = SimpleDateFormat("yyyy")
    return Integer.parseInt(sf.format(this))
}