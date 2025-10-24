package org.centrexcursionistalcoi.app.viewmodel

import androidx.lifecycle.ViewModel
import org.centrexcursionistalcoi.app.auth.AuthBackend
import org.centrexcursionistalcoi.app.doAsync
import org.centrexcursionistalcoi.app.doMain

class LogoutViewModel(afterLogout: () -> Unit) : ViewModel() {
    init {
        launch {
            doAsync { AuthBackend.logout() }
            doMain { afterLogout() }
        }
    }
}
