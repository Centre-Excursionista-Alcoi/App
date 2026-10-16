package org.centrexcursionistalcoi.app.data

import kotlinx.serialization.Serializable

/**
 * Where the electricity of a [Space] comes from.
 */
@Serializable
enum class SpaceEnergySource {
    GRID,
    SOLAR,
    WIND,
    HYDRO,
    DIESEL_GENERATOR,
    GAS_GENERATOR,
    BATTERIES,
}
