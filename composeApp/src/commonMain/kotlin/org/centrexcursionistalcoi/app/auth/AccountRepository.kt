package org.centrexcursionistalcoi.app.auth

import io.github.aakira.napier.Napier
import io.sentry.kotlin.multiplatform.Sentry
import io.sentry.kotlin.multiplatform.protocol.User
import org.centrexcursionistalcoi.app.network.AuthBackend
import org.centrexcursionistalcoi.app.push.PushNotifications

object AccountRepository {
    suspend fun login(email: String, password: String) {
        Napier.i { "Logging is as $email..." }
        AuthBackend.login(email, password)
        Napier.i { "Logged in successfully." }

        Napier.d { "Storing account into AccountManager..." }
        AccountManager.put(Account(email), password)

        Napier.d { "Updating Sentry user..." }
        val user = User().apply {
            this.email = email
        }
        Sentry.setUser(user)

        Napier.d { "Refreshing token on server..." }
        PushNotifications.refreshTokenOnServer()
    }

    suspend fun logout() {
        Sentry.configureScope { scope ->
            scope.user = null
        }

        AccountManager.logout()
    }
}
