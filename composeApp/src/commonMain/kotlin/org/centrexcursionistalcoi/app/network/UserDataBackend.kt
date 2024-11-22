package org.centrexcursionistalcoi.app.network

import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import org.centrexcursionistalcoi.app.data.UserD
import org.centrexcursionistalcoi.app.database.entity.admin.User
import org.centrexcursionistalcoi.app.error.ServerException

object UserDataBackend {
    /**
     * Fetch the user data from the server
     * @return The user data
     * @throws ServerException If the server responds with an error
     * @throws SerializationException If the server responds with invalid data
     */
    suspend fun getUserData(): UserD = Backend.get("/me", UserD.serializer())

    suspend fun listUsers(): List<User> = Backend.get("/users", ListSerializer(UserD.serializer()))
        .map { User.deserialize(it) }

    /**
     * Confirm a user
     * @param user The user to confirm
     * @throws ServerException If the server responds with an error
     */
    suspend fun confirm(user: User) = Backend.post("/users/${user.email}/confirm")

    /**
     * Remove a user completely from the system
     * @param user The user to remove
     * @throws ServerException If the server responds with an error
     */
    suspend fun delete(user: User) = Backend.delete("/users/${user.email}")
}
