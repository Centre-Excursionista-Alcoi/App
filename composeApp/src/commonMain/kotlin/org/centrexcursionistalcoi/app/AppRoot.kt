package org.centrexcursionistalcoi.app

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import ceaapp.composeapp.generated.resources.*
import org.centrexcursionistalcoi.app.component.AppText
import org.centrexcursionistalcoi.app.composition.LocalNavController
import org.centrexcursionistalcoi.app.network.Backend
import org.centrexcursionistalcoi.app.platform.ui.PlatformDialog
import org.centrexcursionistalcoi.app.platform.ui.PlatformTheme
import org.centrexcursionistalcoi.app.route.Loading
import org.centrexcursionistalcoi.app.screen.HomeScreen
import org.centrexcursionistalcoi.app.screen.LoadingScreen
import org.centrexcursionistalcoi.app.screen.LoginScreen
import org.centrexcursionistalcoi.app.screen.NotificationsScreen
import org.centrexcursionistalcoi.app.screen.RegisterScreen
import org.centrexcursionistalcoi.app.screen.ReservationScreen
import org.centrexcursionistalcoi.app.screen.SettingsScreen
import org.centrexcursionistalcoi.app.screen.admin.ItemTypeScreen
import org.centrexcursionistalcoi.app.screen.composable
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
@Preview
fun AppRoot() {
    val navController = rememberNavController()

    PlatformTheme {
        val errorState by Backend.error.collectAsState()
        errorState?.let { error ->
            PlatformDialog(
                onDismissRequest = Backend::clearError,
                title = stringResource(Res.string.error_dialog_title),
                actions = {
                    NeutralButton(
                        text = stringResource(Res.string.ok),
                        onClick = Backend::clearError
                    )
                }
            ) {
                AppText(
                    text = stringResource(Res.string.error_dialog_code, error.code ?: -1),
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp).padding(horizontal = 12.dp)
                )
                AppText(
                    text = stringResource(Res.string.error_dialog_path, error.method?.value ?: "N/A", error.path ?: "N/A"),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)
                )
                AppText(
                    text = stringResource(Res.string.error_dialog_response, error.response),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
        }

        CompositionLocalProvider(
            LocalNavController provides navController
        ) {
            NavHost(navController, startDestination = Loading) {
                composable(LoadingScreen)
                composable(LoginScreen)
                composable(RegisterScreen)
                composable(HomeScreen)
                composable(ReservationScreen)
                composable(NotificationsScreen)
                composable(SettingsScreen)

                composable(ItemTypeScreen)
            }
        }
    }
}
