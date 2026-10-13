package org.centrexcursionistalcoi.app.security.crypto

import java.util.Base64

private val base64Url = Base64.getUrlEncoder().withoutPadding()

/** Base64Url without padding: the encoding WebAuthn uses throughout, and what tokens and ids are written in. */
fun ByteArray.toBase64Url(): String = base64Url.encodeToString(this)

/** The bytes of this Base64Url string, with or without padding. */
fun String.fromBase64Url(): ByteArray = Base64.getUrlDecoder().decode(this)

/** Lowercase hexadecimal, two digits per byte. */
fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }
