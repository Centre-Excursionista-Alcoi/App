package org.centrexcursionistalcoi.app.utils

import java.security.SecureRandom
import java.util.*
import kotlin.uuid.Uuid

/**
 * Tries to convert the string to a UUID.
 * Returns `null` if the string is not a valid UUID.
 */
fun String.toUUIDOrNull() = Uuid.parseOrNull(this)

/**
 * Tries to convert the string to a UUID.
 * Throws `IllegalArgumentException` if the string is not a valid UUID.
 */
fun String.toUUID() = Uuid.parse(this)

/**
 * Generates a random alphanumeric string of the given length.
 * @param length The length of the generated string. Defaults to 12 characters.
 */
fun generateRandomString(length: Int = 12): String {
    val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
    return (1..length)
        .map { chars[SecureRandom().nextInt(chars.length)] }
        .joinToString("")
}

/**
 * Escapes the characters that are special to HTML (`&`, `<`, `>`, `"`, `'`), so this string is safe to interpolate
 * into HTML markup -- an attribute value included, since `'`/`"` could otherwise break out of one.
 */
fun String.escapeHtml(): String = replace("&", "&amp;")
    .replace("<", "&lt;")
    .replace(">", "&gt;")
    .replace("\"", "&quot;")
    .replace("'", "&#39;")

/**
 * Generates a random alphanumeric string of the given length.
 * @param length The length of the generated string. Defaults to 12 characters.
 */
fun generateRandomString(length: Int = 12, random: Random): String {
    val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
    return (1..length)
        .map { chars[random.nextInt(chars.length)] }
        .joinToString("")
}
