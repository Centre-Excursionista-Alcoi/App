package org.centrexcursionistalcoi.app.utils

fun Double.toStringWithDecimals(decimals: Int): String {
    val value = this.toString()
    val dotPos = value.indexOf('.')
    return value.substring(0, minOf(dotPos + decimals + 1, value.length))
}

fun bytesToHumanReadableSize(bytes: Double) = when {
    bytes >= 1 shl 30 -> (bytes / (1 shl 30)).toStringWithDecimals(1) + " GB"
    bytes >= 1 shl 20 -> (bytes / (1 shl 20)).toStringWithDecimals(1) + " MB"
    bytes >= 1 shl 10 -> (bytes / (1 shl 10)).toStringWithDecimals(1) + " kB"
    else -> "$bytes bytes"
}

fun ByteArray.humanReadableSize() = bytesToHumanReadableSize(size.toDouble())
