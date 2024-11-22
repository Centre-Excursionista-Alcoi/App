package org.centrexcursionistalcoi.app.database.entity

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import org.centrexcursionistalcoi.app.data.DatabaseData

interface BookingEntity<SType: DatabaseData>: DatabaseEntity<SType> {
    val from: LocalDate?
    val to: LocalDate?
    val userId: String?
    val confirmed: Boolean
    val takenAt: Instant?
    val returnedAt: Instant?
}
