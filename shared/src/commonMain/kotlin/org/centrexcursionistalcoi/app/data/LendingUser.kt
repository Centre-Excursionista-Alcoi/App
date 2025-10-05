package org.centrexcursionistalcoi.app.data

import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.serialization.Serializable

@Serializable
@OptIn(ExperimentalUuidApi::class)
data class LendingUser(
    override val id: Uuid,
    val sub: String,

    val fullName: String,
    val nif: String,

    val phoneNumber: String,

    val sports: List<Sports>,

    val address: String,
    val postalCode: String,
    val city: String,
    val province: String,
    val country: String,
): Entity<Uuid> {
    override fun toMap(): Map<String, Any?> {
        return mapOf(
            "id" to id,
            "sub" to sub,
            "fullName" to fullName,
            "nif" to nif,
            "phoneNumber" to phoneNumber,
            "sports" to sports,
            "address" to address,
            "postalCode" to postalCode,
            "city" to city,
            "province" to province,
            "country" to country,
        )
    }
}
