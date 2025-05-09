package com.appsbyayush.noteit.utils

import android.content.Context
import android.util.DisplayMetrics

fun Int.toPx(context: Context): Float {
    val displayMetrics = context.resources.displayMetrics
    return Math.round(this * (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT)).toFloat()
}

fun Int.toDp(context: Context): Float {
    val displayMetrics = context.resources.displayMetrics
    return Math.round(this / (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT)).toFloat()
}