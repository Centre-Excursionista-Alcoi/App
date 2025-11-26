package org.centrexcursionistalcoi.app.database.entity.base

import java.time.Instant

interface LastUpdateEntity {
    var lastUpdate: Instant

    /**
     * Notifies that this entity has been updated by storing the current timestamp in Redis, and updating the [lastUpdate] field.
     */
    suspend fun updated()
}
