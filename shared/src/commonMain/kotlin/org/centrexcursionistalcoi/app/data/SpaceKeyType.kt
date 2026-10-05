package org.centrexcursionistalcoi.app.data

import kotlinx.serialization.Serializable
import kotlin.uuid.Uuid

/**
 * A kind of key the club has to access spaces, e.g. the key of the main door of a space, or the permit to get to some
 * spaces by car. The keys themselves are [SpaceKey]s: individual and identifiable, like the items of the inventory.
 *
 * A type can be used in several spaces (a car permit works in all the ones it is [spaces] for), and each space limits
 * how many a lending can take.
 */
@Serializable
data class SpaceKeyType(
    override val id: Uuid,
    val name: String,
    val description: String?,
    /** The spaces this type of key gives access to. */
    val spaces: List<SpaceKeyTypeSpace>,
) : Entity<Uuid>

/**
 * A space a [SpaceKeyType] gives access to, and how many keys of the type a lending of it can take.
 */
@Serializable
data class SpaceKeyTypeSpace(
    val space: Uuid,
    val maxPerLending: Int,
)
