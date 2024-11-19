package org.centrexcursionistalcoi.app.data

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate

interface IBookingD : DatabaseData {
    val from: LocalDate?
    val to: LocalDate?
    val userId: String?
    val confirmed: Boolean
    val takenAt: Instant?
    val returnedAt: Instant?
}
