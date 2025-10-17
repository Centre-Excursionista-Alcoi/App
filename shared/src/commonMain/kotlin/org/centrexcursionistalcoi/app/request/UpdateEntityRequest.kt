package org.centrexcursionistalcoi.app.request

import kotlinx.serialization.Serializable
import org.centrexcursionistalcoi.app.data.Entity

@Serializable
sealed interface UpdateEntityRequest<ID: Any, E: Entity<ID>> {
    fun isEmpty(): Boolean
}
