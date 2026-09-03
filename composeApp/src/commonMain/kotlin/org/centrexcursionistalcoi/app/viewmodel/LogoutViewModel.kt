package org.centrexcursionistalcoi.app.viewmodel

import androidx.lifecycle.ViewModel
import org.centrexcursionistalcoi.app.auth.AuthBackend
import org.centrexcursionistalcoi.app.doAsync
import org.centrexcursionistalcoi.app.doMain
import org.koin.core.annotation.InjectedParam
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
class LogoutViewModel(authBackend: AuthBackend, @InjectedParam afterLogout: () -> Unit) : ViewModel() {
    init {
        launch {
            doAsync { authBackend.logout() }
            doMain { afterLogout() }
        }
    }
}
