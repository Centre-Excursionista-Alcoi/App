package org.centrexcursionistalcoi.app.network

import kotlinx.serialization.SerializationException
import org.centrexcursionistalcoi.app.error.ServerException
import org.centrexcursionistalcoi.app.server.response.data.UserD

object UserDataBackend {
    /**
     * Fetch the user data from the server
     * @return The user data
     * @throws ServerException If the server responds with an error
     * @throws SerializationException If the server responds with invalid data
     */
    suspend fun getUserData(): UserD = Backend.get("/me", UserD.serializer())
}
