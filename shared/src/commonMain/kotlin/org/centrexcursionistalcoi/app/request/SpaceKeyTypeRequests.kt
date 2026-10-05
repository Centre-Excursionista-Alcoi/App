package org.centrexcursionistalcoi.app.request

import kotlinx.serialization.Serializable
import org.centrexcursionistalcoi.app.data.SpaceKeyType
import org.centrexcursionistalcoi.app.data.SpaceKeyTypeSpace
import kotlin.uuid.Uuid

@Serializable
data class CreateSpaceKeyTypeRequest(
    val name: String,
    val description: String? = null,
    /** The spaces the keys give access to, and how many a lending can take. */
    val spaces: List<SpaceKeyTypeSpace> = emptyList(),
)

/**
 * An empty [description] removes it. [spaces] replaces the spaces the type is for.
 */
@Serializable
data class UpdateSpaceKeyTypeRequest(
    val name: String? = null,
    val description: String? = null,
    val spaces: List<SpaceKeyTypeSpace>? = null,
) : UpdateEntityRequest<Uuid, SpaceKeyType> {
    override fun isEmpty(): Boolean = name == null && description == null && spaces == null
}
