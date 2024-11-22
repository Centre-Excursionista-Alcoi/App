package org.centrexcursionistalcoi.app.endpoints.auth

import io.ktor.http.HttpMethod
import io.ktor.server.request.receiveText
import io.ktor.server.routing.RoutingContext
import io.ktor.server.sessions.get
import io.ktor.server.sessions.sessions
import org.centrexcursionistalcoi.app.database.SessionsDatabase
import org.centrexcursionistalcoi.app.database.entity.User
import org.centrexcursionistalcoi.app.endpoints.model.SecureEndpoint
import org.centrexcursionistalcoi.app.security.UserSession
import org.centrexcursionistalcoi.app.server.response.Errors

object UpdateFCMTokenEndpoint: SecureEndpoint("/fcm_token", HttpMethod.Post) {
    override suspend fun RoutingContext.secureBody(user: User) {
        val token = call.receiveText()
        if (token.isBlank()) {
            respondFailure(Errors.InvalidRequest)
            return
        }

        val session = call.sessions.get(UserSession::class)
        SessionsDatabase.updateFCMToken(session!!, token)

        respondSuccess()
    }
}
