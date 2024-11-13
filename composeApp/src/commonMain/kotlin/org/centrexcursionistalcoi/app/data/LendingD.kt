package org.centrexcursionistalcoi.app.data

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.centrexcursionistalcoi.app.server.response.data.LendingD

fun LendingD.fromDate(): LocalDate =
    Instant.fromEpochMilliseconds(from!!).toLocalDateTime(TimeZone.currentSystemDefault()).date

fun LendingD.toDate(): LocalDate =
    Instant.fromEpochMilliseconds(to!!).toLocalDateTime(TimeZone.currentSystemDefault()).date

fun LendingD.takenDate(): LocalDateTime =
    Instant.fromEpochMilliseconds(takenAt!!).toLocalDateTime(TimeZone.currentSystemDefault())

fun LendingD.returnedDate(): LocalDateTime =
    Instant.fromEpochMilliseconds(returnedAt!!).toLocalDateTime(TimeZone.currentSystemDefault())
