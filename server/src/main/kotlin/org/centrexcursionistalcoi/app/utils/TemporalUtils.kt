package org.centrexcursionistalcoi.app.utils

import java.time.Instant

/**
 * Converts a [Long] representing epoch milliseconds to a Java [Instant].
 * @return the corresponding [Instant].
 */
fun Long.toInstant() = Instant.ofEpochMilli(this)
