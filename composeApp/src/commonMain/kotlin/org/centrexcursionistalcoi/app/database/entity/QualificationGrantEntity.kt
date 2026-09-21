package org.centrexcursionistalcoi.app.database.entity

import androidx.room3.Entity
import androidx.room3.PrimaryKey
import kotlin.time.Instant
import kotlin.uuid.Uuid
import org.centrexcursionistalcoi.app.data.QualificationGrant

/**
 * A qualification the logged-in user holds. Only the user's own grants are stored (the database belongs to a
 * single account), so the qualification alone identifies the row.
 */
@Entity(tableName = "QualificationGrants")
data class QualificationGrantEntity(
    @PrimaryKey
    val qualificationId: Uuid,
    val userSub: String,
    val grantedBy: String?,
    val grantedAt: Instant,
    val expiresAt: Instant?,
) {
    fun toGrant() = QualificationGrant(qualificationId, userSub, grantedBy, grantedAt, expiresAt)

    companion object {
        fun QualificationGrant.toEntity() = QualificationGrantEntity(qualificationId, userSub, grantedBy, grantedAt, expiresAt)
    }
}
