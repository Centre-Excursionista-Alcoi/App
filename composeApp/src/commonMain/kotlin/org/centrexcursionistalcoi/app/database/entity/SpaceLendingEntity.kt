package org.centrexcursionistalcoi.app.database.entity

import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey
import kotlinx.datetime.LocalDate
import org.centrexcursionistalcoi.app.data.Category
import org.centrexcursionistalcoi.app.data.PaymentStatus
import org.centrexcursionistalcoi.app.data.SpaceLending
import org.centrexcursionistalcoi.app.data.SpaceLendingKey
import kotlin.time.Instant
import kotlin.uuid.Uuid

@Entity(
    tableName = "SpaceLendings",
    foreignKeys = [
        ForeignKey(
            entity = SpaceEntity::class,
            parentColumns = ["id"],
            childColumns = ["space"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["space"])],
)
data class SpaceLendingEntity(
    @PrimaryKey
    val id: Uuid,
    val lastUpdate: Instant,
    val timestamp: Instant,
    val space: Uuid,
    val userSub: String?,
    val checkIn: LocalDate,
    val checkOut: LocalDate,
    val attendees: Map<Category, Int>,
    val acceptedConditionsAt: Instant?,
    val cancelled: Boolean,
    val notes: String?,
    val pickedUpAt: Instant?,
    val pickedUpBy: String?,
    val returnedAt: Instant?,
    val returnedBy: String?,
    val keys: List<SpaceLendingKey>,
    val totalPrice: Double,
    val paymentStatus: PaymentStatus,
    val reportNotes: String?,
    val reportIssues: String?,
    val reportSubmittedAt: Instant?,
    val reportNotesFiles: List<Uuid>,
    val reportIssuesFiles: List<Uuid>,
    val paymentProofs: List<Uuid>,
) {
    fun toSpaceLending() = SpaceLending(
        id = id,
        lastUpdate = lastUpdate,
        timestamp = timestamp,
        space = space,
        userSub = userSub,
        checkIn = checkIn,
        checkOut = checkOut,
        attendees = attendees,
        acceptedConditionsAt = acceptedConditionsAt,
        cancelled = cancelled,
        notes = notes,
        pickedUpAt = pickedUpAt,
        pickedUpBy = pickedUpBy,
        returnedAt = returnedAt,
        returnedBy = returnedBy,
        keys = keys,
        totalPrice = totalPrice,
        paymentStatus = paymentStatus,
        reportNotes = reportNotes,
        reportIssues = reportIssues,
        reportSubmittedAt = reportSubmittedAt,
        reportNotesFiles = reportNotesFiles,
        reportIssuesFiles = reportIssuesFiles,
        paymentProofs = paymentProofs,
    )

    companion object {
        fun SpaceLending.toEntity() = SpaceLendingEntity(
            id = id,
            lastUpdate = lastUpdate,
            timestamp = timestamp,
            space = space,
            userSub = userSub,
            checkIn = checkIn,
            checkOut = checkOut,
            attendees = attendees,
            acceptedConditionsAt = acceptedConditionsAt,
            cancelled = cancelled,
            notes = notes,
            pickedUpAt = pickedUpAt,
            pickedUpBy = pickedUpBy,
            returnedAt = returnedAt,
            returnedBy = returnedBy,
            keys = keys,
            totalPrice = totalPrice,
            paymentStatus = paymentStatus,
            reportNotes = reportNotes,
            reportIssues = reportIssues,
            reportSubmittedAt = reportSubmittedAt,
            reportNotesFiles = reportNotesFiles,
            reportIssuesFiles = reportIssuesFiles,
            paymentProofs = paymentProofs,
        )
    }
}
