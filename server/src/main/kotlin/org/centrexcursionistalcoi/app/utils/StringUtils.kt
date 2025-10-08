package org.centrexcursionistalcoi.app.utils

import java.util.UUID

/**
 * Tries to convert the string to a UUID.
 * Returns `null` if the string is not a valid UUID.
 */
fun String.toUUIDOrNull() = try {
    UUID.fromString(this)
} catch (_: IllegalArgumentException) {
    null
}

/**
 * Tries to convert the string to a UUID.
 * Throws `IllegalArgumentException` if the string is not a valid UUID.
 */
fun String.toUUID() = UUID.fromString(this)
