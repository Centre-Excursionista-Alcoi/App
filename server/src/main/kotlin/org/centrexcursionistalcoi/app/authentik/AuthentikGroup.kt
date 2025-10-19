package org.centrexcursionistalcoi.app.authentik

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class AuthentikGroup(
    val pk: String,
    val name: String,
    val parent: String,
    val attributes: JsonObject,

    @SerialName("num_pk")
    val numPk: Int,

    @SerialName("is_superuser")
    val isSuperuser: Boolean,

    @SerialName("parent_name")
    val parentName: String
)
