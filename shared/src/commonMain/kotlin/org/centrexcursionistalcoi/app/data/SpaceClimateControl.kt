package org.centrexcursionistalcoi.app.data

import kotlinx.serialization.Serializable

/**
 * The devices a [Space] has to heat or cool it.
 */
@Serializable
enum class SpaceClimateControl {
    GAS_STOVE,
    WOOD_STOVE,
    PELLET_STOVE,
    FIREPLACE,
    ELECTRIC_HEATER,
    CENTRAL_HEATING,
    AIR_CONDITIONING,
    FAN,
}
