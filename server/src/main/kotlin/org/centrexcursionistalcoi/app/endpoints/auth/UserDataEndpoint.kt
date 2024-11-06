package org.centrexcursionistalcoi.app.endpoints.auth

import io.ktor.http.HttpMethod
import io.ktor.server.routing.RoutingContext
import org.centrexcursionistalcoi.app.database.ServerDatabase
import org.centrexcursionistalcoi.app.database.entity.User
import org.centrexcursionistalcoi.app.endpoints.model.SecureEndpoint
import org.centrexcursionistalcoi.app.server.response.data.UserD
import org.slf4j.LoggerFactory

object UserDataEndpoint: SecureEndpoint("/me", HttpMethod.Get) {
    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun RoutingContext.secureBody(user: User) {
        logger.info("Serializing user...")
        val response = ServerDatabase { user.serializable() }
        logger.info("Responding serialized user...")
        respondSuccess(response, UserD.serializer())
    }
}
