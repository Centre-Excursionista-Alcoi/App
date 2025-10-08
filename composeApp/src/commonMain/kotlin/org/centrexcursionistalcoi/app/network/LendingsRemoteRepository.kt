package org.centrexcursionistalcoi.app.network

import kotlin.uuid.Uuid
import org.centrexcursionistalcoi.app.data.Lending
import org.centrexcursionistalcoi.app.database.LendingsRepository

object LendingsRemoteRepository : RemoteRepository<Uuid, Lending>(
    "/inventory/lendings",
    Lending.serializer(),
    LendingsRepository
)
