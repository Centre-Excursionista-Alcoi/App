package org.centrexcursionistalcoi.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import org.centrexcursionistalcoi.app.auth.AccountManager
import org.centrexcursionistalcoi.app.route.Home
import org.centrexcursionistalcoi.app.route.Login

class LoadingViewModel : ViewModel() {
    fun load(navController: NavController) {
        viewModelScope.launch {
            val account = AccountManager.get()
            if (account != null) {
                navController.navigate(Home)
            } else {
                navController.navigate(Login)
            }
        }
    }
}
