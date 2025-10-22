package org.centrexcursionistalcoi.app.network

import org.centrexcursionistalcoi.app.data.UserData
import org.centrexcursionistalcoi.app.database.UsersRepository

object UsersRemoteRepository : SymmetricRemoteRepository<String, UserData>(
    "/users",
    UserData.serializer(),
    UsersRepository
)
