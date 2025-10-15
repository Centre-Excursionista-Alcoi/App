package org.centrexcursionistalcoi.app.utils

fun ByteArray?.isNullOrEmpty(): Boolean {
    return this == null || this.isEmpty()
}
