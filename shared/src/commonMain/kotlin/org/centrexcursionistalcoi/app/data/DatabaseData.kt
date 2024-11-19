package org.centrexcursionistalcoi.app.data

import kotlinx.serialization.Serializable

@Serializable
sealed interface DatabaseData {
    val id: Int?
}
