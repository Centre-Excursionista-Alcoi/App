package org.centrexcursionistalcoi.app.composition

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import org.centrexcursionistalcoi.app.auth.AccountManager
import org.centrexcursionistalcoi.app.screen.Screen

/**
 * Navigates to a screen based on the current account state.
 * @param onLoggedIn The screen to navigate to if the user is logged in. `null` to not navigate.
 * @param onLoggedOut The screen to navigate to if the user is logged out. `null` to not navigate.
 */
@Composable
fun AccountStateNavigator(onLoggedIn: Screen<*, *>? = null, onLoggedOut: Screen<*, *>? = null) {
    val navController = LocalNavController.current
    val account by AccountManager.flow().collectAsState(null)
    LaunchedEffect(account) {
        if (account != null) {
            if (onLoggedIn != null) navController.navigate(onLoggedIn)
        } else {
            if (onLoggedOut != null) navController.navigate(onLoggedOut)
        }
    }
}
