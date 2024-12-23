package org.centrexcursionistalcoi.app.route

import kotlinx.serialization.Serializable

@Serializable
data class ItemTypeEditorRoute(
    val itemTypeId: Int? = null
): Route
