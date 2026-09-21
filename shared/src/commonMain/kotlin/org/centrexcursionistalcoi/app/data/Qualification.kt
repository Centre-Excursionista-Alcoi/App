package org.centrexcursionistalcoi.app.data

import kotlin.time.Instant
import kotlin.uuid.Uuid
import kotlinx.serialization.Serializable
import org.centrexcursionistalcoi.app.serializer.InstantSerializer

/**
 * A skill or certification defined by a department, that members can be granted (see [QualificationGrant]).
 * Definitions are readable by every logged-in user, since events display which qualifications they require.
 */
@Serializable
data class Qualification(
    val id: Uuid,
    val departmentId: Uuid,
    val name: String,
    val description: String?,
)

/**
 * The fact that [userSub] holds a qualification, granted by [grantedBy] (the examiner's sub, `null` if that
 * account no longer exists). A grant with an [expiresAt] in the past is expired and no longer counts.
 *
 * Unlike [Qualification], grants are private: only the holder, examiners/managers of the qualification's
 * department, its people managers and global admins may read them.
 */
@Serializable
data class QualificationGrant(
    val qualificationId: Uuid,
    val userSub: String,
    val grantedBy: String?,
    @Serializable(InstantSerializer::class) val grantedAt: Instant,
    @Serializable(InstantSerializer::class) val expiresAt: Instant?,
)

/** `true` if this grant counts at [now]: it has no expiry, or expires after [now]. */
fun QualificationGrant.isActiveAt(now: Instant): Boolean = expiresAt == null || expiresAt > now

/** A confirmed department member as listed to an examiner picking who to grant a qualification to. */
@Serializable
data class DepartmentRosterMember(
    val sub: String,
    val fullName: String,
)
