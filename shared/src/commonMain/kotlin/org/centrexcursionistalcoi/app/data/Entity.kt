package org.centrexcursionistalcoi.app.data

import kotlinx.serialization.Serializable

@Serializable
sealed interface Entity<IdType: Any> {
    val id: IdType
}
