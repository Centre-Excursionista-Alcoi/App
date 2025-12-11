package org.centrexcursionistalcoi.app.viewmodel

import org.centrexcursionistalcoi.app.auth.AuthBackend
import org.centrexcursionistalcoi.app.doMain

class SettingsViewModel(private val onDeleteAccount: () -> Unit) : ErrorViewModel() {
    fun deleteAccount() = launch {
        AuthBackend.deleteAccount()
        doMain { onDeleteAccount() }
    }
}
