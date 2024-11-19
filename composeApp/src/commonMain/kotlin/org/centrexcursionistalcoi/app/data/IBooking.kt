package org.centrexcursionistalcoi.app.data

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

fun IBookingD.fromDate(): LocalDate =
    Instant.fromEpochMilliseconds(from!!).toLocalDateTime(TimeZone.currentSystemDefault()).date

fun IBookingD.toDate(): LocalDate =
    Instant.fromEpochMilliseconds(to!!).toLocalDateTime(TimeZone.currentSystemDefault()).date

fun IBookingD.takenDate(): LocalDateTime =
    Instant.fromEpochMilliseconds(takenAt!!).toLocalDateTime(TimeZone.currentSystemDefault())

fun IBookingD.returnedDate(): LocalDateTime =
    Instant.fromEpochMilliseconds(returnedAt!!).toLocalDateTime(TimeZone.currentSystemDefault())
