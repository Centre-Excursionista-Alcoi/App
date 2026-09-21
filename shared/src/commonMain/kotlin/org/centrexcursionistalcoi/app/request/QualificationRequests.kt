package org.centrexcursionistalcoi.app.request

import kotlin.time.Instant
import kotlinx.serialization.Serializable
import org.centrexcursionistalcoi.app.serializer.InstantSerializer

/** Body of `POST /departments/{id}/qualifications`. */
@Serializable
data class CreateQualificationRequest(
    val name: String,
    val description: String? = null,
)

/**
 * Body of `PATCH /qualifications/{id}`. A `null` field is left unchanged; a blank [description] clears it.
 */
@Serializable
data class UpdateQualificationRequest(
    val name: String? = null,
    val description: String? = null,
) {
    fun isEmpty(): Boolean = name == null && description == null
}

/**
 * Body of `POST /qualifications/{id}/grants`. Granting again to a user who already holds the qualification
 * replaces their grant (new grantor, timestamp and expiry).
 */
@Serializable
data class GrantQualificationRequest(
    val userSub: String,
    @Serializable(InstantSerializer::class) val expiresAt: Instant? = null,
)
