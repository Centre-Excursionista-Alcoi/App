package org.centrexcursionistalcoi.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.centrexcursionistalcoi.app.auth.oidcConnectClient
import org.centrexcursionistalcoi.app.auth.tokenStore
import org.publicvalue.multiplatform.oidc.ExperimentalOpenIdConnect
import org.publicvalue.multiplatform.oidc.appsupport.CodeAuthFlowFactory
import org.publicvalue.multiplatform.oidc.tokenstore.saveTokens

class LoginViewModel(private val authFlowFactory: CodeAuthFlowFactory): ViewModel() {
    private val _isLoading = MutableStateFlow(false)
    val isLoading get() = _isLoading.asStateFlow()

    fun load() = viewModelScope.launch {
        oidcConnectClient.discover()
    }

    @OptIn(ExperimentalOpenIdConnect::class)
    fun login() = viewModelScope.launch {
        try {
            _isLoading.emit(true)

            val flow = authFlowFactory.createAuthFlow(oidcConnectClient)
            println("Auth flow complete. Getting access token...")
            val token = flow.getAccessToken()
            println("Access token obtained, saving token...")
            tokenStore.saveTokens(token)
        } finally {
            _isLoading.emit(false)
        }
    }
}
