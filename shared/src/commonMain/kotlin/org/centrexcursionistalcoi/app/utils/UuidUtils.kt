package org.centrexcursionistalcoi.app.utils

import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@ExperimentalUuidApi
fun Uuid.isZero(): Boolean {
    return this == Uuid.fromLongs(0, 0)
}

@ExperimentalUuidApi
val Uuid.Companion.Zero get() = Uuid.fromLongs(0, 0)

fun String.toUuid(): Uuid = Uuid.parse(this)

fun String.toUuidOrNull(): Uuid? = try {
    Uuid.parse(this)
} catch (_: IllegalArgumentException) {
    null
}
