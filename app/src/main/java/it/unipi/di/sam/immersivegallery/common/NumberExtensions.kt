package it.unipi.di.sam.immersivegallery.common

import android.icu.text.DecimalFormat
import kotlin.math.log10
import kotlin.math.pow

val SIZE_UNITS = arrayOf("B", "KB", "MB", "GB", "TB")
val SIZE_UNIT_BASE = log10(1024.0)

val SIZE_UNIT_FORMATTER = DecimalFormat("#,##0.#")

fun Int.toSizeWithUnit(): String {
    val v = this

    // Negative values => 0
    if (v <= 0) return "0"

    // Find correct "group" (ex. KB) by counting how many times 1024 divide v (log_1024)
    val group = (log10(v.toDouble()) / SIZE_UNIT_BASE).toInt()

    // Transform v (in bytes) into v' (in "group")
    val v_ = v / (1024.0.pow(group.toDouble()))

    // Return formatted string
    return SIZE_UNIT_FORMATTER.format(v_) + " " + SIZE_UNITS[group]
}

fun Int?.toSizeWithUnit(): String = this?.toSizeWithUnit() ?: "null"
