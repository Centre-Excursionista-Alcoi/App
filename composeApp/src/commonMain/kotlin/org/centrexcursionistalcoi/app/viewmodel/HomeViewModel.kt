package org.centrexcursionistalcoi.app.viewmodel

import androidx.lifecycle.ViewModel
import org.centrexcursionistalcoi.app.auth.AccountManager

class HomeViewModel : ViewModel() {
    fun logout() {
        launch {
            AccountManager.logout()
        }
    }
}
