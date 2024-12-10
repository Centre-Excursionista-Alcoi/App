package org.centrexcursionistalcoi.app.route

import kotlinx.serialization.Serializable
import org.centrexcursionistalcoi.app.data.ItemLendingD
import org.centrexcursionistalcoi.app.data.SpaceBookingD

@Serializable
data class Home(
    /**
     * **Only for admins**
     *
     * If this is set, the booking with the given ID will be shown.
     * Note that this is the string ID, which includes the type of booking (see [ItemLendingD.toString] and
     * [SpaceBookingD.toString]).
     */
    val showBookingIdString: String? = null,

    /**
     * **Only for admins**
     *
     * If this is set, the user with the given ID will be shown.
     * If it's not confirmed, the confirmation dialog will be shown.
     */
    val showUserId: String? = null
) : Route
