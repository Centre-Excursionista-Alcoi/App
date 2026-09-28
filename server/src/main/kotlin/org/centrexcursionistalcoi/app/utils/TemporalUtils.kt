package org.centrexcursionistalcoi.app.utils

import kotlin.time.Instant

/**
 * Converts a [Long] representing epoch milliseconds to an [Instant].
 * @return the corresponding [Instant].
 */
fun Long.toInstant() = Instant.fromEpochMilliseconds(this)
