package org.centrexcursionistalcoi.app.network

import kotlin.time.Duration
import kotlin.uuid.Uuid
import org.centrexcursionistalcoi.app.data.Space
import org.centrexcursionistalcoi.app.database.SpacesRepository
import org.centrexcursionistalcoi.app.utils.Zero

object SpacesRemoteRepository : SymmetricRemoteRepository<Uuid, Space>(
    "/spaces",
    Space.serializer(),
    SpacesRepository
) {
    suspend fun create(
        name: String,
        description: String?,
        /**
         * Pair of price (€) and duration (e.g., hourly, daily).
         */
        price: Pair<Double, Duration>?,
        capacity: Int?,
    ) {
        create(Space(Uuid.Zero, name, description, price, capacity))
    }
}
