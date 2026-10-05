package org.centrexcursionistalcoi.app.request

import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable
import org.centrexcursionistalcoi.app.data.Category
import org.centrexcursionistalcoi.app.data.FileWithContext
import kotlin.uuid.Uuid

@Serializable
data class CreateSpaceLendingRequest(
    val space: Uuid,
    val checkIn: LocalDate,
    val checkOut: LocalDate,
    val attendees: Map<Category, Int>,
    /** Quantity wanted of each key (by [org.centrexcursionistalcoi.app.data.SpaceKey] id). */
    val keys: Map<Uuid, Int> = emptyMap(),
    val acceptConditions: Boolean = false,
    val notes: String? = null,
)

@Serializable
data class UpdateSpaceLendingAttendeesRequest(
    val attendees: Map<Category, Int>,
)

@Serializable
data class SubmitSpaceLendingReportRequest(
    val reportNotes: String? = null,
    val reportIssues: String? = null,
    val reportNotesFiles: List<FileWithContext> = emptyList(),
    val reportIssuesFiles: List<FileWithContext> = emptyList(),
) : RequestWithFiles<SubmitSpaceLendingReportRequest> {
    override fun mapFiles(transform: (FileWithContext) -> FileWithContext) = copy(
        reportNotesFiles = reportNotesFiles.map(transform),
        reportIssuesFiles = reportIssuesFiles.map(transform),
    )
}

@Serializable
data class AttachPaymentProofRequest(
    val files: List<FileWithContext>,
) : RequestWithFiles<AttachPaymentProofRequest> {
    override fun mapFiles(transform: (FileWithContext) -> FileWithContext) = copy(files = files.map(transform))
}

/**
 * Changes a lending that hasn't been picked up yet. Missing fields stay as they are. [keys] replaces all the keys.
 */
@Serializable
data class UpdateSpaceLendingRequest(
    val checkIn: LocalDate? = null,
    val checkOut: LocalDate? = null,
    val attendees: Map<Category, Int>? = null,
    val keys: Map<Uuid, Int>? = null,
    val notes: String? = null,
)

/** Admin: sets the payment status. */
@Serializable
data class SetSpaceLendingPaymentRequest(
    val status: org.centrexcursionistalcoi.app.data.PaymentStatus,
)
