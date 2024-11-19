package org.centrexcursionistalcoi.app.utils

import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Returns the last moment of the day.
 *
 * It's equivalent to `this.plusDays(1).atStartOfDay().minusNanos(1)`.
 */
fun LocalDate.atEndOfDay(): LocalDateTime {
    return plusDays(1).atStartOfDay().minusNanos(1)
}
