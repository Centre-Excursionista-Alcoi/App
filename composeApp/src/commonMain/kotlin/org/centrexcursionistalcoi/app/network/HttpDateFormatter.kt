package org.centrexcursionistalcoi.app.network

import kotlin.time.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format
import kotlinx.datetime.format.DateTimeFormat
import kotlinx.datetime.format.DayOfWeekNames
import kotlinx.datetime.format.MonthNames
import kotlinx.datetime.format.char
import kotlinx.datetime.toLocalDateTime

object HttpDateFormatter {
    val rfc1123 = LocalDateTime.Format {
        // Use Kotlin DSL to build the format according to RFC 1123
        dayOfWeek(DayOfWeekNames.ENGLISH_ABBREVIATED) // EEE
        chars(", ")
        day()                                                  // dd
        char(' ')
        monthName(MonthNames.ENGLISH_ABBREVIATED)     // MMM
        char(' ')
        year()                                                // yyyy
        char(' ')
        hour()                                                // HH
        char(':')
        minute()                                              // mm
        char(':')
        second()                                              // ss
        char(' ')
        chars("GMT")                                   // literal 'GMT'
    }

    fun format(timestamp: Long, format: DateTimeFormat<LocalDateTime> = rfc1123): String {
        val instant = Instant.fromEpochMilliseconds(timestamp)
        val localDateTime = instant.toLocalDateTime(TimeZone.UTC)
        return localDateTime.format(format)
    }
}
