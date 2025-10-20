package org.centrexcursionistalcoi.app.authentik

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class AuthentikUser(
    val pk: Int,
    val username: String,
    val name: String,
    val email: String,
    val avatar: String,
    val uid: String,
    val path: String,
    val type: String,
    val uuid: String,
    val groups: List<String>,
    val attributes: JsonObject,

    @SerialName("is_active")
    val isActive: Boolean,

    @SerialName("last_login")
    val lastLogin: String?,

    @SerialName("date_joined")
    val dateJoined: String,

    @SerialName("is_superuser")
    val isSuperuser: Boolean,

    @SerialName("groups_obj")
    val groupsObj: List<AuthentikGroup>,

    @SerialName("password_change_date")
    val passwordChangeDate: String?,

    @SerialName("last_updated")
    val lastUpdated: String
)
