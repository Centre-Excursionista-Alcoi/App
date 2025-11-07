package org.centrexcursionistalcoi.app.utils

import java.util.UUID
import kotlin.random.Random

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

/**
 * Generates a random alphanumeric string of the given length.
 * @param length The length of the generated string. Defaults to 12 characters.
 */
fun generateRandomString(length: Int = 12): String {
    val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
    return (1..length)
        .map { chars[Random.nextInt(chars.length)] }
        .joinToString("")
}
