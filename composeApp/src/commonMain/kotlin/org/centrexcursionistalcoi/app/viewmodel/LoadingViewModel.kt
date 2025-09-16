package org.centrexcursionistalcoi.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.centrexcursionistalcoi.app.auth.tokenStore
import org.publicvalue.multiplatform.oidc.ExperimentalOpenIdConnect

@OptIn(ExperimentalOpenIdConnect::class)
class LoadingViewModel : ViewModel() {
    fun load(
        onLoggedIn: () -> Unit,
        onNotLoggedIn: () -> Unit,
    ) = viewModelScope.launch {
        val accessToken = tokenStore.getAccessToken()
        if (accessToken != null) {
            onLoggedIn()
        } else {
            onNotLoggedIn()
        }
    }
}
