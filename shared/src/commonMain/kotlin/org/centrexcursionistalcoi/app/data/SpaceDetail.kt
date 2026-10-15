package org.centrexcursionistalcoi.app.data

import kotlinx.serialization.Serializable

/**
 * The optional values of a [Space] that [org.centrexcursionistalcoi.app.request.UpdateSpaceRequest.clear] can remove.
 */
@Serializable
enum class SpaceDetail {
    /** [Space.seats] */
    SEATS,

    /** [Space.beds] */
    BEDS,

    /** [Space.altitude] */
    ALTITUDE,

    /** Both [Space.latitude] and [Space.longitude] */
    COORDINATES,
}
