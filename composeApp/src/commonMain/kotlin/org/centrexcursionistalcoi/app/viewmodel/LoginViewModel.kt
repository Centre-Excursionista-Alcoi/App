package org.centrexcursionistalcoi.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.aakira.napier.Napier
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.centrexcursionistalcoi.app.auth.createOidcConnectClient
import org.centrexcursionistalcoi.app.auth.getOidcConnectClient
import org.centrexcursionistalcoi.app.auth.setOidcConnectClient
import org.centrexcursionistalcoi.app.auth.tokenStore
import org.publicvalue.multiplatform.oidc.ExperimentalOpenIdConnect
import org.publicvalue.multiplatform.oidc.OpenIdConnectException
import org.publicvalue.multiplatform.oidc.appsupport.CodeAuthFlowFactory
import org.publicvalue.multiplatform.oidc.tokenstore.saveTokens

class LoginViewModel(private val authFlowFactory: CodeAuthFlowFactory): ViewModel() {
    private val oidcConnectClient get() = getOidcConnectClient()

    private val _isLoggingIn = MutableStateFlow(false)
    val isLoggingIn get() = _isLoggingIn.asStateFlow()

    private val _isStoringToken = MutableStateFlow(false)
    val isStoringToken get() = _isStoringToken.asStateFlow()

    val isLoading = combine(isLoggingIn, isStoringToken) { loggingIn, storingToken ->
        loggingIn || storingToken
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val _discoveryComplete = MutableStateFlow(false)
    val discoveryComplete get() = _discoveryComplete.asStateFlow()

    private val _error = MutableStateFlow<OpenIdConnectException?>(null)
    val error get() = _error.asStateFlow()

    fun load() = viewModelScope.launch {
        try {
            _discoveryComplete.tryEmit(false)
            if (oidcConnectClient.config.discoveryUri != null) {
                Napier.i { "Running discovery for OIDC..." }
                oidcConnectClient.discover()
            } else {
                Napier.i { "No discovery URI configured, skipping discovery." }
            }
        } catch (e: OpenIdConnectException) {
            Napier.e("Error during OIDC discovery", e)
            Napier.i { "Using default OIDC endpoints" }
            setOidcConnectClient(
                createOidcConnectClient(includeDiscoveryUri = false)
            )
        } finally {
            Napier.i { "Discovery complete." }
            _discoveryComplete.tryEmit(true)
        }
    }

    @OptIn(ExperimentalOpenIdConnect::class)
    fun login() = viewModelScope.launch {
        try {
            if (isLoading.value) {
                Napier.d { "Already loading." }
                return@launch
            }

            _isLoggingIn.emit(true)
            _isStoringToken.emit(false)
            _error.emit(null)

            Napier.i { "Creating auth flow..." }
            val flow = authFlowFactory.createAuthFlow(oidcConnectClient)
            Napier.i { "Auth flow creation complete. Getting access token..." }
            val token = flow.getAccessToken()

            _isLoggingIn.emit(false)
            _isStoringToken.emit(true)

            Napier.i { "Access token obtained, saving token..." }
            tokenStore.saveTokens(token)
        } catch (e: OpenIdConnectException.AuthenticationCancelled) {
            // TODO: Handle auth cancelled by user
            _error.tryEmit(e)
            Napier.i("Authentication cancelled by user", e)
        } catch (e: OpenIdConnectException.AuthenticationFailure) {
            // TODO: Handle auth failure
            _error.tryEmit(e)
            Napier.e("Authentication failed", e)
        } catch (e: OpenIdConnectException.TechnicalFailure) {
            // TODO: Handle auth failure
            _error.tryEmit(e)
            Napier.e("Technical failure", e)
        } catch (e: OpenIdConnectException) {
            // TODO: Handle auth failure
            _error.tryEmit(e)
            Napier.e("OIDC error", e)
        } finally {
            _isLoggingIn.emit(false)
            _isStoringToken.emit(false)
        }
    }
}
