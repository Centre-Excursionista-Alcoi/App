package org.centrexcursionistalcoi.app.viewmodel.spaces

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.centrexcursionistalcoi.app.data.SpaceKeyType
import kotlin.uuid.Uuid

/** A type of key a lending of a space can take, and how many. */
data class SpaceKeyOption(val type: SpaceKeyType, val maxPerLending: Int)

/** Keeps the key types that are for [space], with their maximum for a lending of it. */
fun Flow<List<SpaceKeyType>>.forSpace(space: Uuid): Flow<List<SpaceKeyOption>> = map { types ->
    types.mapNotNull { type ->
        type.spaces.find { it.space == space }?.let { SpaceKeyOption(type, it.maxPerLending) }
    }
}
