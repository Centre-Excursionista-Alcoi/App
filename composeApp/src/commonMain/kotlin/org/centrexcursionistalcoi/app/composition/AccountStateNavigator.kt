package org.centrexcursionistalcoi.app.composition

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import org.centrexcursionistalcoi.app.auth.Account
import org.centrexcursionistalcoi.app.auth.AccountManager
import org.centrexcursionistalcoi.app.route.Route

/**
 * Navigates to a screen based on the current account state.
 * @param onLoggedIn The screen to navigate to if the user is logged in. `null` to not navigate.
 * @param onLoggedOut The screen to navigate to if the user is logged out. `null` to not navigate.
 */
@Composable
fun AccountStateNavigator(onLoggedIn: Route? = null, onLoggedOut: Route? = null) {
    val navController = LocalNavController.current
    val account by AccountManager.flow().collectAsState(null)
    var initial: Account? by remember { mutableStateOf(null) }
    LaunchedEffect(account) {
        // Ignore the initial state
        if (initial == null) {
            initial = account
            return@LaunchedEffect
        }
        if (account != null) {
            if (onLoggedIn != null) navController.navigate(onLoggedIn)
        } else {
            if (onLoggedOut != null) navController.navigate(onLoggedOut)
        }
    }
}
