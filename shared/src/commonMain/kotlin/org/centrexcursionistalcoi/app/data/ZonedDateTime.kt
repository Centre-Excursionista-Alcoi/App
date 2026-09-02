package org.centrexcursionistalcoi.app.data

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format.DateTimeComponents
import kotlinx.datetime.format.DateTimeFormat
import kotlinx.datetime.format.char
import kotlinx.datetime.format.format
import kotlinx.datetime.offsetAt
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.Serializable
import org.centrexcursionistalcoi.app.datetime.RFC_9557
import org.centrexcursionistalcoi.app.serializer.ZonedDateTimeSerializer
import kotlin.time.Instant

@Serializable(ZonedDateTimeSerializer::class)
data class ZonedDateTime(
    val timeZone: TimeZone,
    val date: LocalDate,
    val time: LocalTime,
) {
    val dateTime: LocalDateTime get() = LocalDateTime(date, time)

    fun toInstant(): Instant = dateTime.toInstant(timeZone)

    fun offset() = timeZone.offsetAt(toInstant())

    fun format(format: DateTimeFormat<DateTimeComponents>): String {
        return format.format {
            setZonedDateTime(this@ZonedDateTime)
        }
    }

    /**
     * Formats the [ZonedDateTime] as a compact string, using the format `YYYY/MM/DD` if the timezone
     * is the same as the system's, or `YYYY/MM/DD (TimeZone)` if it's different.
     */
    fun toStringCompact(): String {
        val simpleFormat = DateTimeComponents.Format {
            year()
            char('/')
            monthNumber()
            char('/')
            day()
        }
        val timezoneFormat = DateTimeComponents.Format {
            year()
            char('/')
            monthNumber()
            char('/')
            day()
            chars(" (")
            timeZoneId()
            char(')')
        }
        return if (timeZone == TimeZone.currentSystemDefault()) {
            // If the timezone is the same as the system's, we can format it directly
            format(simpleFormat)
        } else {
            // If the timezone is different, make it explicit in the format
            format(timezoneFormat)
        }
    }

    override fun toString(): String {
        return DateTimeComponents.Formats.RFC_9557.format {
            setZonedDateTime(this@ZonedDateTime)
        }
    }

    companion object {
        fun DateTimeComponents.setZonedDateTime(zonedDateTime: ZonedDateTime) {
            setDateTime(zonedDateTime.dateTime)

            timeZoneId = zonedDateTime.timeZone.id
            setOffset(zonedDateTime.offset())
        }

        fun parse(string: String): ZonedDateTime {
            val parsed = DateTimeComponents.Formats.RFC_9557.parse(string)
            val timeZoneId = parsed.timeZoneId
            require(timeZoneId != null) { "Time zone ID is missing in the serialized string." }
            return ZonedDateTime(
                timeZone = TimeZone.of(timeZoneId),
                date = parsed.toLocalDate(),
                time = parsed.toLocalTime()
            )
        }

        /** Builds a [ZonedDateTime] from an [instant], expressed as wall-clock date/time in [timeZone]. */
        fun fromInstant(instant: Instant, timeZone: TimeZone): ZonedDateTime {
            val localDateTime = instant.toLocalDateTime(timeZone)
            return ZonedDateTime(timeZone, localDateTime.date, localDateTime.time)
        }

        fun forSystemDefault(localDateTime: LocalDateTime): ZonedDateTime {
            val timeZone = TimeZone.currentSystemDefault()
            return ZonedDateTime(timeZone, localDateTime.date, localDateTime.time)
        }
    }
}
