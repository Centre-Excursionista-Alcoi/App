package org.centrexcursionistalcoi.app.data

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

fun SpaceBookingD.fromDate(): LocalDate =
    Instant.fromEpochMilliseconds(from!!).toLocalDateTime(TimeZone.currentSystemDefault()).date

fun SpaceBookingD.toDate(): LocalDate =
    Instant.fromEpochMilliseconds(to!!).toLocalDateTime(TimeZone.currentSystemDefault()).date

fun SpaceBookingD.takenDate(): LocalDateTime =
    Instant.fromEpochMilliseconds(takenAt!!).toLocalDateTime(TimeZone.currentSystemDefault())

fun SpaceBookingD.returnedDate(): LocalDateTime =
    Instant.fromEpochMilliseconds(returnedAt!!).toLocalDateTime(TimeZone.currentSystemDefault())
