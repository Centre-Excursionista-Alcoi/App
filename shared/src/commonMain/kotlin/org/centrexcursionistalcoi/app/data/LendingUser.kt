package org.centrexcursionistalcoi.app.data

import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.serialization.Serializable

@Serializable
@OptIn(ExperimentalUuidApi::class)
data class LendingUser(
    override val id: Uuid,
    val sub: String,
    val nif: String,
    val phoneNumber: String,
): Entity<Uuid> {
    override fun toMap(): Map<String, Any?> {
        return mapOf(
            "id" to id,
            "sub" to sub,
            "nif" to nif,
            "phoneNumber" to phoneNumber,
        )
    }
}
