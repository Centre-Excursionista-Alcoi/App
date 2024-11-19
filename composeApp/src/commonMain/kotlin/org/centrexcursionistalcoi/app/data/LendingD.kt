package org.centrexcursionistalcoi.app.data

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.centrexcursionistalcoi.app.server.response.data.ItemLendingD

fun ItemLendingD.fromDate(): LocalDate =
    Instant.fromEpochMilliseconds(from!!).toLocalDateTime(TimeZone.currentSystemDefault()).date

fun ItemLendingD.toDate(): LocalDate =
    Instant.fromEpochMilliseconds(to!!).toLocalDateTime(TimeZone.currentSystemDefault()).date

fun ItemLendingD.takenDate(): LocalDateTime =
    Instant.fromEpochMilliseconds(takenAt!!).toLocalDateTime(TimeZone.currentSystemDefault())

fun ItemLendingD.returnedDate(): LocalDateTime =
    Instant.fromEpochMilliseconds(returnedAt!!).toLocalDateTime(TimeZone.currentSystemDefault())
