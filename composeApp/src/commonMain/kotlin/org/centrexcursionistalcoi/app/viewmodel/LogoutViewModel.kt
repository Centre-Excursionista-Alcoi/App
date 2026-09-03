package org.centrexcursionistalcoi.app.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.withContext
import org.centrexcursionistalcoi.app.auth.AuthBackend
import org.centrexcursionistalcoi.app.di.DispatcherProvider
import org.koin.core.annotation.InjectedParam
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
class LogoutViewModel(
    authBackend: AuthBackend,
    dispatcherProvider: DispatcherProvider,
    @InjectedParam afterLogout: () -> Unit,
) : ViewModel() {
    init {
        launch {
            withContext(dispatcherProvider.io) { authBackend.logout() }
            withContext(dispatcherProvider.main) { afterLogout() }
        }
    }
}
