package backend.data.user

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class UserData(
    val fullName: String,
    val birthday: Instant,
    val phone: String,
    val city: String
)
