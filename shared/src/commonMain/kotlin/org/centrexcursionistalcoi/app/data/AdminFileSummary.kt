package org.centrexcursionistalcoi.app.data

import kotlin.time.Instant
import kotlin.uuid.Uuid
import kotlinx.serialization.Serializable
import org.centrexcursionistalcoi.app.serializer.InstantSerializer

/**
 * Lightweight summary of a row in the `files` table, for the admin file manager's list view.
 * Deliberately excludes [org.centrexcursionistalcoi.app.database.table.Files]'s `bytes` column.
 */
@Serializable
data class AdminFileSummary(
    val id: Uuid,
    val name: String?,
    val type: String?,
    val sizeBytes: Long,
    @Serializable(InstantSerializer::class) val lastModified: Instant,
)
