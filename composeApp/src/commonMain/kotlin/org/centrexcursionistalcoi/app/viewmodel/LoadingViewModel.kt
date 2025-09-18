package org.centrexcursionistalcoi.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.aakira.napier.Napier
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import kotlinx.coroutines.launch
import org.centrexcursionistalcoi.app.auth.getOidcConnectClient
import org.centrexcursionistalcoi.app.auth.refreshHandler
import org.centrexcursionistalcoi.app.auth.tokenStore
import org.centrexcursionistalcoi.app.network.getHttpClient
import org.publicvalue.multiplatform.oidc.ExperimentalOpenIdConnect
import org.publicvalue.multiplatform.oidc.types.Jwt

@OptIn(ExperimentalOpenIdConnect::class, ExperimentalTime::class)
class LoadingViewModel : ViewModel() {
    fun load(
        onLoggedIn: (name: String, groups: List<String>) -> Unit,
        onNotLoggedIn: () -> Unit,
    ) = viewModelScope.launch {
        val accessToken = tokenStore.getAccessToken()
        if (accessToken != null) {
            val refreshToken = tokenStore.getRefreshToken()
            if (refreshToken != null) {
                Napier.i { "User is already logged in. Refreshing token..." }

                refreshHandler.refreshAndSaveToken(getOidcConnectClient(), accessToken)
                Napier.i { "Token is refreshed, navigating..." }
            } else {
                Napier.i { "User is already logged in but no refresh token available, navigating..." }
            }

            var name = "Unknown"
            var groups = emptyList<String>()

            val idToken = tokenStore.getIdToken()
            if (idToken != null) {
                val jwt = Jwt.parse(idToken).payload
                name = jwt.additionalClaims.getValue("name") as String
                @Suppress("UNCHECKED_CAST")
                groups = jwt.additionalClaims.getValue("groups") as List<String>
            } else {
                Napier.w { "ID token is not available" }
            }

            onLoggedIn(name, groups)
        } else {
            Napier.i { "User is not logged in" }
            onNotLoggedIn()
        }
    }
}
